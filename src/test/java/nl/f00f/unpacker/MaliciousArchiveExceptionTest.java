package nl.f00f.unpacker;

import net.wukl.exceptionverifier.ExceptionVerifier;
import org.junit.jupiter.api.Test;

public class MaliciousArchiveExceptionTest {
    @Test
    public void verify() {
        ExceptionVerifier.forClass(MaliciousArchiveException.class).verify();
    }
}
