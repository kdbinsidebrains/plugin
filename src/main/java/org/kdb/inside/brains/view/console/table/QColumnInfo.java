package org.kdb.inside.brains.view.console.table;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.ColumnInfo;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.settings.KdbSettingsService;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.Month;
import java.util.Comparator;

import static org.kdb.inside.brains.UIUtils.KEY_COLUMN_PREFIX;
import static org.kdb.inside.brains.UIUtils.KEY_COLUMN_PREFIX_XMAS;

public class QColumnInfo extends ColumnInfo<Object, Object> {
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

        if (columnClass != null) {
            if (Comparable.class.isAssignableFrom(columnClass) || columnClass.isPrimitive()) {
                comparator = (o1, o2) -> ((Comparable<Object>) o1).compareTo(o2);
            } else if (columnClass.isArray() && (Comparable.class.isAssignableFrom(columnClass.getComponentType()) || columnClass.getComponentType().isPrimitive())) {
                comparator = (o1, o2) -> {
                    final int l1 = Array.getLength(o1);
                    final int l2 = Array.getLength(o2);
                    final int l = Math.min(l1, l2);
                    for (int i = 0; i < l; i++) {
                        final int r = ((Comparable<Object>) Array.get(o1, i)).compareTo(Array.get(o2, i));
                        if (r != 0) {
                            return r;
                        }
                    }
                    return Integer.compare(l1, l2);
                };
            } else {
                comparator = null;
            }
        } else {
            comparator = null;
        }
    }

    public boolean isKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public KdbType getColumnType() {
        return columnType;
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
        return getName() + ": " + columnType.getTypeName();
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
