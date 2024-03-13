package org.kdb.inside.brains.view.console;

import org.kdb.inside.brains.settings.SettingsBean;

import java.math.RoundingMode;
import java.util.Objects;

public class NumericalOptions implements SettingsBean<NumericalOptions> {
    public static final int MAX_DECIMAL_PRECISION = 16;
    private int floatPrecision = 7;
    private boolean scientificNotation = false;
    private RoundingMode roundingMode = RoundingMode.HALF_EVEN;

    public int getFloatPrecision() {
        return floatPrecision;
    }

    public void setFloatPrecision(int floatPrecision) {
        if (floatPrecision < 0) {
            throw new IllegalArgumentException("Precision can't be < 0");
        }
        if (floatPrecision > MAX_DECIMAL_PRECISION) {
            throw new IllegalArgumentException("Precision can't be > MAX_DECIMAL_PRECISION (" + MAX_DECIMAL_PRECISION + ")");
        }
        this.floatPrecision = floatPrecision;
    }

    public boolean isScientificNotation() {
        return scientificNotation;
    }

    public void setScientificNotation(boolean scientificNotation) {
        this.scientificNotation = scientificNotation;
    }

    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    public void setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    @Override
    public void copyFrom(NumericalOptions o) {
        floatPrecision = o.floatPrecision;
        scientificNotation = o.scientificNotation;
        roundingMode = o.roundingMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NumericalOptions that)) return false;
        return floatPrecision == that.floatPrecision && scientificNotation == that.scientificNotation && roundingMode == that.roundingMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floatPrecision, scientificNotation, roundingMode);
    }

    @Override
    public String toString() {
        return "NumericalOptions{" +
                "floatPrecision=" + floatPrecision +
                ", scientificNotation=" + scientificNotation +
                ", roundingMode=" + roundingMode +
                '}';
    }
}