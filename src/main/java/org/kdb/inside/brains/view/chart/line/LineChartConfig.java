package org.kdb.inside.brains.view.chart.line;

import org.kdb.inside.brains.view.chart.ColumnConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class LineChartConfig {
    private final ColumnConfig domain;
    private final List<RangeConfig> ranges;
    private final boolean drawShapes;

    public LineChartConfig(ColumnConfig domain, List<RangeConfig> rangeColumns, boolean drawShapes) {
        this.domain = domain;
        this.ranges = rangeColumns;
        this.drawShapes = drawShapes;
    }

    public boolean isDrawShapes() {
        return drawShapes;
    }

    public ColumnConfig getDomain() {
        return domain;
    }

    public List<RangeConfig> getRanges() {
        return ranges;
    }

    public boolean isEmpty() {
        return domain == null || ranges.isEmpty();
    }

    public Map<SeriesConfig, List<RangeConfig>> dataset() {
        final Map<SeriesConfig, List<RangeConfig>> res = new LinkedHashMap<>();
        for (RangeConfig v : ranges) {
            res.computeIfAbsent(v.getSeries(), l -> new ArrayList<>()).add(v);
        }
        return res;
    }
}
