package org.kdb.inside.brains.view.console.table;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.ColumnInfo;
import kx.c;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.ConsoleOptions;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;

import static org.kdb.inside.brains.UIUtils.KEY_COLUMN_PREFIX;
import static org.kdb.inside.brains.UIUtils.KEY_COLUMN_PREFIX_XMAS;

public abstract class QTableModel implements TableModel {
    private final QColumnInfo[] columns;

    public static final QTableModel EMPTY_MODEL = new EmptyTableModel();

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

    public QColumnInfo[] getColumns() {
        return columns;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns[columnIndex].getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns[columnIndex].columnClass;
    }

    public String getDisplayColumnName(int columnIndex) {
        return columns[columnIndex].displayName;
    }

    public boolean isKeyColumn(int columnIndex) {
        return columns[columnIndex].key;
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

    public static class QColumnInfo extends ColumnInfo<Object, Object> {
        private final boolean key;
        private final KdbType columnType;
        private final Class<?> columnClass;
        private final Comparator<Object> comparator;
        private final String displayName;

        @SuppressWarnings("unchecked")
        public QColumnInfo(String name, Class<?> columnClass, boolean key) {
            super(name);
            this.key = key;
            this.columnClass = columnClass;
            this.columnType = KdbType.typeOf(columnClass);
            this.displayName = createDisplayName(name, key);

            if (columnClass != null && (Comparable.class.isAssignableFrom(columnClass) || columnClass.isPrimitive())) {
                comparator = (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
            } else {
                comparator = null;
            }
        }

        public String getDisplayName() {
            return displayName;
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
        public @NlsContexts.Tooltip @Nullable String getTooltipText() {
            return "Type: " + columnType.getTypeName();
        }

        @Override
        public @Nullable Comparator<Object> getComparator() {
            return comparator;
        }

        static QColumnInfo[] of(c.Flip flip, boolean key) {
            final int length = flip.x.length;
            QColumnInfo[] res = new QColumnInfo[length];
            for (int i = 0; i < length; i++) {
                res[i] = new QColumnInfo(flip.x[i], guessColumnType(flip.y[i]), key);
            }
            return res;
        }

        private static Class<?> guessColumnType(Object column) {
            // list of lists
            if (column.getClass().equals(Object[].class)) {
                Object[] obj = (Object[]) column;
                if (obj.length != 0) {
                    Class<?> c = obj[0].getClass();
                    for (int i = 1; i < obj.length; i++) {
                        if (c.equals(obj[i])) {
                            return Object.class;
                        }
                    }
                    return c;
                }
            }
            return column.getClass().getComponentType();
        }

        @NotNull
        private static String createDisplayName(String name, boolean key) {
            if (key) {
                if (KdbSettingsService.getInstance().getTableOptions().isXmasKeyColumn()) {
                    final LocalDate now = LocalDate.now();
                    if (now.getMonth() == Month.DECEMBER && now.getDayOfMonth() >= 14) {
                        return KEY_COLUMN_PREFIX_XMAS + " " + name;
                    }
                }
                return KEY_COLUMN_PREFIX + " " + name;
            }
            return name;
        }
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
