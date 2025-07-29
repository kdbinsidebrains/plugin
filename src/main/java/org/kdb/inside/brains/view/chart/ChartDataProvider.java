package org.kdb.inside.brains.view.chart;

import kx.KxConnection;
import kx.c;
import org.jfree.data.time.*;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.console.table.QColumnInfo;
import org.kdb.inside.brains.view.console.table.QTableModel;
import org.kdb.inside.brains.view.console.table.TableResult;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

public interface ChartDataProvider {
    Locale DEFAULT_LOCALE = Locale.getDefault();

    LocalDate KDB_FIRST_DATE = LocalDate.of(2000, 1, 1);

    static ChartDataProvider of(TableResult table) {
        return of(table.tableModel());
    }

    static ChartDataProvider of(QTableModel model) {
        final int rowsCount = model.getRowCount();

        final Map<ChartColumn, Object[]> cache = new HashMap<>();

        final QColumnInfo[] modelCols = model.getColumnInfos();
        final List<ChartColumn> columns = Stream.of(modelCols).map(ChartColumn::new).toList();

        return new ChartDataProvider() {
            @Override
            public int getRowsCount() {
                return rowsCount;
            }

            @Override
            public List<ChartColumn> getColumns() {
                return columns;
            }

            @Override
            public Object[] getValues(ChartColumn column) {
                return cache.computeIfAbsent(column, c -> {
                    final int colIndex = columns.indexOf(c);
                    if (colIndex == -1) {
                        throw new IllegalArgumentException("Unknown column " + column);
                    }

                    final Object[] values = new Object[rowsCount];
                    for (int row = 0; row < values.length; row++) {
                        values[row] = model.getValueAt(row, colIndex);
                    }
                    return values;
                });
            }
        };
    }

    int getRowsCount();

    List<ChartColumn> getColumns();

    Object[] getValues(ChartColumn column);

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
        // SQL Date, Time, Timestamp is here
        if (value instanceof Date date) {
            return date;
        } else if (value instanceof c.Month month) {
            final long epochMilli = KDB_FIRST_DATE.plusMonths(month.i).atTime(LocalTime.MIDNIGHT).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            return new Date(epochMilli);
        } else if (value instanceof c.Second v) {
            return new Date(v.i * 1_000L);
        } else if (value instanceof c.Minute v) {
            return new Date(v.i * 60_000L);
        } else if (value instanceof c.Timespan v) {
            return new Date(v.j / 1_000_000L);
        }
        throw new IllegalArgumentException("Invalid value style: " + value.getClass());
    }

    static RegularTimePeriod createPeriod(Object value) {
        if (value instanceof java.sql.Date date) {
            return new Day(date, KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof java.sql.Time time) {
            return new Millisecond(time, KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof java.util.Date datetime) {
            return new Millisecond(datetime, KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof c.Month v) {
            final long epochMilli = KDB_FIRST_DATE.plusMonths(v.i).atTime(LocalTime.MIDNIGHT).toInstant(ZoneOffset.UTC).toEpochMilli();
            return new Month(new Date(epochMilli), KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof c.Second v) {
            return new Second(new Date(v.i * 1_000L), KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof c.Minute v) {
            return new Minute(new Date(v.i * 60_000L), KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        } else if (value instanceof c.Timespan v) {
            return new Millisecond(new Date(v.j / 1_000_000L), KxConnection.UTC_TIMEZONE, DEFAULT_LOCALE);
        }
        throw new IllegalArgumentException("Invalid value style: " + value.getClass());
    }

    default long getDistinctCount(ChartColumn column) {
        return Stream.of(getValues(column)).distinct().count();
    }

    default String[] getSymbols(ChartColumn column) {
        final Object[] row = getValues(column);
        final String[] res = new String[row.length];
        for (int i = 0; i < res.length; i++) {
            final Object o = row[i];
            if (o instanceof char[] ch) {
                res[i] = new String(ch);
            } else {
                res[i] = String.valueOf(o);
            }
        }
        return res;
    }

    default double[] getDoubles(ChartColumn column) {
        final Object[] row = getValues(column);
        final double[] res = new double[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = ((Number) row[i]).doubleValue();
        }
        return res;
    }

    default Date[] getDates(ChartColumn column) {
        final Object[] row = getValues(column);
        final Date[] res = new Date[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = createDate(row[i]);
        }
        return res;
    }


    default Number[] getNumbers(ChartColumn column) {
        final Object[] row = getValues(column);
        final Number[] res = new Number[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = ((Number) row[i]);
        }
        return res;
    }

    default RegularTimePeriod[] getPeriods(ChartColumn column) {
        final Object[] row = getValues(column);
        final RegularTimePeriod[] res = new RegularTimePeriod[row.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = createPeriod(row[i]);
        }
        return res;
    }
}