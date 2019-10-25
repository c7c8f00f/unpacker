package nl.f00f.unpacker.settings;

import org.jetbrains.annotations.Contract;

/**
 * A builder for unpacker settings.
 *
 * This builder is multi-use. Each call to {@link #build()} will return a distinct instance of
 * an {@link UnpackerSettings} implementation.
 */
@SuppressWarnings({"ReturnOfThis", "customary for the builder pattern"})
public class UnpackerSettingsBuilder {
    /**
     * The settings object under construction.
     */
    private UnpackerSettingsImpl settings;

    /**
     * Creates a new unpacker settings builder.
     *
     * The settings object under construction is prepopulated with values from
     * {@link DefaultUnpackerSettings}.
     */
    public UnpackerSettingsBuilder() {
        this.settings = new UnpackerSettingsImpl();
        this.fromUnpackerSettings(new DefaultUnpackerSettings());
    }

    /**
     * Copies the values of another settings object into the object under construction.
     *
     * @param unpackerSettings the settings to copy
     *
     * @return the builder
     */
    public UnpackerSettingsBuilder fromUnpackerSettings(final UnpackerSettings unpackerSettings) {
        return this.withMaxUnpackedArchiveSize(unpackerSettings.getMaxUnpackedArchiveSize());
    }

    /**
     * Sets the maximum unpacked archive size.
     *
     * @param size the size
     *
     * @return the builder
     *
     * @see UnpackerSettings#getMaxUnpackedArchiveSize()
     */
    public UnpackerSettingsBuilder withMaxUnpackedArchiveSize(final long size) {
        this.settings.maxUnpackedArchiveSize = size;
        return this;
    }

    /**
     * Builds an instance of unpacker settings with the configured values.
     *
     * Subsequent calls will return distinct instances, though with the same values unless the
     * builder was modified between builds.
     *
     * @return the instance
     */
    @Contract(pure = false)
    public UnpackerSettings build() {
        final var oldSettings = this.settings;
        this.settings = new UnpackerSettingsImpl();
        this.fromUnpackerSettings(oldSettings);
        return oldSettings;
    }

    /**
     * The internal implementation class.
     */
    private static final class UnpackerSettingsImpl implements UnpackerSettings {
        /**
         * The maximum unpacked archive size.
         */
        private long maxUnpackedArchiveSize;

        @Override
        public long getMaxUnpackedArchiveSize() {
            return this.maxUnpackedArchiveSize;
        }
    }
}
