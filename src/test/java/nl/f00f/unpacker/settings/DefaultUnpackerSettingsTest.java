package nl.f00f.unpacker.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultUnpackerSettingsTest {
    private UnpackerSettings unpackerSettings;

    @BeforeEach
    public void before() {
        this.unpackerSettings = new DefaultUnpackerSettings();
    }

    @Test
    public void testSensibleMaxUnpackedArchiveSize() {
        assertThat(this.unpackerSettings.getMaxUnpackedArchiveSize()).isGreaterThan(0);
    }
}
