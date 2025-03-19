package org.kdb.inside.brains.view.chart.types;

import icons.KdbIcons;
import org.jdom.Element;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.types.line.LineChartConfig;
import org.kdb.inside.brains.view.chart.types.ohlc.OHLCChartConfig;

import javax.swing.*;
import java.util.function.Function;

public enum ChartType {
    LINE("line-chart", KdbIcons.Chart.Line, LineChartConfig::restore),
    OHLC("ohlc-chart", KdbIcons.Chart.Candlestick, OHLCChartConfig::restore);

    private final Icon icon;
    private final String tagName;
    private final Function<Element, ChartConfig> function;

    ChartType(String tagName, Icon icon, Function<Element, ChartConfig> function) {
        this.icon = icon;
        this.tagName = tagName;
        this.function = function;
    }

    public static ChartType byName(String name) {
        for (ChartType type : ChartType.values()) {
            if (name.equals(type.tagName)) {
                return type;
            }
        }
        return null;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getTagName() {
        return tagName;
    }

    public ChartConfig restore(Element element) {
        return function.apply(element);
    }
}
