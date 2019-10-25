package nl.f00f.unpacker;

import nl.f00f.unpacker.settings.UnpackerSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class UnpackerTest {
    private Path temp;
    private UnpackerSettings settings;
    private Unpacker unpacker;
    private Path path;

    @BeforeEach
    public void before() throws IOException {
        this.temp = Files.createTempDirectory("unpacker-test-");

        this.settings = mock(UnpackerSettings.class);

        this.unpacker = new Unpacker(this.settings);

        doReturn(512 * 1024 * 1024L).when(this.settings).getMaxUnpackedArchiveSize();
    }

    @Test
    public void testUnzip() throws IOException {
        this.copyOut("zipfile", "zip");

        final var files = this.unpacker.unpack(this.path, this.temp);

        assertThat(files).allMatch(p -> p.endsWith("nijlgans.jpeg"));
    }

    @Test
    public void testUnzipStarts() throws IOException {
        this.copyOut("zipfile", "zip");

        final var files = this.unpacker.unpack(this.path, this.temp);

        assertThat(files).allMatch(p -> p.endsWith("nijlgans.jpeg"));
    }


    @Test
    public void testUnzipTooLarge() throws IOException {
        doReturn(1024 * 1024L).when(this.settings).getMaxUnpackedArchiveSize();

        this.copyOut("zipfile", "zip");

        assertThrows(MaliciousArchiveException.class,
                () -> this.unpacker.unpack(this.path, this.temp)
        );
    }

    @Test
    public void tesUntarGz() throws IOException {
        this.copyOut("tgzfile", "tar.gz");

        final var files = this.unpacker.unpack(this.path, this.temp);
        assertThat(files).allMatch(p -> p.endsWith("vercingetorix.s3m"));
    }

    @Test
    public void testUntarXz() throws IOException {
        this.copyOut("txzfile", "tar.xz");

        final var files = this.unpacker.unpack(this.path, this.temp);
        assertThat(files).allMatch(p -> p.endsWith(
                "jack-black-is-dancing-at-an-octagon-party-but-you-were-not-invited.mp3"
        ));
    }

    @Test
    public void testUntarXzTooLarge() throws IOException {
        doReturn(1024 * 1024L).when(this.settings).getMaxUnpackedArchiveSize();

        this.copyOut("txzfile", "tar.xz");

        assertThrows(MaliciousArchiveException.class,
                () -> this.unpacker.unpack(this.path, this.temp)
        );
    }

    @Test
    public void testUntarBz2WithNestedDirectories() throws IOException {
        this.copyOut("tb2file", "tar.bz2");

        final var files = this.unpacker.unpack(this.path, this.temp);
        assertThat(files).containsExactlyInAnyOrderElementsOf(this.inTemp(
                "CMakeLists.txt",
                "test.s3m",
                "src/audio.c",
                "src/main.c",
                "src/s3m.c",
                "src/s3m.h",
                "src/slopt/CMakeLists.txt",
                "src/slopt/opt.c",
                "src/slopt/opt.h"
        ));
    }

    @Test
    public void testUnzipWithNestedDirectories() throws IOException {
        this.copyOut("deepzipfile", "zip");

        final var files = this.unpacker.unpack(this.path, this.temp);
        assertThat(files).containsExactlyInAnyOrderElementsOf(this.inTemp(
                "lbs/makefile",
                "lbs/res/sprites/0-0.png",
                "lbs/res/sprites/0-1.png",
                "lbs/res/sprites/1-0.png",
                "lbs/res/sprites/1-1.png",
                "lbs/res/sprites/frog.png",
                "lbs/res/sprites/player.png",
                "lbs/src/lbs-error.h",
                "lbs/src/main.c"
        ));
    }

    @Test
    public void testAbsoluteTar() throws IOException {
        this.copyOut("abs", "tar");

        assertThrows(MaliciousArchiveException.class,
                () -> this.unpacker.unpack(this.path, this.temp)
        );
    }

    @Test
    public void testNotAnArchive() throws IOException {
        this.copyOut("bloop", "xpf");

        final var files = this.unpacker.unpack(this.path, this.temp);
        // Can't assert the exact name here since the name is mangled by copyOut
        assertThat(files).hasSize(1);
        assertThat(files).allMatch(n -> n.toString().endsWith("xpf"));
    }

    private Collection<Path> inTemp(final String... paths) {
        return Arrays.stream(paths)
                       .map(this.temp::resolve)
                       .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Copies a test resource to the file system.
     *
     * @param resource the name of the resource without the extension
     * @param extension the extension excluding the dot
     *
     * @return {@code this.path}
     *
     * @throws IOException if copying fails
     */
    private Path copyOut(final String resource, final String extension) throws IOException {
        final var fres = "/nl/f00f/unpacker/" + resource + '.' + extension;
        this.path = Files.createTempFile("unpacker-test-", '-' + resource + '.' + extension);
        try (var in = UnpackerTest.class.getResourceAsStream(fres);
             var out = Files.newOutputStream(this.path)
        ) {
            assert in != null : "Unable to find resource " + fres + " to copy out";
            in.transferTo(out);
        }

        return this.path;
    }

    @AfterEach
    public void after() throws IOException {
        FilesystemTestingHelper.removeDirectory(this.temp);
        Files.deleteIfExists(this.path);
    }
}
