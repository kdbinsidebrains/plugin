package org.kdb.inside.brains.view.console;

import org.kdb.inside.brains.settings.SettingsBean;
import org.kdb.inside.brains.view.FormatterOptions;

import java.util.Objects;

public final class ConsoleOptions implements SettingsBean<ConsoleOptions>, FormatterOptions {
    private int floatPrecision = 7;
    private boolean wrapStrings = true;
    private boolean prefixSymbols = true;
    private boolean enlistArrays = true;
    private boolean dictAsTable = true;
    private boolean listAsTable = true;
    private boolean consoleBackground = true;
    private boolean clearTableResult = true;
    private ConsoleSplitType splitType = ConsoleSplitType.NO;

    public static final int MAX_DECIMAL_PRECISION = 16;

    public ConsoleOptions() {
    }

    @Override
    public boolean isWrapStrings() {
        return wrapStrings;
    }

    public void setWrapStrings(boolean wrapStrings) {
        this.wrapStrings = wrapStrings;
    }

    @Override
    public boolean isPrefixSymbols() {
        return prefixSymbols;
    }

    public void setPrefixSymbols(boolean prefixSymbols) {
        this.prefixSymbols = prefixSymbols;
    }

    public boolean isDictAsTable() {
        return dictAsTable;
    }

    public void setDictAsTable(boolean dictAsTable) {
        this.dictAsTable = dictAsTable;
    }

    @Override
    public int getFloatPrecision() {
        return floatPrecision;
    }

    public void setFloatPrecision(int floatPrecision) {
        this.floatPrecision = floatPrecision;
    }

    @Override
    public boolean isEnlistArrays() {
        return enlistArrays;
    }

    public void setEnlistArrays(boolean enlistArrays) {
        this.enlistArrays = enlistArrays;
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

    public boolean isConsoleBackground() {
        return consoleBackground;
    }

    public void setConsoleBackground(boolean consoleBackground) {
        this.consoleBackground = consoleBackground;
    }

    public boolean isClearTableResult() {
        return clearTableResult;
    }

    public void setClearTableResult(boolean clearTableResult) {
        this.clearTableResult = clearTableResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsoleOptions that)) return false;
        return floatPrecision == that.floatPrecision && wrapStrings == that.wrapStrings && prefixSymbols == that.prefixSymbols && enlistArrays == that.enlistArrays && dictAsTable == that.dictAsTable && listAsTable == that.listAsTable && consoleBackground == that.consoleBackground && clearTableResult == that.clearTableResult && splitType == that.splitType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floatPrecision, wrapStrings, prefixSymbols, enlistArrays, dictAsTable, listAsTable, consoleBackground, clearTableResult, splitType);
    }

    @Override
    public void copyFrom(ConsoleOptions options) {
        this.wrapStrings = options.wrapStrings;
        this.prefixSymbols = options.prefixSymbols;
        this.enlistArrays = options.enlistArrays;
        this.floatPrecision = options.floatPrecision;
        this.listAsTable = options.listAsTable;
        this.dictAsTable = options.dictAsTable;
        this.splitType = options.splitType;
        this.consoleBackground = options.consoleBackground;
        this.clearTableResult = options.clearTableResult;
    }

    @Override
    public String toString() {
        return "ConsoleOptions{" +
                "floatPrecision=" + floatPrecision +
                ", wrapStrings=" + wrapStrings +
                ", prefixSymbols=" + prefixSymbols +
                ", enlistArrays=" + enlistArrays +
                ", dictAsTable=" + dictAsTable +
                ", listAsTable=" + listAsTable +
                ", consoleBackground=" + consoleBackground +
                ", clearTableResult=" + clearTableResult +
                ", splitType=" + splitType +
                '}';
    }
}
