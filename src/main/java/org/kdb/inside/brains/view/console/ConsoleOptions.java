package org.kdb.inside.brains.view.console;

import com.intellij.util.xmlb.annotations.Property;
import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public final class ConsoleOptions implements SettingsBean<ConsoleOptions> {
    private boolean wrapStrings = true;
    private boolean prefixSymbols = true;
    private boolean enlistArrays = true;
    private boolean dictAsTable = true;
    private boolean listAsTable = true;
    private boolean consoleBackground = true;
    private boolean clearTableResult = true;
    private ConsoleSplitType splitType = ConsoleSplitType.NO;

    @Property
    @Deprecated(forRemoval = true)
    private int floatPrecision = -1;

    public int getLegacyFloatPrecision() {
        int asd = floatPrecision;
        floatPrecision = -1;
        return asd;
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

    public boolean isDictAsTable() {
        return dictAsTable;
    }

    public void setDictAsTable(boolean dictAsTable) {
        this.dictAsTable = dictAsTable;
    }

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
        this.listAsTable = options.listAsTable;
        this.dictAsTable = options.dictAsTable;
        this.splitType = options.splitType;
        this.consoleBackground = options.consoleBackground;
        this.clearTableResult = options.clearTableResult;
    }

    @Override
    public String toString() {
        return "ConsoleOptions{" +
                "wrapStrings=" + wrapStrings +
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
