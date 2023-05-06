package org.kdb.inside.brains.view.chart;

import org.kdb.inside.brains.KdbType;

import java.util.Objects;

class ColumnKey {
    private final String name;
    private final KdbType type;

    public ColumnKey(ColumnConfig c) {
        name = c.getName();
        type = c.getType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnKey c = (ColumnKey) o;
        return name.equals(c.name) && type == c.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
