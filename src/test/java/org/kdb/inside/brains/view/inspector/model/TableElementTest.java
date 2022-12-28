package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import kx.c;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;

import static org.junit.jupiter.api.Assertions.*;

class TableElementTest {
    @Test
    void realtime() {
        final c.Flip flip = new c.Flip(new c.Dict(new String[]{"name", "type"}, new Object[]{
                new String[]{"asd", "qwe"},
                new char[]{'s', 'j'}
        }));

        final TableElement t = new TableElement(".namespace", new Object[]{"mockTable", 100, flip, new String[]{"asd"}});
        assertFalse(t.isHistorical());
        assertEquals("mockTable", t.getName());
        assertEquals(".namespace.mockTable", t.getCanonicalName());
        assertEquals(100, t.getSize());
        assertEquals("2 columns, 100 rows, memorable", t.getLocationString());

        final TableElement.Column[] columns = t.getColumns();
        assertEquals(2, columns.length);
        assertColumn(columns[0], "asd", KdbType.SYMBOL, true);
        assertColumn(columns[1], "qwe", KdbType.LONG, false);

        assertEquals(0, t.getChildren().length);
    }

    @Test
    void historical() {
        final c.Flip flip = new c.Flip(new c.Dict(new String[]{"name", "type"}, new Object[]{
                new String[]{"date", "qwe"},
                new char[]{'d', 'j'}
        }));

        final TableElement t = new TableElement(".namespace", new Object[]{"mockTable", 100, flip, new String[]{"date"}});
        assertTrue(t.isHistorical());
        assertEquals("mockTable", t.getName());
        assertEquals(".namespace.mockTable", t.getCanonicalName());
        assertEquals(100, t.getSize());
        assertEquals("2 columns, 100 rows, historical", t.getLocationString());

        final TableElement.Column[] columns = t.getColumns();
        assertEquals(2, columns.length);
        assertColumn(columns[0], "date", KdbType.DATE, true);
        assertColumn(columns[1], "qwe", KdbType.LONG, false);

        assertEquals(0, t.getChildren().length);
    }

    private void assertColumn(TableElement.Column c, String name, KdbType type, boolean keyed) {
        assertEquals(name, c.getName());
        assertEquals(type, c.getType());
        assertEquals(keyed, c.isKeyed());
        assertSame(keyed ? KdbIcons.Node.TableKeyColumn : KdbIcons.Node.TableValueColumn, c.getIcon());
    }
}