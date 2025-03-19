package org.kdb.inside.brains.view.chart.types.ohlc;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.ColumnDefinition;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record OHLCChartConfig(ColumnDefinition domain, ColumnDefinition openColumn, ColumnDefinition highColumn,
                              ColumnDefinition lowColumn, ColumnDefinition closeColumn,
                              ColumnDefinition volumeColumn) implements ChartConfig {

    @Override
    public ChartType getChartType() {
        return ChartType.OHLC;
    }

    @Override
    public boolean isInvalid() {
        return domain == null || openColumn == null || highColumn == null || lowColumn == null || closeColumn == null;
    }

    @Override
    public List<ColumnDefinition> getRequiredColumns() {
        return Stream.of(domain, openColumn, highColumn, lowColumn, closeColumn, volumeColumn).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public String toHumanString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<h2>Candlestick chart</h2>");
        builder.append("<table>");
        builder.append("<tr><th align=\"left\">Domain:</th><td>").append(domain.name()).append("</td>");
        builder.append("<tr><th align=\"left\">Open:</th><td>").append(openColumn.name()).append("</td>");
        builder.append("<tr><th align=\"left\">High:</th><td>").append(highColumn.name()).append("</td>");
        builder.append("<tr><th align=\"left\">Low:</th><td>").append(lowColumn.name()).append("</td>");
        builder.append("<tr><th align=\"left\">Close:</th><td>").append(closeColumn.name()).append("</td>");
        if (volumeColumn != null) {
            builder.append("<tr><th align=\"left\">Volume:</th><td>").append(volumeColumn.name()).append("</td>");
        }
        builder.append("</table>");
        builder.append("</html>");
        return builder.toString();
    }

    public static @NotNull OHLCChartConfig restore(Element element) {
        final ColumnDefinition date = ColumnDefinition.restore(element.getChild("domain"));
        final ColumnDefinition open = ColumnDefinition.restore(element.getChild("open"));
        final ColumnDefinition high = ColumnDefinition.restore(element.getChild("high"));
        final ColumnDefinition low = ColumnDefinition.restore(element.getChild("low"));
        final ColumnDefinition close = ColumnDefinition.restore(element.getChild("close"));
        final ColumnDefinition volume = ColumnDefinition.restore(element.getChild("volume"));
        return new OHLCChartConfig(date, open, high, low, close, volume);
    }

    @Override
    public Element store() {
        Element e = new Element(ChartType.OHLC.getTagName());
        e.addContent(domain.store().setName("domain"));
        e.addContent(openColumn.store().setName("open"));
        e.addContent(highColumn.store().setName("high"));
        e.addContent(lowColumn.store().setName("low"));
        e.addContent(closeColumn.store().setName("close"));
        if (volumeColumn != null) {
            e.addContent(volumeColumn.store().setName("volume"));
        }
        return e;
    }

    @Override
    public KdbType getDomainType() {
        return domain.type();
    }
}
