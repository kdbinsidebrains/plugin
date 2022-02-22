package org.kdb.inside.brains.view.chart;

import org.kdb.inside.brains.KdbType;

import javax.swing.*;

public interface ChartDataProvider {
    int getColumnCount();

    String getColumnName(int col);

    Class<?> getColumnClass(int col);


    int getRowCount();

    Object getValueAt(int row, int col);


    default KdbType getColumnType(int col) {
        return KdbType.typeOf(getColumnClass(col));
    }

    static ChartDataProvider tableCopy(JTable table) {
        final int rowsCount = table.getRowCount();
        final int columnCount = table.getColumnCount();

        final String[] columnNames = new String[columnCount];
        final Class<?>[] columnTypes = new Class<?>[columnCount];

        final Object[][] data = new Object[columnCount][rowsCount];
        for (int col = 0; col < columnCount; col++) {
            columnNames[col] = table.getColumnName(col);
            columnTypes[col] = table.getColumnClass(col);

            for (int row = 0; row < rowsCount; row++) {
                data[col][row] = table.getValueAt(row, col);
            }
        }

        return new ChartDataProvider() {
            @Override
            public int getColumnCount() {
                return columnCount;
            }

            @Override
            public String getColumnName(int col) {
                return columnNames[col];
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return columnTypes[col];
            }

            @Override
            public int getRowCount() {
                return rowsCount;
            }

            @Override
            public Object getValueAt(int row, int col) {
                return data[col][row];
            }
        };
    }

    static ChartDataProvider tableWrap(JTable table) {
        return new ChartDataProvider() {
            @Override
            public int getColumnCount() {
                return table.getColumnCount();
            }

            @Override
            public String getColumnName(int col) {
                return table.getColumnName(col);
            }

            @Override
            public Class<?> getColumnClass(int col) {
                return table.getColumnClass(col);
            }

            @Override
            public int getRowCount() {
                return table.getRowCount();
            }

            @Override
            public Object getValueAt(int row, int col) {
                return table.getValueAt(row, col);
            }
        };
    }
}
