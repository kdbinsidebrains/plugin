package org.kdb.inside.brains.view.chart;

import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;
import java.util.Set;

public class ColumnConfig {
    private final int index;
    private final String name;
    private final KdbType type;

    protected static final Set<KdbType> NUMBER_TYPES = Set.of(
            KdbType.BYTE,
            KdbType.SHORT,
            KdbType.INT,
            KdbType.LONG,
            KdbType.REAL,
            KdbType.FLOAT
    );

    protected static final Set<KdbType> TEMPORAL_TYPES = Set.of(
            KdbType.SECOND,
            KdbType.MINUTE,
            KdbType.MONTH,
            KdbType.TIME,
            KdbType.DATE,
            KdbType.DATETIME,
            KdbType.TIMESPAN,
            KdbType.TIMESTAMP
    );

    public ColumnConfig(int index, String name, Class<?> type) {
        this(index, name, KdbType.typeOf(type));
    }

    public ColumnConfig(int index, String name, KdbType type) {
        this.index = index;
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }


    public String getName() {
        return name;
    }

    public KdbType getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    public String getLabel() {
        return "<html>" + name + " <font color=\"gray\">(" + type.getTypeName().toLowerCase() + ")</font></html>";
    }

    public String getLabelWidth() {
        return "  " + name + " (" + type.getTypeName().toLowerCase() + ")  ";
    }

    public static boolean isTemporal(KdbType type) {
        return TEMPORAL_TYPES.contains(type);
    }

    public boolean isNumber() {
        return isNumberType(type);
    }

    public boolean isTemporal() {
        return isTemporal(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ColumnConfig that = (ColumnConfig) o;
        return index == that.index && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, name, type);
    }

    public static boolean isNumberType(KdbType type) {
        return NUMBER_TYPES.contains(type);
    }

    @Override
    public String toString() {
        return "ColumnConfig{" +
                "index=" + index +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    public static TableCellRenderer createTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final ColumnConfig cc = (ColumnConfig) value;
                return super.getTableCellRendererComponent(table, cc == null ? null : cc.getLabel(), isSelected, hasFocus, row, column);
            }
        };
    }

    public static ListCellRenderer<Object> createListCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final ColumnConfig cc = (ColumnConfig) value;
                return super.getListCellRendererComponent(list, cc == null ? null : cc.getLabel(), index, isSelected, cellHasFocus);
            }
        };
    }
}
