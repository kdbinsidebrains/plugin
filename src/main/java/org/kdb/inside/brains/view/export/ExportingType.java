package org.kdb.inside.brains.view.export;

import javax.swing.*;

public enum ExportingType {
    ALL(false),
    ALL_WITH_HEADER(true),

    SELECTION(false),
    SELECTION_WITH_HEADER(true),

    ROWS(false),
    ROWS_WITH_HEADER(true),

    COLUMNS(false),
    COLUMNS_WITH_HEADER(true);

    private final boolean withHeader;

    ExportingType(boolean withHeader) {
        this.withHeader = withHeader;
    }

    public boolean withHeader() {
        return withHeader;
    }

    public boolean hasExportingData(JTable table) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        if (isExportAll(selectionModel)) {
            return table.getColumnCount() > 0 && table.getRowCount() > 0;
        }
        return table.getSelectedRowCount() > 0 || table.getSelectedColumnCount() > 0;
    }

    public boolean hasExportingData(ExportDataProvider dataProvider) {
        return hasExportingData(dataProvider.getTable());
    }

    private boolean isExportAll(ListSelectionModel selectionModel) {
        if (this == ALL || this == ALL_WITH_HEADER) {
            return true;
        }
        return (this == SELECTION || this == SELECTION_WITH_HEADER) && selectionModel.isSelectionEmpty();
    }

    public IndexIterator rowsIterator(JTable table) {
        return newIterator(table, true);
    }

    public IndexIterator columnsIterator(JTable table) {
        return newIterator(table, false);
    }

    private boolean iterateAllIndexes(ListSelectionModel selectionModel, boolean rowsIterator) {
        if (isExportAll(selectionModel)) {
            return true;
        }
        if (rowsIterator) {
            return this == COLUMNS || this == COLUMNS_WITH_HEADER;
        } else {
            return this == ROWS || this == ROWS_WITH_HEADER;
        }
    }

    private IndexIterator newIterator(JTable table, boolean rowsIterator) {
        final ListSelectionModel selectionModel = rowsIterator ? table.getSelectionModel() : table.getColumnModel().getSelectionModel();
        if (iterateAllIndexes(selectionModel, rowsIterator)) {
            final int count = rowsIterator ? table.getRowCount() : table.getColumnCount();
            return new IndexIterator(count) {
                @Override
                public int next() {
                    return i < count ? i++ : -1;
                }
            };
        } else {
            final int[] indexes = selectionModel.getSelectedIndices();
            return new IndexIterator(indexes.length) {
                @Override
                public int next() {
                    try {
                        return i < count ? indexes[i] : -1;
                    } finally {
                        i++;
                    }
                }
            };
        }
    }

    public abstract static class IndexIterator {
        protected final int count;
        protected int i;

        public IndexIterator(int count) {
            this.count = count;
        }

        public abstract int next();

        public final int reset() {
            i = 0;
            return next();
        }

        public final int count() {
            return count;
        }
    }
}
