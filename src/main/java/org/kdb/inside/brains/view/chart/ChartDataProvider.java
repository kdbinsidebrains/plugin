package org.kdb.inside.brains.view.chart;

import kx.KxConnection;
import kx.c;
import org.jfree.data.time.Month;
import org.jfree.data.time.*;
import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public interface ChartDataProvider {
    LocalDate KDB_FIRST_DATE = LocalDate.of(2000, 1, 1);

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

    static Object createKdbTemporal(long millis, KdbType type) {
        if (type == null || type == KdbType.TIMESTAMP) {
            return new Timestamp(millis);
        }
        return switch (type) {
            case DATE -> new java.sql.Date(millis);
            case TIME -> new java.sql.Time(millis);
            case DATETIME -> new java.util.Date(millis);
            case MONTH ->
                    new c.Month((int) ChronoUnit.MONTHS.between(KDB_FIRST_DATE, Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)));

            case SECOND -> new c.Second((int) (millis / 1_000L));
            case MINUTE -> new c.Minute((int) (millis / 60_000L));
            case TIMESPAN -> new c.Timespan(millis * 1_000_000L);
            default -> new Timestamp(millis);
        };
    }

    static Date createDate(Object value) {
        // SQL Date, Time, Timestamp are here
        Date res;
        if (value instanceof Date date) {
            res = date;
        } else if (value instanceof c.Month month) {
            final long epochMilli = KDB_FIRST_DATE.plusMonths(month.i).atTime(LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            res = new Date(epochMilli);
        } else if (value instanceof c.Second v) {
            res = new Date(v.i * 1_000L);
        } else if (value instanceof c.Minute v) {
            res = new Date(v.i * 60_000L);
        } else if (value instanceof c.Timespan v) {
            res = new Date(v.j / 1_000_000L);
        } else {
            throw new IllegalArgumentException("Invalid value type: " + value.getClass());
        }
        final ZonedDateTime f = Instant.ofEpochMilli(res.getTime()).atZone(ZoneId.systemDefault()).withZoneSameLocal(ZoneId.systemDefault());
        final ZonedDateTime t = f.withZoneSameInstant(ZoneOffset.UTC).withZoneSameLocal(ZoneId.systemDefault());
        return Date.from(t.toInstant());
    }

    static RegularTimePeriod createPeriod(Object value) {
        if (value instanceof java.sql.Date date) {
            return new Day(date, KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof java.sql.Time time) {
            return new Millisecond(time, KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof java.util.Date datetime) {
            return new Millisecond(datetime, KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof c.Month v) {
            final long epochMilli = KDB_FIRST_DATE.plusMonths(v.i).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC).toEpochMilli();
            return new Month(new Date(epochMilli), KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof c.Second v) {
            return new Second(new Date(v.i * 1_000L), KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof c.Minute v) {
            return new Minute(new Date(v.i * 60_000L), KxConnection.UTC_TIMEZONE, Locale.getDefault());
        } else if (value instanceof c.Timespan v) {
            return new Millisecond(new Date(v.j / 1_000_000L), KxConnection.UTC_TIMEZONE, Locale.getDefault());
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