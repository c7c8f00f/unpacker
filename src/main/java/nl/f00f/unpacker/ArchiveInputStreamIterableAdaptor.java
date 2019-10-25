package nl.f00f.unpacker;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A single-use archive input stream iterable adaptor.
 */
public final class ArchiveInputStreamIterableAdaptor implements Iterable<ArchiveEntry> {
    /**
     * The input stream the entries are to be read from.
     */
    private final ArchiveInputStream in;

    /**
     * Creates a new archive input stream iterable adaptor.
     *
     * @param in the input stream the entries are to be read from
     */
    @Contract(pure = true)
    public ArchiveInputStreamIterableAdaptor(final ArchiveInputStream in) {
        this.in = in;
    }

    /**
     * Returns a new iterator iterating over the archive entries in the stream.
     *
     * Do not call this function multiple times as the returned iterator holds some state and
     * the input stream instance is shared.
     *
     * @return the iterator
     */
    @Override
    @Contract("-> new")
    public CloseableIterator<ArchiveEntry> iterator() {
        return new CloseableIterator<>() {
            /**
             * The input stream the entries are to be read from.
             */
            private final ArchiveInputStream in = ArchiveInputStreamIterableAdaptor.this.in;

            /**
             * The most recently read entry that will be the next entry returned when
             * {@link #next()} is called.
             */
            @Nullable
            private ArchiveEntry entryCache = null;

            /**
             * Ensures that the most recent value is present in the cache.
             */
            private void ensurePresentInCache() {
                if (this.entryCache != null) {
                    return;
                }

                try {
                    this.entryCache = this.in.getNextEntry();
                } catch (final IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public boolean hasNext() {
                this.ensurePresentInCache();

                return this.entryCache != null;
            }

            @Override
            public ArchiveEntry next() {
                this.ensurePresentInCache();

                if (this.entryCache == null) {
                    throw new NoSuchElementException(
                            "ArchiveInputStream#getNextEntry returned null"
                    );
                }

                final var entry = this.entryCache;
                this.entryCache = null;

                return entry;
            }

            @Override
            public void close() throws IOException {
                this.in.close();
            }
        };
    }

    public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {
        @Override
        void close() throws IOException;
    }
}
