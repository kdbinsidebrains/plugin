package org.kdb.inside.brains.view.chart;

import kx.c;
import org.jfree.data.time.*;

import javax.swing.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public interface ChartDataProvider {
    static ChartDataProvider copy(JTable table) {
        final int rowsCount = table.getRowCount();
        final int columnCount = table.getColumnCount();

        final ColumnConfig[] configs = new ColumnConfig[columnCount];
        final Map<String, Object[]> dataMap = new HashMap<>();

        for (int col = 0; col < columnCount; col++) {
            final ColumnConfig column = new ColumnConfig(table.getColumnName(col), table.getColumnClass(col));
            configs[col] = column;

            final Object[] rData = new Object[rowsCount];
            for (int row = 0; row < rowsCount; row++) {
                rData[row] = table.getValueAt(row, col);
            }
            dataMap.put(column.getName(), rData);
        }

        return new ChartDataProvider() {
            @Override
            public ColumnConfig[] getColumns() {
                return configs;
            }

            @Override
            public int getRowsCount() {
                return rowsCount;
            }

            @Override
            public Object[] getRows(ColumnConfig column) {
                return dataMap.get(column.getName());
            }
        };
    }

    static ChartDataProvider columns(JTable table) {
        final int rowsCount = table.getRowCount();
        final int columnCount = table.getColumnCount();

        final ColumnConfig[] configs = new ColumnConfig[columnCount];
        for (int col = 0; col < columnCount; col++) {
            configs[col] = new ColumnConfig(table.getColumnName(col), table.getColumnClass(col));
        }

        return new ChartDataProvider() {
            @Override
            public ColumnConfig[] getColumns() {
                return configs;
            }

            @Override
            public int getRowsCount() {
                return rowsCount;
            }

            @Override
            public Object[] getRows(ColumnConfig column) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }

    int getRowsCount();

    static Date createDate(Object value) {
        // SQL Date, Time, Timestamp are here
        if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof c.Second) {
            final c.Second v = (c.Second) value;
            return new Date(v.i * 1000L);
        } else if (value instanceof c.Minute) {
            final c.Minute v = (c.Minute) value;
            return new Date(v.i * 60 * 1000L);
        } else if (value instanceof c.Month) {
            final c.Month v = (c.Month) value;
            return new Date(v.i * 12 * 24 * 60 * 1000L);
        } else if (value instanceof c.Timespan) {
            final c.Timespan v = (c.Timespan) value;
            return new Date(v.j / 1_000_000L);
        }
        throw new IllegalArgumentException("Invalid value type: " + value.getClass());
    }

    static RegularTimePeriod createPeriod(Object value) {
        // SQL Date, Time, Timestamp are here
        if (value instanceof Date) {
            return new Millisecond((Date) value);
        } else if (value instanceof c.Second) {
            final c.Second v = (c.Second) value;
            return new Second(new Date(v.i * 1000L));
        } else if (value instanceof c.Minute) {
            final c.Minute v = (c.Minute) value;
            return new Minute(new Date(v.i * 60 * 1000L));
        } else if (value instanceof c.Month) {
            final c.Month v = (c.Month) value;
            return new Month(new Date(v.i * 12 * 24 * 60 * 1000L));
        } else if (value instanceof c.Timespan) {
            final c.Timespan v = (c.Timespan) value;
            return new Millisecond(new Date(v.j / 1_000_000L));
        }
        throw new IllegalArgumentException("Invalid value type: " + value.getClass());
    }

    ColumnConfig[] getColumns();

    Object[] getRows(ColumnConfig column);

    default double[] getDoubles(ColumnConfig column) {
        final Object[] row = getRows(column);
        final double[] res = new double[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = ((Number) row[i]).doubleValue();
        }
        return res;
    }

    default Number[] getNumbers(ColumnConfig column) {
        final Object[] row = getRows(column);
        final Number[] res = new Number[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = ((Number) row[i]);
        }
        return res;
    }

    default Date[] getDates(ColumnConfig column) {
        final Object[] row = getRows(column);
        final Date[] res = new Date[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = createDate(row[i]);
        }
        return res;
    }

    default RegularTimePeriod[] getPeriods(ColumnConfig column) {
        final Object[] row = getRows(column);
        final RegularTimePeriod[] res = new RegularTimePeriod[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = createPeriod(row[i]);
        }
        return res;
    }
}