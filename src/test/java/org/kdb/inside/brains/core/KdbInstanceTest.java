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

    @Test
    void updateAddress() {
        final KdbInstance i = new KdbInstance("name", "host", 1010, null);
        assertEquals("`:host:1010", i.toAddress());

        i.updateAddress("`:host1:2010");
        assertEquals("`:host1:2010", i.toAddress());

        i.updateAddress("`host2:3010");
        assertEquals("`:host2:3010", i.toAddress());

        i.updateAddress(":host3:4010");
        assertEquals("`:host3:4010", i.toAddress());

        i.updateAddress("host4:5010");
        assertEquals("`:host4:5010", i.toAddress());

        assertThrows(IllegalArgumentException.class, () -> i.updateAddress("asdf"));
        assertEquals("`:host4:5010", i.toAddress());

        assertThrows(IllegalArgumentException.class, () -> i.updateAddress("asdf:qwer"));
        assertEquals("`:host4:5010", i.toAddress());
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