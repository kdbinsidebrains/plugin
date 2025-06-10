package org.kdb.inside.brains.view.chart;

import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import org.kdb.inside.brains.settings.SettingsBean;
import org.kdb.inside.brains.view.chart.tools.ChartTool;
import org.kdb.inside.brains.view.chart.tools.ModeChartTool;
import org.kdb.inside.brains.view.chart.tools.ToolMode;
import org.kdb.inside.brains.view.chart.tools.impl.CrosshairTool;

import java.util.*;

public class ChartOptions implements SettingsBean<ChartOptions> {
    private SnapType snapType = SnapType.NO;

    @XCollection(propertyElementName = "enabledTools")
    public List<ToolState> enabledTools = new ArrayList<>();

    @Transient
    private final Map<String, ToolState> toolStates = new HashMap<>();

    public ChartOptions() {
        setEnabled(CrosshairTool.ID, true);
    }

    public SnapType getSnapType() {
        return snapType;
    }

    public void setSnapType(SnapType snapType) {
        this.snapType = snapType;
    }

    public boolean isEnabled(ChartTool tool) {
        return toolStates.containsKey(tool.getId());
    }

    public void setEnabled(ChartTool tool, boolean state) {
        this.setEnabled(tool.getId(), state);
    }

    public <M extends ToolMode> M getMode(ModeChartTool<M> tool) {
        final ToolState ts = toolStates.get(tool.getId());
        return ts == null ? null : tool.findMode(ts.mode);
    }

    public <M extends ToolMode> void setModel(ModeChartTool<M> tool, M mode) {
        final ToolState ts = setEnabled(tool.getId(), mode != null);
        if (ts != null && mode != null) {
            ts.mode = mode.name();
        }
    }

    public <M extends ToolMode> boolean isMode(ModeChartTool<M> tool, M mode) {
        if (mode == null) {
            return !isEnabled(tool);
        }
        final ToolState toolState = toolStates.get(tool.getId());
        return toolState != null && mode.name().equals(toolState.mode);
    }

    private ToolState setEnabled(String id, boolean state) {
        if (state) {
            ToolState ts = toolStates.get(id);
            if (ts == null) {
                ts = new ToolState(id);
                enabledTools.add(ts);
                toolStates.put(id, ts);
            }
            return ts;
        } else {
            final ToolState ts = toolStates.remove(id);
            if (ts != null) {
                enabledTools.remove(ts);
            }
            return ts;
        }
    }

    @Override
    public void copyFrom(ChartOptions chartOptions) {
        snapType = chartOptions.snapType;

        toolStates.clear();
        enabledTools.clear();
        for (ToolState tool : chartOptions.enabledTools) {
            final ToolState copy = tool.copy();
            toolStates.put(tool.id, copy);
            enabledTools.add(copy);
        }
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
        return "ChartOptions{" + "snapType=" + snapType + ", enabledTools=" + enabledTools + '}';
    }

    @Tag("tool")
    public static class ToolState {
        @Attribute("id")
        public String id;

        @Attribute("mode")
        public String mode; // Optional extra attribute

        public ToolState() {
        } // Required for serialization

        public ToolState(String id) {
            this(id, null);
        }

        public ToolState(String id, String mode) {
            this.id = id;
            this.mode = mode;
        }

        private ToolState copy() {
            return new ToolState(id, mode);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ToolState toolState)) return false;
            return Objects.equals(id, toolState.id) && Objects.equals(mode, toolState.mode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, mode);
        }

        @Override
        public String toString() {
            return "ToolState{" + "id='" + id + '\'' + ", mode='" + mode + '\'' + '}';
        }
    }
}