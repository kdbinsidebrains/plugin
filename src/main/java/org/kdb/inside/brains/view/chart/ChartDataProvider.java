package org.kdb.inside.brains.view.chart;

import javax.swing.*;

public interface ChartDataProvider {
    static ChartDataProvider copy(JTable table) {
        final int rowsCount = table.getRowCount();
        final int columnCount = table.getColumnCount();

        final ColumnConfig[] configs = new ColumnConfig[columnCount];
        final Object[][] data = new Object[columnCount][rowsCount];
        for (int col = 0; col < columnCount; col++) {
            configs[col] = new ColumnConfig(col, table.getColumnName(col), table.getColumnClass(col));
            for (int row = 0; row < rowsCount; row++) {
                data[col][row] = table.getValueAt(row, col);
            }
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
            public Object getValueAt(int row, int col) {
                return data[col][row];
            }
        };
    }

    ColumnConfig[] getColumns();

    Object getValueAt(int row, int col);

    int getRowsCount();

/*
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
            public int getRowsCount() {
                return table.getRowCount();
            }

            @Override
            public Object getValueAt(int row, int col) {
                return table.getValueAt(row, col);
            }
        };
    }
*/
}
