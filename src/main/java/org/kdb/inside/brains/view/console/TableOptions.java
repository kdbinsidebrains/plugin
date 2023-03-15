package org.kdb.inside.brains.view.console;

import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class TableOptions implements SettingsBean<TableOptions> {
    private boolean striped = true;
    private boolean showGrid = true;
    private boolean indexColumn = true;
    private boolean xmasKeyColumn = true;
    private boolean expandList = true;
    private boolean expandDict = true;
    private boolean expandTable = true;


    public boolean isStriped() {
        return striped;
    }

    public void setStriped(boolean striped) {
        this.striped = striped;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }


    public boolean isIndexColumn() {
        return indexColumn;
    }

    public void setIndexColumn(boolean indexColumn) {
        this.indexColumn = indexColumn;
    }

    public boolean isExpandList() {
        return expandList;
    }

    public void setExpandList(boolean expandList) {
        this.expandList = expandList;
    }

    public boolean isExpandDict() {
        return expandDict;
    }

    public void setExpandDict(boolean expandDict) {
        this.expandDict = expandDict;
    }

    public boolean isExpandTable() {
        return expandTable;
    }

    public void setExpandTable(boolean expandTable) {
        this.expandTable = expandTable;
    }

    public boolean isXmasKeyColumn() {
        return xmasKeyColumn;
    }

    public void setXmasKeyColumn(boolean xmasKeyColumn) {
        this.xmasKeyColumn = xmasKeyColumn;
    }

    @Override
    public void copyFrom(TableOptions options) {
        this.striped = options.striped;
        this.showGrid = options.showGrid;
        this.indexColumn = options.indexColumn;
        this.expandList = options.expandList;
        this.expandDict = options.expandDict;
        this.expandTable = options.expandTable;
        this.xmasKeyColumn = options.xmasKeyColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TableOptions)) return false;
        TableOptions that = (TableOptions) o;
        return striped == that.striped && showGrid == that.showGrid && indexColumn == that.indexColumn && xmasKeyColumn == that.xmasKeyColumn && expandList == that.expandList && expandDict == that.expandDict && expandTable == that.expandTable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(striped, showGrid, indexColumn, xmasKeyColumn, expandList, expandDict, expandTable);
    }

    @Override
    public String toString() {
        return "TableOptions{" +
                "striped=" + striped +
                ", showGrid=" + showGrid +
                ", indexColumn=" + indexColumn +
                ", xmasKeyColumn=" + xmasKeyColumn +
                ", expandList=" + expandList +
                ", expandDict=" + expandDict +
                ", expandTable=" + expandTable +
                '}';
    }
}