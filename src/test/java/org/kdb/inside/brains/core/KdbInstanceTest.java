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
        final KdbInstance instance = KdbInstance.parseInstance(url);

        assertNotNull(instance);
        assertEquals(url, instance.getName());
        assertEquals("asd", instance.getHost());
        assertEquals(9090, instance.getPort());
        assertEquals("my:pass", instance.getCredentials());
        assertNotNull(instance.getOptions());
    }
}