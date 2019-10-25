package nl.f00f.unpacker.settings;

import org.jetbrains.annotations.Contract;

/**
 * Parameters for the unpacker.
 */
public interface UnpackerSettings {
    /**
     * Returns the maximum size of an unpacked archive.
     *
     * The size of an unpacked archive is the sum of the sizes of all its files.
     *
     * If an archive exceeds this size, a {@link nl.f00f.unpacker.MaliciousArchiveException}
     * is thrown.
     *
     * @return the maximum size in bytes
     */
    @Contract(pure = true)
    long getMaxUnpackedArchiveSize();
}
