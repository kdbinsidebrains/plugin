package org.kdb.inside.brains.view.console;

import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public final class ConsoleOptions implements SettingsBean<ConsoleOptions> {
    private int floatPrecision = 7;
    private boolean enlistArrays = true;
    private boolean wrapStrings = true;
    private boolean prefixSymbols = true;
    private boolean striped = true;
    private boolean showGrid = true;
    private boolean indexColumn = false;
    private boolean dictAsTable = true;
    private boolean listAsTable = true;
    private boolean expandList = true;
    private boolean expandDict = true;
    private boolean expandTable = true;
    private boolean consoleBackground = true;
    private boolean xmasKeyColumn = true;
    private ConsoleSplitType splitType = ConsoleSplitType.NO;

    public static final int MAX_DECIMAL_PRECISION = 16;

    public ConsoleOptions() {
    }

    public boolean isDictAsTable() {
        return dictAsTable;
    }

    public void setDictAsTable(boolean dictAsTable) {
        this.dictAsTable = dictAsTable;
    }

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

    public int getFloatPrecision() {
        return floatPrecision;
    }

    public void setFloatPrecision(int floatPrecision) {
        this.floatPrecision = floatPrecision;
    }

    public boolean isEnlistArrays() {
        return enlistArrays;
    }

    public void setEnlistArrays(boolean enlistArrays) {
        this.enlistArrays = enlistArrays;
    }

    public boolean isWrapStrings() {
        return wrapStrings;
    }

    public void setWrapStrings(boolean wrapStrings) {
        this.wrapStrings = wrapStrings;
    }

    public boolean isPrefixSymbols() {
        return prefixSymbols;
    }

    public void setPrefixSymbols(boolean prefixSymbols) {
        this.prefixSymbols = prefixSymbols;
    }

    public boolean isListAsTable() {
        return listAsTable;
    }

    public void setListAsTable(boolean listAsTable) {
        this.listAsTable = listAsTable;
    }

    public ConsoleSplitType getSplitType() {
        return splitType;
    }

    public void setSplitType(ConsoleSplitType splitType) {
        this.splitType = splitType == null ? ConsoleSplitType.NO : splitType;
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

    public boolean isConsoleBackground() {
        return consoleBackground;
    }

    public void setConsoleBackground(boolean consoleBackground) {
        this.consoleBackground = consoleBackground;
    }

    public boolean isXmasKeyColumn() {
        return xmasKeyColumn;
    }

    public void setXmasKeyColumn(boolean xmasKeyColumn) {
        this.xmasKeyColumn = xmasKeyColumn;
    }

    @Override
    public void copyFrom(ConsoleOptions options) {
        this.enlistArrays = options.enlistArrays;
        this.floatPrecision = options.floatPrecision;
        this.wrapStrings = options.wrapStrings;
        this.prefixSymbols = options.prefixSymbols;
        this.striped = options.striped;
        this.showGrid = options.showGrid;
        this.listAsTable = options.listAsTable;
        this.dictAsTable = options.dictAsTable;
        this.splitType = options.splitType;
        this.indexColumn = options.indexColumn;
        this.expandList = options.expandList;
        this.expandDict = options.expandDict;
        this.expandTable = options.expandTable;
        this.xmasKeyColumn = options.xmasKeyColumn;
        this.consoleBackground = options.consoleBackground;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsoleOptions that = (ConsoleOptions) o;
        return floatPrecision == that.floatPrecision && enlistArrays == that.enlistArrays && wrapStrings == that.wrapStrings && prefixSymbols == that.prefixSymbols && striped == that.striped && showGrid == that.showGrid && indexColumn == that.indexColumn && dictAsTable == that.dictAsTable && listAsTable == that.listAsTable && expandList == that.expandList && expandDict == that.expandDict && expandTable == that.expandTable && splitType == that.splitType && consoleBackground == that.consoleBackground && xmasKeyColumn == that.xmasKeyColumn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floatPrecision, enlistArrays, wrapStrings, prefixSymbols, striped, showGrid, indexColumn, dictAsTable, listAsTable, expandList, expandDict, expandTable, splitType, consoleBackground, xmasKeyColumn);
    }

    @Override
    public String toString() {
        return "ConsoleOptions{" +
                "floatPrecision=" + floatPrecision +
                ", enlistArrays=" + enlistArrays +
                ", wrapStrings=" + wrapStrings +
                ", prefixSymbols=" + prefixSymbols +
                ", striped=" + striped +
                ", showGrid=" + showGrid +
                ", indexColumn=" + indexColumn +
                ", dictAsTable=" + dictAsTable +
                ", listAsTable=" + listAsTable +
                ", expandList=" + expandList +
                ", expandDict=" + expandDict +
                ", expandTable=" + expandTable +
                ", splitType=" + splitType +
                ", xmasKeyColumn=" + xmasKeyColumn +
                ", consoleBackground=" + consoleBackground +
                '}';
    }
}
