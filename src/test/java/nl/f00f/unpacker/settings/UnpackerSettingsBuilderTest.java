package nl.f00f.unpacker.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UnpackerSettingsBuilderTest {
    private UnpackerSettingsBuilder unpackerSettingsBuilder;

    @BeforeEach
    public void before() {
        this.unpackerSettingsBuilder = new UnpackerSettingsBuilder();
    }

    @Test
    public void testDefault() {
        final var defaults = new DefaultUnpackerSettings();
        final var settings = this.unpackerSettingsBuilder.build();

        assertThat(settings.getMaxUnpackedArchiveSize())
                .isEqualTo(defaults.getMaxUnpackedArchiveSize());
    }

    @Test
    public void testModified() {
        final var newSize = 4;
        final var settings = this.unpackerSettingsBuilder
                .withMaxUnpackedArchiveSize(newSize)
                .build();

        assertThat(settings.getMaxUnpackedArchiveSize()).isEqualTo(newSize);
    }

    @Test
    public void testDoubleBuildSameValues() {
        final var newSize = 16;
        this.unpackerSettingsBuilder
                .withMaxUnpackedArchiveSize(newSize)
                .build();

        final var otherSettings = this.unpackerSettingsBuilder.build();

        assertThat(otherSettings.getMaxUnpackedArchiveSize()).isEqualTo(newSize);
    }

    @Test
    public void testDoubleBuildDistinctInstance() {
        final var firstSettings = this.unpackerSettingsBuilder.build();
        final var secondSettings = this.unpackerSettingsBuilder.build();

        assertThat(firstSettings).isNotSameAs(secondSettings);
    }
}
