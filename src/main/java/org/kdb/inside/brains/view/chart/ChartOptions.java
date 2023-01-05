package org.kdb.inside.brains.view.chart;

import org.kdb.inside.brains.settings.SettingsBean;

import java.util.Objects;

public class ChartOptions implements SettingsBean<ChartOptions> {
    private SnapType snapType = SnapType.NO;
    private boolean valuesToolEnabled = false;
    private boolean measureToolEnabled = false;
    private boolean crosshairToolEnabled = true;

    public SnapType getSnapType() {
        return snapType;
    }

    public void setSnapType(SnapType snapType) {
        this.snapType = snapType;
    }

    public boolean isValuesToolEnabled() {
        return valuesToolEnabled;
    }

    public void setValuesToolEnabled(boolean valuesToolEnabled) {
        this.valuesToolEnabled = valuesToolEnabled;
    }

    public boolean isMeasureToolEnabled() {
        return measureToolEnabled;
    }

    public void setMeasureToolEnabled(boolean measureToolEnabled) {
        this.measureToolEnabled = measureToolEnabled;
    }

    public boolean isCrosshairToolEnabled() {
        return crosshairToolEnabled;
    }

    public void setCrosshairToolEnabled(boolean crosshairToolEnabled) {
        this.crosshairToolEnabled = crosshairToolEnabled;
    }

    @Override
    public void copyFrom(ChartOptions chartOptions) {
        snapType = chartOptions.snapType;
        valuesToolEnabled = chartOptions.valuesToolEnabled;
        measureToolEnabled = chartOptions.measureToolEnabled;
        crosshairToolEnabled = chartOptions.crosshairToolEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChartOptions)) return false;
        ChartOptions that = (ChartOptions) o;
        return valuesToolEnabled == that.valuesToolEnabled && measureToolEnabled == that.measureToolEnabled && crosshairToolEnabled == that.crosshairToolEnabled && snapType == that.snapType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapType, valuesToolEnabled, measureToolEnabled, crosshairToolEnabled);
    }

    @Override
    public String toString() {
        return "ChartOptions{" +
                "snapType=" + snapType +
                ", valuesToolEnabled=" + valuesToolEnabled +
                ", measureToolEnabled=" + measureToolEnabled +
                ", crosshairToolEnabled=" + crosshairToolEnabled +
                '}';
    }
}