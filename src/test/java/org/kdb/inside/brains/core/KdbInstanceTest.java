package org.kdb.inside.brains.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KdbInstanceTest {
    @Test
    void parse() {
        validateInstance("`:asd:9090:my:pass");
        validateInstance(":asd:9090:my:pass");
        validateInstance("`asd:9090:my:pass");
        validateInstance(" asd:9090:my:pass   ");
    }

    @Test
    void fake() {
        assertNull(KdbInstance.parseInstance("zvn 4e 32u890r4 q13r4q"));
    }

    private void validateInstance(String url) {
        final KdbInstance kdbInstance = KdbInstance.parseInstance(url);

        assertNotNull(kdbInstance);
        assertEquals(url, kdbInstance.getName());
        assertEquals("asd", kdbInstance.getHost());
        assertEquals(9090, kdbInstance.getPort());
        assertEquals("my:pass", kdbInstance.getCredentials());
        assertNull(kdbInstance.getOptions());
    }
}