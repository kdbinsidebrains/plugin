package org.kdb.inside.brains.view.console.table;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class QColumnModel extends DefaultTableColumnModel {
    private final QTable table;

    private final List<TableColumn> allColumns = new ArrayList<>();

    QColumnModel(QTable table) {
        this.table = table;
    }

    public int getColumnCount(boolean includeHidden) {
        return includeHidden ? this.allColumns.size() : this.getColumnCount();
    }

    public List<TableColumn> getColumns(boolean includeHidden) {
        return includeHidden ? new ArrayList<>(this.allColumns) : Collections.list(this.getColumns());
    }

    public boolean isVisible(TableColumn column) {
        return tableColumns.contains(column);
    }

    public List<TableColumn> getInvisibleColumns() {
        final List<TableColumn> res = new ArrayList<>(allColumns);
        res.removeAll(tableColumns);
        return res;
    }

    public QColumnInfo getColumnInfo(TableColumn column) {
        return table.getModel().getColumnInfo(column.getModelIndex());
    }

    @Override
    public void removeColumn(TableColumn column) {
        this.allColumns.remove(column);
        super.removeColumn(column);
    }

    @Override
    public void addColumn(TableColumn aColumn) {
        this.allColumns.add(aColumn);
        super.addColumn(aColumn);
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        if (columnIndex != newIndex) {
            this.updateCurrentColumns(columnIndex, newIndex);
        }
        super.moveColumn(columnIndex, newIndex);
    }

    public void reset() {
        getInvisibleColumns().forEach(this::moveToVisible);
    }

    public void setVisible(TableColumn column, boolean visible) {
        if (!allColumns.contains(column)) {
            return;
        }

        if (visible) {
            if (!tableColumns.contains(column)) {
                moveToVisible(column);
            }
        } else {
            if (tableColumns.contains(column)) {
                moveToInvisible(column);
            }
        }
    }

    protected void moveToInvisible(TableColumn col) {
        super.removeColumn(col);
    }

    protected void moveToVisible(TableColumn col) {
        super.addColumn(col);
        int addIndex = this.allColumns.indexOf(col);
        for (int i = 0; i < this.getColumnCount() - 1; ++i) {
            TableColumn tableCol = this.getColumn(i);
            int actualPosition = this.allColumns.indexOf(tableCol);
            if (actualPosition > addIndex) {
                super.moveColumn(this.getColumnCount() - 1, i);
                break;
            }
        }
    }

    private void updateCurrentColumns(int oldIndex, int newIndex) {
        final TableColumn movedColumn = this.tableColumns.elementAt(oldIndex);
        final TableColumn targetColumn = this.tableColumns.elementAt(newIndex);
        final int oldPosition = this.allColumns.indexOf(movedColumn);
        final int newPosition = this.allColumns.indexOf(targetColumn);
        this.allColumns.remove(oldPosition);
        this.allColumns.add(newPosition, movedColumn);
    }
}