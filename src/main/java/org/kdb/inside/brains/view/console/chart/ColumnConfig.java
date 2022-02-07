package org.kdb.inside.brains.view.console.chart;

import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
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

    public ColumnConfig(int index, String name, KdbType type) {
        this.index = index;
        this.name = name;
        this.type = type;
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

    public static boolean isNumberType(KdbType type) {
        return NUMBER_TYPES.contains(type);
    }

    public static boolean isTemporalType(KdbType type) {
        return TEMPORAL_TYPES.contains(type);
    }

    public static TableCellRenderer createTableCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final ColumnConfig cc = (ColumnConfig) value;
                return super.getTableCellRendererComponent(table, cc.getLabel(), isSelected, hasFocus, row, column);
            }
        };
    }

    public static ListCellRenderer<Object> createListCellRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, ((ColumnConfig) value).getLabel(), index, isSelected, cellHasFocus);
            }
        };
    }
}
