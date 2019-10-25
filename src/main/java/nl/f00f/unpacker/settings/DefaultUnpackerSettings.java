package nl.f00f.unpacker.settings;

/**
 * Sensible defaults for unpacker settings.
 */
public final class DefaultUnpackerSettings implements UnpackerSettings {
    /**
     * One kilobyte, in bytes.
     */
    private static final long KILOBYTE = 1024L;

    /**
     * One megabyte, in bytes.
     */
    private static final long MEGABYTE = 1024 * KILOBYTE;

    /**
     * One gigabyte, in bytes.
     */
    private static final long GIGABYTE = 1024 * MEGABYTE;

    /**
     * Returns the maximum size of an unpacked archive, 2 GiB.
     *
     * @return the maximum size of an unpacked archive
     */
    @Override
    public long getMaxUnpackedArchiveSize() {
        return 2 * GIGABYTE;
    }
}
