package org.kdb.inside.brains.view.chart.types.ohlc;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.ColumnConfig;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OHLCChartConfig implements ChartConfig {
    private final ColumnConfig domain;
    private final ColumnConfig openColumn;
    private final ColumnConfig highColumn;
    private final ColumnConfig lowColumn;
    private final ColumnConfig closeColumn;
    private final ColumnConfig volumeColumn;

    public OHLCChartConfig(ColumnConfig domain, ColumnConfig openColumn, ColumnConfig highColumn, ColumnConfig lowColumn, ColumnConfig closeColumn, ColumnConfig volumeColumn) {
        this.domain = domain;
        this.openColumn = openColumn;
        this.highColumn = highColumn;
        this.lowColumn = lowColumn;
        this.closeColumn = closeColumn;
        this.volumeColumn = volumeColumn;
    }

    public static @NotNull OHLCChartConfig restore(Element element) {
        final ColumnConfig date = ColumnConfig.restore(element.getChild("domain"));
        final ColumnConfig open = ColumnConfig.restore(element.getChild("open"));
        final ColumnConfig high = ColumnConfig.restore(element.getChild("high"));
        final ColumnConfig low = ColumnConfig.restore(element.getChild("low"));
        final ColumnConfig close = ColumnConfig.restore(element.getChild("close"));
        final ColumnConfig volume = ColumnConfig.restore(element.getChild("volume"));
        return new OHLCChartConfig(date, open, high, low, close, volume);
    }

    @Override
    public ChartType getType() {
        return ChartType.OHLC;
    }

    public ColumnConfig getOpenColumn() {
        return openColumn;
    }

    public ColumnConfig getHighColumn() {
        return highColumn;
    }

    public ColumnConfig getLowColumn() {
        return lowColumn;
    }

    public ColumnConfig getCloseColumn() {
        return closeColumn;
    }

    public ColumnConfig getVolumeColumn() {
        return volumeColumn;
    }

    public ColumnConfig getDomain() {
        return domain;
    }

    @Override
    public boolean isInvalid() {
        return domain == null || openColumn == null || highColumn == null || lowColumn == null || closeColumn == null;
    }

    @Override
    public List<ColumnConfig> getColumns() {
        return Stream.of(domain, openColumn, highColumn, lowColumn, closeColumn, volumeColumn).filter(Objects::nonNull).collect(Collectors.toList());
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
    public String toHumanString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<h2>Candlestick chart</h2>");
        builder.append("<table>");
        builder.append("<tr><th align=\"left\">Domain:</th><td>").append(domain.getName()).append("</td>");
        builder.append("<tr><th align=\"left\">Open:</th><td>").append(openColumn.getName()).append("</td>");
        builder.append("<tr><th align=\"left\">High:</th><td>").append(highColumn.getName()).append("</td>");
        builder.append("<tr><th align=\"left\">Low:</th><td>").append(lowColumn.getName()).append("</td>");
        builder.append("<tr><th align=\"left\">Close:</th><td>").append(closeColumn.getName()).append("</td>");
        if (volumeColumn != null) {
            builder.append("<tr><th align=\"left\">Volume:</th><td>").append(volumeColumn.getName()).append("</td>");
        }
        builder.append("</table>");
        builder.append("</html>");
        return builder.toString();
    }

    @Override
    public KdbType getDomainType() {
        return domain.getType();
    }

    @Override
    public OHLCChartConfig copy() {
        return new OHLCChartConfig(ColumnConfig.copy(domain), ColumnConfig.copy(openColumn), ColumnConfig.copy(highColumn), ColumnConfig.copy(lowColumn), ColumnConfig.copy(closeColumn), ColumnConfig.copy(volumeColumn));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OHLCChartConfig that)) return false;
        return Objects.equals(domain, that.domain) && Objects.equals(openColumn, that.openColumn) && Objects.equals(highColumn, that.highColumn) && Objects.equals(lowColumn, that.lowColumn) && Objects.equals(closeColumn, that.closeColumn) && Objects.equals(volumeColumn, that.volumeColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, openColumn, highColumn, lowColumn, closeColumn, volumeColumn);
    }
}
