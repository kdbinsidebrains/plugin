package org.kdb.inside.brains.view.chart;

import org.jdom.Element;
import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Set;

public record ColumnDefinition(String name, KdbType type) {
    static final Set<KdbType> SYMBOL_TYPES = Set.of(KdbType.SYMBOL, KdbType.CHAR, KdbType.CHAR_LIST);

    static final Set<KdbType> NUMBER_TYPES = Set.of(KdbType.BYTE, KdbType.SHORT, KdbType.INT, KdbType.LONG, KdbType.REAL, KdbType.FLOAT);

    static final Set<KdbType> TEMPORAL_TYPES = Set.of(KdbType.SECOND, KdbType.MINUTE, KdbType.MONTH, KdbType.TIME, KdbType.DATE, KdbType.DATETIME, KdbType.TIMESPAN, KdbType.TIMESTAMP);

    public ColumnDefinition(String name, Class<?> type) {
        this(name, KdbType.typeOf(type));
    }

    public ColumnDefinition(ColumnDefinition d) {
        this(d.name, d.type);
    }

    public String getLabel() {
        return "<html>" + name + " <font color=\"gray\">(" + type.getTypeName().toLowerCase() + ")</font></html>";
    }

    public static ColumnDefinition restore(Element element) {
        if (element == null) {
            return null;
        }
        final String name = element.getAttributeValue("name");
        final KdbType type = KdbType.typeOf(element.getAttributeValue("type").charAt(0));
        return new ColumnDefinition(name, type);
    }

    public static boolean isSymbol(KdbType type) {
        return SYMBOL_TYPES.contains(type);
    }

    public static boolean isNumber(KdbType type) {
        return NUMBER_TYPES.contains(type);
    }

    public static boolean isTemporal(KdbType type) {
        return TEMPORAL_TYPES.contains(type);
    }

    public static TableCellRenderer createTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final ColumnDefinition cc = (ColumnDefinition) value;
                return super.getTableCellRendererComponent(table, cc == null ? null : cc.getLabel(), isSelected, hasFocus, row, column);
            }
        };
    }

    public static ListCellRenderer<Object> createListCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final ColumnDefinition cc = (ColumnDefinition) value;
                return super.getListCellRendererComponent(list, cc == null ? null : cc.getLabel(), index, isSelected, cellHasFocus);
            }
        };
    }

    public boolean isTemporal() {
        return isTemporal(type);
    }

    public String getLabelWidthTemplate() {
        return "  " + name + " (" + type.getTypeName().toLowerCase() + ")  ";
    }

    public Element store() {
        final Element e = new Element("column");
        e.setAttribute("name", name);
        e.setAttribute("type", String.valueOf(type.getCode()));
        return e;
    }

    public boolean isSymbol() {
        return isSymbol(type);
    }

    public boolean isNumber() {
        return isNumber(type);
    }

    @Override
    public String toString() {
        return "ColumnConfig{" + "name='" + name + '\'' + ", style=" + type + '}';
    }
}
