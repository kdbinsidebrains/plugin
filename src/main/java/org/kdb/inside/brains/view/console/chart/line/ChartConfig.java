package org.kdb.inside.brains.view.console.chart.line;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChartConfig {
    private final AxisConfig domainValues;
    private final List<AxisConfig> rangeValues;
    private final boolean drawShapes;

    public ChartConfig(AxisConfig domainValues, List<AxisConfig> rangeColumns, boolean drawShapes) {
        this.domainValues = domainValues;
        this.rangeValues = rangeColumns;
        this.drawShapes = drawShapes;
    }

    public boolean isDrawShapes() {
        return drawShapes;
    }

    public AxisConfig getDomainValues() {
        return domainValues;
    }

    public List<AxisConfig> getRangeValues() {
        return rangeValues;
    }

    public boolean isEmpty() {
        return domainValues == null || rangeValues.isEmpty();
    }

    public Map<SeriesConfig, List<AxisConfig>> dataset() {
        final Map<SeriesConfig, List<AxisConfig>> res = new LinkedHashMap<>();
        for (AxisConfig v : rangeValues) {
            res.computeIfAbsent(v.getSeries(), l -> new ArrayList<>()).add(v);
        }
        return res;
    }
}
