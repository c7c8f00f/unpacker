package nl.f00f.unpacker;

import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FilesystemTestingHelper {
    private static final Logger logger = LoggerFactory.getLogger(FilesystemTestingHelper.class);

    /**
     * Do not instantiate.
     */
    @Contract(" -> fail")
    private FilesystemTestingHelper() {
        throw new AssertionError("Do not instantiate");
    }

    /**
     * Removes a directory recursively.
     *
     * @param path the path to the directory
     *
     * @throws IOException if walking the directory fails
     */
    public static void removeDirectory(final Path path) throws IOException {
        Files.walk(path)
                // Sort the list in reverse so the deepest files are deleted first
                .sorted((a, b) ->
                                Integer.compare(
                                        b.toAbsolutePath().toString().length(),
                                        a.toAbsolutePath().toString().length()
                                )
                )
                .forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (final IOException ex) {
                        logger.warn("Unable to clean up after test", ex);
                    }
                });
    }
}
