package org.kdb.inside.brains.view.treeview.actions;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.ScopeType;
import org.kdb.inside.brains.core.StructuralItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportQPadActionTest {
    @Test
    void importTest() {
        var text = "`:localhost:9001::`sdfgsdfg`52345`Current server\n" +
                "`:localhost:134`sdfgsdfg`3452345\n" +
                "`:asdfasdf:32234:asdf:234`a\n" +
                "`:asdfasdf:32234:asdf:`b\n" +
                "`:asdfasdf:32234:asdf`c";

        final StructuralItem scope = new KdbScope("asd", ScopeType.SHARED);
        ImportQPadAction.importData(text, scope);
        assertEquals(4, scope.getChildrenCount());

        final StructuralItem i1 = (StructuralItem) scope.getChild(0);
        assertEquals("sdfgsdfg", i1.getName());
        assertEquals(2, i1.getChildrenCount());

        final StructuralItem i2 = (StructuralItem) i1.getChild(0);
        assertEquals("52345", i2.getName());
        assertEquals(1, i2.getChildrenCount());

        final KdbInstance i3 = (KdbInstance) i2.getChild(0);
        assertEquals("Current server", i3.getName());
        assertEquals("localhost", i3.getHost());
        assertEquals(9001, i3.getPort());
        assertNull(i3.getCredentials());
        assertNull(i3.getOptions());

        final KdbInstance i4 = (KdbInstance) i1.getChild(1);
        assertEquals("3452345", i4.getName());
        assertEquals("localhost", i4.getHost());
        assertEquals(134, i4.getPort());
        assertNull(i4.getCredentials());
        assertNull(i4.getOptions());

        final KdbInstance i5 = (KdbInstance) scope.getChild(1);
        assertEquals("a", i5.getName());
        assertEquals("asdfasdf", i5.getHost());
        assertEquals(32234, i5.getPort());
        assertEquals("asdf:234", i5.getCredentials());
        assertNull(i5.getOptions());

        final KdbInstance i6 = (KdbInstance) scope.getChild(2);
        assertEquals("b", i6.getName());
        assertEquals("asdfasdf", i6.getHost());
        assertEquals(32234, i6.getPort());
        assertEquals("asdf", i6.getCredentials());
        assertNull(i6.getOptions());

        final KdbInstance i7 = (KdbInstance) scope.getChild(3);
        assertEquals("c", i7.getName());
        assertEquals("asdfasdf", i7.getHost());
        assertEquals(32234, i7.getPort());
        assertEquals("asdf", i7.getCredentials());
        assertNull(i7.getOptions());
    }
}