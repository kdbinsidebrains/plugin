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

    public static final int MAX_DECIMAL_PRECISION = 15;

    public ConsoleOptions() {
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

    @Override
    public void copyFrom(ConsoleOptions options) {
        this.enlistArrays = options.enlistArrays;
        this.floatPrecision = options.floatPrecision;
        this.wrapStrings = options.wrapStrings;
        this.prefixSymbols = options.prefixSymbols;
        this.striped = options.striped;
        this.showGrid = options.showGrid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsoleOptions that = (ConsoleOptions) o;
        return floatPrecision == that.floatPrecision && enlistArrays == that.enlistArrays && wrapStrings == that.wrapStrings && prefixSymbols == that.prefixSymbols && striped == that.striped && showGrid == that.showGrid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floatPrecision, enlistArrays, wrapStrings, prefixSymbols, striped, showGrid);
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
                '}';
    }
}
