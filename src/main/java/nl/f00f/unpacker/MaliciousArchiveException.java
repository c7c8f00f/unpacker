package nl.f00f.unpacker;

/**
 * Indicates that the submitted archive could damage the system (hopefully accidentally) by (e.g.)
 * escaping its temporary extraction directory or bombing the memory.
 */
public class MaliciousArchiveException extends RuntimeException {
    /**
     * Creates a new malicious archive exception.
     */
    public MaliciousArchiveException() {
        super();
    }

    /**
     * Creates a new malicious archive exception.
     *
     * @param message the detail message explaining what caused the exception
     */
    public MaliciousArchiveException(final String message) {
        super(message);
    }

    /**
     * Creates a new malicious archive exception.
     *
     * @param cause the exception that caused this exception
     */
    public MaliciousArchiveException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new malicious archive exception.
     *
     * @param message the detail message explaining what caused the exception
     * @param cause   the exception that caused this exception
     */
    public MaliciousArchiveException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
