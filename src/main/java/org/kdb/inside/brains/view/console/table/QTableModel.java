package org.kdb.inside.brains.view.console.table;

import com.intellij.util.ui.SortableColumnModel;
import kx.c;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.ConsoleOptions;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.lang.reflect.Array;

public abstract class QTableModel implements TableModel, SortableColumnModel {
    private final QColumnInfo[] columns;

    public static final QTableModel EMPTY_MODEL = new EmptyTableModel();

    protected QTableModel(QColumnInfo[] columns) {
        this.columns = columns;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        // read-only
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        // read-only
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // read-only
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // read-only
        throw new UnsupportedOperationException("Read-only model");
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public QColumnInfo[] getColumnInfos() {
        return columns;
    }

    public QColumnInfo getColumnInfo(int columnIndex) {
        return columns[columnIndex];
    }

    @Override
    public RowSorter.@Nullable SortKey getDefaultSortKey() {
        return null;
    }

    @Override
    public Object getRowValue(int i) {
        return null;
    }

    @Override
    public boolean isSortable() {
        return true;
    }

    @Override
    public void setSortable(boolean b) {
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns[columnIndex].getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns[columnIndex].getColumnClass();
    }

    public boolean isKeyColumn(TableColumn column) {
        return columns[column.getModelIndex()].isKey();
    }

    @Nullable
    public static QTableModel from(Object k) {
        if (k instanceof c.Flip) {
            return new SimpleTableModel((c.Flip) k);
        }

        final ConsoleOptions options = KdbSettingsService.getInstance().getConsoleOptions();
        if (KdbType.isNotEmptyList(k) && options.isListAsTable()) {
            return new ListTableModel(k);
        }

        if (k instanceof c.Dict dict) {
            final Object x = dict.x;
            final Object y = dict.y;

            final boolean xa = x.getClass().isArray();
            final boolean ya = y.getClass().isArray();
            if (xa && ya) {
                if (options.isDictAsTable()) {
                    return new DictTableModel(x, y);
                }
            } else {
                if ((x instanceof c.Flip || xa) && (y instanceof c.Flip || ya)) {
                    return new DictTableModel(x, y);
                }
            }
        }
        return null;
    }

    private static class EmptyTableModel extends QTableModel {
        private static final QColumnInfo[] EMPTY_COLUMNS = new QColumnInfo[0];

        private EmptyTableModel() {
            super(EMPTY_COLUMNS);
        }

        @Override
        public int getRowCount() {
            return 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return null;
        }
    }

    public static class ListTableModel extends QTableModel {
        private final Object array;
        private final int rowsCount;

        protected ListTableModel(Object array) {
            this(array, array.getClass().getComponentType());
        }

        private ListTableModel(Object array, Class<?> type) {
            super(new QColumnInfo[]{new QColumnInfo(KdbType.typeOf(type).getTypeName(), type, false)});
            this.array = array;
            this.rowsCount = Array.getLength(array);
        }

        @Override
        public int getRowCount() {
            return rowsCount;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return Array.get(array, rowIndex);
        }
    }

    public static class DictTableModel extends QTableModel {
        private final Object keys;
        private final Object values;

        private final int keysCount;
        private final int rowsCount;

        private DictTableModel(Object keys, Object values) {
            this(keys, cols(keys, true), values, cols(values, false));
        }

        private DictTableModel(Object keys, QColumnInfo[] keysInfo, Object values, QColumnInfo[] valuesInfo) {
            super(ArrayUtils.addAll(keysInfo, valuesInfo));
            this.keys = keys;
            this.values = values;

            keysCount = keysInfo.length;
            rowsCount = Array.getLength(keys instanceof c.Flip ? ((c.Flip) keys).y[0] : keys);
        }

        private static QColumnInfo[] cols(Object v, boolean key) {
            if (v instanceof c.Flip) {
                return QColumnInfo.of((c.Flip) v, key);
            }
            return new QColumnInfo[]{
                    new QColumnInfo(key ? "Key" : "Value", v.getClass().getComponentType(), key)
            };
        }

        @Override
        public int getRowCount() {
            return rowsCount;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final Object row = getRow(columnIndex);
            return Array.get(row, rowIndex);
        }

        public Object getRow(int columnIndex) {
            final Object o = columnIndex < keysCount ? keys : values;
            if (o instanceof c.Flip flip) {
                final int index = columnIndex < keysCount ? columnIndex : columnIndex - keysCount;
                return flip.y[index];
            }
            return o;
        }
    }

    public static class SimpleTableModel extends QTableModel {
        final c.Flip flip;
        final int rowsCount;

        private SimpleTableModel(c.Flip flip) {
            super(QColumnInfo.of(flip, false));
            this.flip = flip;
            this.rowsCount = Array.getLength(flip.y[0]);
        }

        @Override
        public int getRowCount() {
            return rowsCount;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return Array.get(flip.y[columnIndex], rowIndex);
        }
    }
}
