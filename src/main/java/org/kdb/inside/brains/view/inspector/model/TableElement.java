package org.kdb.inside.brains.view.inspector.model;

import icons.KdbIcons;
import kx.c;
import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import java.lang.reflect.Array;
import java.util.Set;

public class TableElement extends ExecutableElement {
    private final long size;
    private final c.Flip meta;
    private final Set<String> keys;
    private final String location;

    private Column[] columns = null;

    public TableElement(String namespace, Object[] item) {
        super((String) item[0], namespace, KdbIcons.Node.Table);
        size = ((Number) item[1]).longValue();
        meta = (c.Flip) item[2];
        keys = Set.of((String[]) item[3]);
        location = Array.getLength(meta.y[0]) + " columns, " + size + " rows, " + (isHistorical() ? "historical" : "memorable");
    }

    public long getSize() {
        return size;
    }

    public Column[] getColumns() {
        if (columns == null) {
            columns = createColumns();
        }
        return columns;
    }

    private Column[] createColumns() {
        final String[] names = (String[]) meta.y[0];
        final char[] types = (char[]) meta.y[1];

        final Column[] columns = new Column[names.length];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = new Column(names[i], types[i], keys.contains(names[i]));
        }
        return columns;
    }

    @Override
    public String getLocationString() {
        return location;
    }

    public boolean isHistorical() {
        return "date".equals(Array.get(meta.y[0], 0));
    }

    public static final class Column {
        private final String name;
        private final KdbType type;
        private final boolean keyed;

        public Column(String name, char type, boolean keyed) {
            this.name = name;
            this.type = KdbType.typeOf(type);
            this.keyed = keyed;
        }

        public String getName() {
            return name;
        }

        public KdbType getType() {
            return type;
        }

        public boolean isKeyed() {
            return keyed;
        }

        public Icon getIcon() {
            return keyed ? KdbIcons.Node.TableKeyColumn : KdbIcons.Node.TableValueColumn;
        }
    }
}