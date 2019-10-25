package nl.f00f.unpacker;

import nl.f00f.unpacker.settings.UnpackerSettings;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Unpacks an archive.
 */
public class Unpacker {
    private static final Logger logger = LoggerFactory.getLogger(Unpacker.class);

    /**
     * Tar decompressor algorithms.
     */
    private static final List<TarDecompressor> ALGORITHMS = List.of(
            new TarDecompressor("tar", Pattern.compile(".*?\\.tar$"), TarArchiveInputStream::new),
            new TarDecompressor(
                    "tar.gz",
                    Pattern.compile(".*?(?:\\.tar\\.gz|\\.tgz|\\.tpz)$"),
                    in -> new TarArchiveInputStream(new GzipCompressorInputStream(in))
            ),
            new TarDecompressor(
                    "tar.bz2",
                    Pattern.compile(".*?(?:\\.tar\\.bz2|\\.tar\\.bzip2|\\.tbz|\\.tbz2|\\.tb2)$"),
                    in -> new TarArchiveInputStream(new BZip2CompressorInputStream(in))
            ),
            new TarDecompressor(
                    "tar.lzma",
                    Pattern.compile(".*?(?:\\.tar\\.lzma|\\.tlz|\\.tlzma)$"),
                    in -> new TarArchiveInputStream(new LZMACompressorInputStream(in))
            ),
            new TarDecompressor(
                    "tar.xz",
                    Pattern.compile(".*?(?:\\.tar\\.xz|\\.txz)$"),
                    in -> new TarArchiveInputStream(new XZCompressorInputStream(in))
            ),
            new TarDecompressor(
                    "tar.Z",
                    Pattern.compile(".*?(?:\\.tar\\.Z|\\.tZ)$"),
                    in -> new TarArchiveInputStream(new ZCompressorInputStream(in))
            )
    );

    /**
     * The worker settings. Used to find the size limits for extracted ZIPs.
     */
    private final UnpackerSettings settings;

    /**
     * Creates a new unpacker.
     *
     * @param settings the worker settings
     */
    @Contract(pure = true)
    public Unpacker(final UnpackerSettings settings) {
        this.settings = settings;
    }

    /**
     * Unpacks an archive into a given directory.
     *
     * If the archive type can not be determined, the archive itself is treated as the single
     * file to unpack.
     *
     * @param arPath the archive to unpack
     * @param targetDir the directory the uncompressed files are to be written to
     *
     * @return the collection of uncompressed files
     *
     * @throws IOException if uncompressing fails
     */
    public Collection<Path> unpack(final Path arPath, final Path targetDir) throws IOException {
        final var pathStr = arPath.toString();

        if (pathStr.endsWith(".zip")) {
            return this.unzip(arPath, targetDir);
        }

        @Nullable
        final var tarDecompressor = ALGORITHMS.stream()
                .filter(a -> a.extensionMatcher.matcher(pathStr).matches())
                .findAny()
                .orElse(null);

        if (tarDecompressor != null) {
            return this.untar(arPath, tarDecompressor, targetDir);
        }

        return this.copySingleFile(arPath, targetDir);
    }

    /**
     * Unpacks a zip archive.
     *
     * @param zipPath the path to the zip file
     * @param targetDir the directory where to unzip into
     *
     * @return the files (not directories) that were unzipped
     *
     * @throws IOException if the file couldn't be unzipped
     */
    private Collection<Path> unzip(final Path zipPath, final Path targetDir) throws IOException {
        final var files = new ArrayList<Path>();

        logger.trace("Unpacking zip file {}", zipPath);

        try (var zip = new ZipFile(zipPath.toFile())) {
            var totalSize = 0L;

            final var entries = Collections.list(zip.getEntries());

            files.ensureCapacity(entries.size());

            // Create the directories first so a skeleton exists
            entries.stream()
                    .map(ZipArchiveEntry::getName)
                    .map(targetDir::resolve)
                    .map(d -> Files.isDirectory(d) ? d : d.getParent())
                    .forEach(this::createDirectory);

            // Then extract the actual files
            for (final var entry : entries) {
                if (entry.isDirectory()) {
                    continue;
                }

                final var entryPath = targetDir.resolve(entry.getName());
                this.assertWithinTargetDir(entryPath, targetDir);

                totalSize = this.trackSize(totalSize, entry);

                try (var in = zip.getInputStream(entry);
                     var out = Files.newOutputStream(entryPath)) {
                    IOUtils.copy(in, out);
                }
                files.add(entryPath);
            }
        } catch (final IOException | RuntimeException ex) {
            // Clean up in case of an error.
            // NB: this should NOT happen in a finally block
            this.cleanFiles(files);

            throw ex;
        }

        return files;
    }

    /**
     * Unpacks a tar archive.
     *
     * @param arPath the path to the archive to decompress
     * @param decompressor the decompression algorithm stack to use
     * @param targetDir the directory uncompressed files should be written to
     *
     * @return the files that were in the archive
     *
     * @throws IOException if uncompressing fails
     */
    private Collection<Path> untar(
            final Path arPath, final TarDecompressor decompressor, final Path targetDir
    ) throws IOException {
        final var files = new ArrayList<Path>();

        logger.trace("Unpacking {} file {}", decompressor.name, arPath);

        try (var pin = Files.newInputStream(arPath);
             var bin = new BufferedInputStream(pin);
             var tin = decompressor.streamTransformer.apply(bin);
             var tar = new ArchiveInputStreamIterableAdaptor(tin).iterator()
        ) {
            var totalSize = 0L;

            while (tar.hasNext()) {
                final var entry = tar.next();

                final var entryPath = targetDir.resolve(entry.getName());
                this.assertWithinTargetDir(entryPath, targetDir);

                if (entry.isDirectory()) {
                    this.createDirectory(entryPath);
                    continue;
                }

                totalSize = this.trackSize(totalSize, entry);

                this.createDirectory(entryPath.getParent());

                try (var out = Files.newOutputStream(entryPath)) {
                    IOUtils.copy(tin, out);
                }
                files.add(entryPath);
            }
        } catch (final IOException | RuntimeException ex) {
            // Clean up in case of an error.
            // NB: this should NOT happen in a finally block
            this.cleanFiles(files);
            throw ex;
        }

        return files;
    }

    /**
     * Attempts to delete all files in the given collection.
     *
     * @param files the files to delete
     */
    private void cleanFiles(final Collection<Path> files) {
        for (final var path : files) {
            try {
                Files.deleteIfExists(path);
            } catch (final Exception dex) {
                logger.warn("Unable to clean file {}", path.toAbsolutePath(), dex);
            }
        }
    }

    /**
     * Checks if the entry's path is within the submission's target directory. If it is not, a
     * {@link MaliciousArchiveException} is thrown.
     *
     * @param entryPath the entry's path
     * @param targetDir the submission's base dir the entry path should be within
     *
     * @throws MaliciousArchiveException if the entry's path is outside the target directory
     */
    private void assertWithinTargetDir(final Path entryPath, final Path targetDir) {
        if (!entryPath.startsWith(targetDir)) {
            throw new MaliciousArchiveException(
                    entryPath.toAbsolutePath()
                            + " is outside the extraction path "
                            + targetDir.toAbsolutePath()
            );
        }
    }

    /**
     * Keeps track of the total size of the submission, checking it it remains within configured
     * limits.
     *
     * @param totalSize the total size before adding the entry
     * @param entry the entry to be added
     *
     * @return the new total size
     */
    private long trackSize(final long totalSize, final ArchiveEntry entry) {
        final var newSize = totalSize + entry.getSize();
        final var maxSize = this.settings.getMaxUnpackedArchiveSize();

        if (newSize >= maxSize) {
            throw new MaliciousArchiveException(
                    "Archive is too big: limit is " + maxSize
                            + "; archive size so far is " + newSize
            );
        }

        return newSize;
    }

    /**
     * Copies a single file to a new directory.
     *
     * @param path the path to the file
     * @param targetDir the directory to copy to
     *
     * @return the path of the new file
     *
     * @throws IOException if the file couldn't be copied
     */
    private Collection<Path> copySingleFile(
            final Path path, final Path targetDir
    ) throws IOException {
        logger.debug("Could not determine archive type, treating {} as a regular file", path);

        final var newFile = targetDir.resolve(path.getFileName());
        try (var out = Files.newOutputStream(newFile)) {
            Files.copy(path, out);
            return Collections.singleton(newFile);
        } catch (final IOException ex) {
            // Clean up before rethrowing
            try {
                Files.deleteIfExists(newFile);
            } catch (final Exception dex) {
                logger.warn("Unable to clean file {}", newFile.toAbsolutePath(), ex);
            }
            throw ex;
        }
    }

    /**
     * Creates a directory.
     *
     * @param dir the directory to create
     */
    @Contract("null -> fail")
    private void createDirectory(final @Nullable Path dir) {
        assert dir != null : "Refusing to create root";
        try {
            Files.createDirectories(dir);
        } catch (final IOException ex) {
            // Handled by the job runner.
            throw new RuntimeException(ex);
        }
    }

    /**
     * A tar decompression stack.
     */
    private static final class TarDecompressor {
        /**
         * The name of the stack.
         */
        private final String name;

        /**
         * A regular expression matching a file extension this decompressor supports.
         */
        private final Pattern extensionMatcher;

        /**
         * A function mapping an input stream to the corresponding decompression stack.
         */
        private final DecompressionStackBuilder streamTransformer;

        /**
         * Creates a new tar decompressor.
         *
         * @param name the name of the stack
         * @param extensionMatcher the regular expression matching valid extensions
         * @param streamTransformer the stream stack transformer
         */
        @Contract(pure = true)
        private TarDecompressor(
                final String name,
                final Pattern extensionMatcher,
                final DecompressionStackBuilder streamTransformer
        ) {
            this.name = name;
            this.extensionMatcher = extensionMatcher;
            this.streamTransformer = streamTransformer;
        }
    }

    /**
     * A function transforming an input stream to an archive input stream.
     */
    @FunctionalInterface
    private interface DecompressionStackBuilder {
        ArchiveInputStream apply(InputStream inputStream) throws IOException;
    }
}
