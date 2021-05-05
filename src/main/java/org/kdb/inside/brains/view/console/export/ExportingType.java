package org.kdb.inside.brains.view.console.export;

import org.kdb.inside.brains.view.console.TableResultView;

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

    public boolean hasExportingData(TableResultView resultView) {
        return hasExportingData(resultView.getTable());
    }

    public boolean hasExportingData(JTable table) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        if (isExportAll(selectionModel)) {
            return table.getColumnCount() > 0 && table.getRowCount() > 0;
        }
        return table.getSelectedRowCount() > 0 && table.getSelectedColumnCount() > 0;
    }

    private boolean isExportAll(ListSelectionModel selectionModel) {
        if (this == ALL || this == ALL_WITH_HEADER) {
            return true;
        }
        return (this == SELECTION || this == SELECTION_WITH_HEADER) && selectionModel.isSelectionEmpty();
    }

    public IndexIterator rowsIterator(JTable table) {
        return newIterator(table.getSelectionModel(), table.getRowCount() - 1);
    }

    public IndexIterator columnsIterator(JTable table) {
        return newIterator(table.getColumnModel().getSelectionModel(), table.getColumnCount() - 1);
    }

    private IndexIterator newIterator(ListSelectionModel selectionModel, int totalMax) {
        return new IndexIterator() {
            private int i;
            private int maxIndex;
            private final boolean exportAll = isExportAll(selectionModel);

            @Override
            public int next() {
                if (!exportAll) {
                    while (i < maxIndex && !selectionModel.isSelectedIndex(i)) {
                        i++;
                    }
                }
                return i <= maxIndex ? i++ : -1;
            }

            @Override
            public int reset() {
                i = exportAll ? 0 : selectionModel.getMinSelectionIndex();
                maxIndex = exportAll ? totalMax : selectionModel.getMaxSelectionIndex();
                return next();
            }
        };
    }

    public interface IndexIterator {
        int next();

        int reset();
    }
}
