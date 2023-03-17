package org.kdb.inside.brains.core;

import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstanceOptionsTest {
    @Test
    void builder() {
        InstanceOptions.Builder b = new InstanceOptions.Builder();
        assertFalse(b.create().hasTls());
        assertFalse(b.create().isSafeTls());
        assertFalse(b.create().hasZip());
        assertFalse(b.create().isSafeZip());
        assertFalse(b.create().hasAsync());
        assertFalse(b.create().isSafeAsync());
        assertFalse(b.create().hasTimeout());
        assertEquals(InstanceOptions.DEFAULT_TIMEOUT, b.create().getSafeTimeout());
        assertOptions(b.create(), null, null, null, null);

        b.tls(true);
        assertTrue(b.create().hasTls());
        assertTrue(b.create().isSafeTls());
        assertFalse(b.create().hasZip());
        assertFalse(b.create().isSafeZip());
        assertFalse(b.create().hasAsync());
        assertFalse(b.create().isSafeAsync());
        assertFalse(b.create().hasTimeout());
        assertEquals(InstanceOptions.DEFAULT_TIMEOUT, b.create().getSafeTimeout());
        assertOptions(b.create(), true, null, null, null);

        b.zip(true);
        assertTrue(b.create().hasTls());
        assertTrue(b.create().isSafeTls());
        assertTrue(b.create().hasZip());
        assertTrue(b.create().isSafeZip());
        assertFalse(b.create().hasAsync());
        assertFalse(b.create().isSafeAsync());
        assertFalse(b.create().hasTimeout());
        assertEquals(InstanceOptions.DEFAULT_TIMEOUT, b.create().getSafeTimeout());
        assertOptions(b.create(), true, true, null, null);

        b.async(true);
        assertTrue(b.create().hasTls());
        assertTrue(b.create().isSafeTls());
        assertTrue(b.create().hasZip());
        assertTrue(b.create().isSafeZip());
        assertTrue(b.create().hasAsync());
        assertTrue(b.create().isSafeAsync());
        assertFalse(b.create().hasTimeout());
        assertEquals(InstanceOptions.DEFAULT_TIMEOUT, b.create().getSafeTimeout());
        assertOptions(b.create(), true, true, true, null);

        b.timeout(2333);
        assertTrue(b.create().hasTls());
        assertTrue(b.create().isSafeTls());
        assertTrue(b.create().hasZip());
        assertTrue(b.create().isSafeZip());
        assertTrue(b.create().hasAsync());
        assertTrue(b.create().isSafeAsync());
        assertTrue(b.create().hasTimeout());
        assertEquals(2333, b.create().getSafeTimeout());
        assertOptions(b.create(), true, true, true, 2333);
    }

    @Test
    void parameters() {
        final InstanceOptions o1 = InstanceOptions.INHERITED;
        assertSame(o1, InstanceOptions.fromParameters(null));
        assertEquals("", o1.toParameters());

        final InstanceOptions o2 = InstanceOptions.fromParameters("");
        assertOptions(o2, null, null, null, null);
        assertEquals("", o2.toParameters());

        final InstanceOptions o3 = InstanceOptions.fromParameters("tls=true&zip=false&async=true&timeout=3000&mock=dfsdf");
        assertOptions(o3, true, false, true, 3000);
        assertEquals("tls=true&zip=false&async=true&timeout=3000", o3.toParameters());
    }

    @Test
    void xml() {
        final Element e = new Element("mock");

        InstanceOptions.Builder b = new InstanceOptions.Builder();

        b.create().store(e);
        assertEquals(b.create(), InstanceOptions.restore(e));

        b.tls(true);
        b.create().store(e);
        assertEquals("true", e.getAttributeValue("tls"));
        assertEquals(b.create(), InstanceOptions.restore(e));

        b.zip(true);
        b.create().store(e);
        assertEquals("true", e.getAttributeValue("zip"));
        assertEquals(b.create(), InstanceOptions.restore(e));

        b.async(true);
        b.create().store(e);
        assertEquals("true", e.getAttributeValue("async"));
        assertEquals(b.create(), InstanceOptions.restore(e));

        b.timeout(2343);
        b.create().store(e);
        assertEquals("2343", e.getAttributeValue("timeout"));
        assertEquals(b.create(), InstanceOptions.restore(e));
    }

    @Test
    void safe() {
        InstanceOptions.Builder b = new InstanceOptions.Builder().safe();
        assertTrue(b.create().hasTls());
        assertFalse(b.create().isSafeTls());
        assertTrue(b.create().hasZip());
        assertFalse(b.create().isSafeZip());
        assertTrue(b.create().hasAsync());
        assertFalse(b.create().isSafeAsync());
        assertTrue(b.create().hasTimeout());
        assertEquals(1000, b.create().getSafeTimeout());
        assertOptions(b.create(), false, false, false, 1000);
    }

    private void assertOptions(InstanceOptions options, Boolean tsl, Boolean zip, Boolean async, Integer timeout) {
        if (tsl == null) {
            assertFalse(options.hasTls());
        } else {
            assertEquals(tsl, options.isSafeTls());
        }

        if (zip == null) {
            assertFalse(options.hasZip());
        } else {
            assertEquals(zip, options.isSafeZip());
        }

        if (async == null) {
            assertFalse(options.hasAsync());
        } else {
            assertEquals(async, options.isSafeAsync());
        }

        if (timeout == null) {
            assertFalse(options.hasTimeout());
        } else {
            assertEquals(timeout, options.getSafeTimeout());
        }
    }
}