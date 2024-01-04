package org.kdb.inside.brains.view.chart;

import org.jdom.Element;
import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Objects;
import java.util.Set;

public class ColumnConfig {
    private final String name;
    private final KdbType type;

    protected static final Set<KdbType> NUMBER_TYPES = Set.of(KdbType.BYTE, KdbType.SHORT, KdbType.INT, KdbType.LONG, KdbType.REAL, KdbType.FLOAT);

    protected static final Set<KdbType> TEMPORAL_TYPES = Set.of(KdbType.SECOND, KdbType.MINUTE, KdbType.MONTH, KdbType.TIME, KdbType.DATE, KdbType.DATETIME, KdbType.TIMESPAN, KdbType.TIMESTAMP);

    public ColumnConfig(String name, Class<?> type) {
        this(name, KdbType.typeOf(type));
    }

    public ColumnConfig(String name, KdbType type) {
        this.name = Objects.requireNonNull(name);
        this.type = Objects.requireNonNull(type);
    }

    public String getName() {
        return name;
    }

    public KdbType getType() {
        return type;
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

    public static ColumnConfig restore(Element element) {
        if (element == null) {
            return null;
        }
        final String name = element.getAttributeValue("name");
        final KdbType type = KdbType.typeOf(element.getAttributeValue("type").charAt(0));
        return new ColumnConfig(name, type);
    }

    public static ColumnConfig copy(ColumnConfig c) {
        if (c == null) {
            return null;
        }
        return new ColumnConfig(c.getName(), c.getType());
    }

    public static boolean isNumberType(KdbType type) {
        return NUMBER_TYPES.contains(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnConfig that)) return false;
        return Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    public Element store() {
        final Element e = new Element("column");
        e.setAttribute("name", name);
        e.setAttribute("type", String.valueOf(type.getCode()));
        return e;
    }

    @Override
    public String toString() {
        return "ColumnConfig{" + "name='" + name + '\'' + ", type=" + type + '}';
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
