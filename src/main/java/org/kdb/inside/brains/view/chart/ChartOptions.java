package org.kdb.inside.brains.view.chart;

import com.intellij.util.xmlb.annotations.XCollection;
import org.kdb.inside.brains.settings.SettingsBean;
import org.kdb.inside.brains.view.chart.tools.ChartTool;
import org.kdb.inside.brains.view.chart.tools.impl.CrosshairTool;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChartOptions implements SettingsBean<ChartOptions> {
    private SnapType snapType = SnapType.NO;

    @XCollection(propertyElementName = "enabledTools", elementName = "tool", valueAttributeName = "id")
    private final Set<String> enabledTools = new HashSet<>();

    public ChartOptions() {
        enabledTools.add(CrosshairTool.ID);
    }

    public SnapType getSnapType() {
        return snapType;
    }

    public void setSnapType(SnapType snapType) {
        this.snapType = snapType;
    }

    public void setEnabled(ChartTool tool, boolean state) {
        if (state) {
            enabledTools.add(tool.getId());
        } else {
            enabledTools.remove(tool.getId());
        }
    }

    public boolean isEnabled(ChartTool tool) {
        return enabledTools.contains(tool.getId());
    }

    @Override
    public void copyFrom(ChartOptions chartOptions) {
        snapType = chartOptions.snapType;
        enabledTools.clear();
        enabledTools.addAll(chartOptions.enabledTools);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChartOptions that)) return false;
        return snapType == that.snapType && Objects.equals(enabledTools, that.enabledTools);
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapType, enabledTools);
    }

    @Override
    public String toString() {
        return "ChartOptions{" +
                "snapType=" + snapType +
                ", enabledTools=" + enabledTools +
                '}';
    }
}