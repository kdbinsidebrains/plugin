package org.kdb.inside.brains.view.console.chart.line;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChartConfig {
    private final AxisConfig domainValues;
    private final List<AxisConfig> rangeValues;

    public ChartConfig(AxisConfig domainValues, List<AxisConfig> rangeColumns) {
        this.domainValues = domainValues;
        this.rangeValues = rangeColumns;
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

    public Map<String, List<AxisConfig>> dataset() {
        return rangeValues.stream().collect(Collectors.groupingBy(AxisConfig::getGroup));
    }
}
