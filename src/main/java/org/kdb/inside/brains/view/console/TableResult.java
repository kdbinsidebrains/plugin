package org.kdb.inside.brains.view.console;

import com.intellij.util.ui.ColumnInfo;
import kx.c;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.Array;
import java.util.Comparator;

public class TableResult {
    private final KdbQuery query;
    private final KdbResult result;
    private final QTableModel tableModel;

    private static final String KEY_COLUMN_PREFIX = "\u00A1 ";

    public static final QTableModel EMPTY_MODEL = new EmptyTableModel();

    private TableResult(KdbQuery query, KdbResult result, QTableModel tableModel) {
        this.query = query;
        this.result = result;
        this.tableModel = tableModel;
    }

    public KdbQuery getQuery() {
        return query;
    }

    public KdbResult getResult() {
        return result;
    }

    public QTableModel getTableModel() {
        return tableModel;
    }

    public TableResult copy() {
        return new TableResult(query, result, tableModel);
    }

    public static TableResult from(KdbQuery query, KdbResult result) {
        final Object k = result.getObject();

        QTableModel model = null;
        if (k instanceof c.Flip) {
            model = new SimpleTableModel((c.Flip) k);
        } else if (k instanceof c.Dict) {
            final c.Dict dict = (c.Dict) k;
            if (dict.x instanceof c.Flip && dict.y instanceof c.Flip) {
                model = new KeyedTableMode((c.Flip) dict.x, (c.Flip) dict.y);
            } else {
                model = new DictTableMode(dict);
            }
        }
        return model == null ? null : new TableResult(query, result, model);
    }

    public static abstract class QTableModel implements TableModel {
        final QColumnInfo[] columns;

        static final QColumnInfo[] EMPTY_COLUMNS = new QColumnInfo[0];

        protected QTableModel(QColumnInfo[] columns) {
            this.columns = columns;
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            throw new UnsupportedOperationException("Read-only model");
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Nls
        @Override
        public String getColumnName(int columnIndex) {
            return columns[columnIndex].getName();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columns[columnIndex].getColumnClass();
        }

        public boolean isKeyColumn(int column) {
            return columns[column].key;
        }
    }

    private static class EmptyTableModel extends QTableModel {
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

    public static class DictTableMode extends QTableModel {
        final c.Dict dict;

        private DictTableMode(c.Dict dict) {
            super(QColumnInfo.of(dict));
            this.dict = dict;
        }

        @Override
        public int getRowCount() {
            return Array.getLength(dict.x);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object row = columnIndex == 0 ? dict.x : dict.y;
            return Array.get(row, rowIndex);
        }
    }

    public static class KeyedTableMode extends QTableModel {
        final c.Flip keys;
        final c.Flip values;

        public KeyedTableMode(c.Flip keys, c.Flip values) {
            super(QColumnInfo.of(keys, values));
            this.keys = keys;
            this.values = values;
        }

        @Override
        public int getRowCount() {
            return Array.getLength(keys.y[0]);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Object row = columnIndex < keys.x.length ? keys.y[columnIndex] : values.y[columnIndex - keys.y.length];
            return Array.get(row, rowIndex);
        }
    }

    public static class SimpleTableModel extends QTableModel {
        final c.Flip flip;

        private SimpleTableModel(c.Flip flip) {
            super(QColumnInfo.of(flip));
            this.flip = flip;
        }

        @Override
        public int getRowCount() {
            return Array.getLength(flip.y[0]);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return Array.get(flip.y[columnIndex], rowIndex);
        }
    }

    protected static class QColumnInfo extends ColumnInfo<Object, Object> {
        private final boolean key;
        private final Class<?> columnClass;
        private final Comparator<Object> comparator;

        @SuppressWarnings("unchecked")
        public QColumnInfo(String name, Class<?> columnClass, boolean key) {
            super(name);
            this.key = key;
            this.columnClass = columnClass;

            if (columnClass != null && (Comparable.class.isAssignableFrom(columnClass) || columnClass.isPrimitive())) {
                comparator = (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
            } else {
                comparator = null;
            }
        }

        @Override
        public Class<?> getColumnClass() {
            return columnClass;
        }

        @Override
        public boolean isCellEditable(Object o) {
            return false;
        }

        @Override
        public @Nullable Object valueOf(Object o) {
            return o;
        }

        @Override
        public @Nullable Comparator<Object> getComparator() {
            return comparator;
        }

        static QColumnInfo[] of(c.Dict dict) {
            return new QColumnInfo[]{
                    new QColumnInfo(KEY_COLUMN_PREFIX + "Key", dict.x.getClass().getComponentType(), true),
                    new QColumnInfo("Value", dict.y.getClass().getComponentType(), false)
            };
        }

        static QColumnInfo[] of(c.Flip flip) {
            final int length = flip.x.length;
            QColumnInfo[] res = new QColumnInfo[length];
            for (int i = 0; i < length; i++) {
                res[i] = QColumnInfo.from(flip, i, false);
            }
            return res;
        }

        static QColumnInfo[] of(c.Flip keys, c.Flip vals) {
            final int kl = keys.x.length;
            final int vl = vals.x.length;

            QColumnInfo[] res = new QColumnInfo[kl + vl];
            for (int i = 0; i < kl; i++) {
                res[i] = QColumnInfo.from(keys, i, true);
            }
            for (int i = 0; i < vl; i++) {
                res[kl + i] = QColumnInfo.from(vals, i, false);
            }
            return res;
        }

        static QColumnInfo from(c.Flip flip, int index, boolean key) {
            return new QColumnInfo(flip.x[index], flip.y[index].getClass().getComponentType(), key);
        }
    }
}
