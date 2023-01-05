package org.kdb.inside.brains.view.chart.types.line;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.ColumnConfig;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.*;
import java.util.stream.Collectors;

public class LineChartConfig implements ChartConfig {
    private final ColumnConfig domain;
    private final List<RangeConfig> ranges;
    private final boolean drawShapes;

    public LineChartConfig(ColumnConfig domain, List<RangeConfig> rangeColumns, boolean drawShapes) {
        this.domain = domain;
        this.ranges = rangeColumns;
        this.drawShapes = drawShapes;
    }

    @NotNull
    public static LineChartConfig restore(@NotNull Element element) {
        final boolean drawShapes = Boolean.parseBoolean(element.getAttributeValue("drawShapes"));
        final ColumnConfig domain = ColumnConfig.restore(element.getChild("domain"));
        final List<RangeConfig> ranges = element.getChildren("series").stream().flatMap(e -> {
            final SeriesConfig sc = SeriesConfig.restore(e);
            return e.getChildren().stream().map(RangeConfig::restore).peek(r -> r.setSeries(sc));
        }).collect(Collectors.toList());
        return new LineChartConfig(domain, ranges, drawShapes);
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

    @Override
    public ChartType getType() {
        return ChartType.LINE;
    }

    @Override
    public boolean isInvalid() {
        return domain == null || ranges.isEmpty();
    }

    public Map<SeriesConfig, List<RangeConfig>> dataset() {
        final Map<SeriesConfig, List<RangeConfig>> res = new LinkedHashMap<>();
        for (RangeConfig v : ranges) {
            res.computeIfAbsent(v.getSeries(), l -> new ArrayList<>()).add(v);
        }
        return res;
    }

    @Override
    public List<ColumnConfig> getColumns() {
        final List<ColumnConfig> c = new ArrayList<>(ranges);
        c.add(domain);
        return c;
    }

    @Override
    public @NotNull Element store() {
        final Element e = new Element(ChartType.LINE.getTagName());
        e.setAttribute("drawShapes", String.valueOf(drawShapes));
        e.addContent(domain.store().setName("domain"));

        final Map<SeriesConfig, List<RangeConfig>> dataset = dataset();
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : dataset.entrySet()) {
            final SeriesConfig key = entry.getKey();
            final List<RangeConfig> value = entry.getValue();

            final Element store = key.store();
            e.addContent(store);
            for (RangeConfig rangeConfig : value) {
                store.addContent(rangeConfig.store());
            }
        }
        return e;
    }

    @Override
    public String toHumanString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<h2>Line chart</h2>");
        builder.append("<table>");
        builder.append("<tr><th align=\"left\">Domain:</th><td>").append(domain.getName()).append("</td>");
        builder.append("<tr><th align=\"left\">Draw Shapes:</th><td>").append(drawShapes ? "Yes" : "No").append("</td>");
        builder.append("<tr><th align=\"left\"><u>Series</u></th><th align=\"left\"><u>Columns</u></th><td>");
        final Map<SeriesConfig, List<RangeConfig>> dataset = dataset();
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : dataset.entrySet()) {
            final SeriesConfig key = entry.getKey();
            boolean first = true;
            for (RangeConfig range : entry.getValue()) {
                builder.append("<tr><th align=\"left\">");
                if (first) {
                    builder.append(key.getName()).append("");
                    first = false;
                }
                builder.append("</th><td>").append(range.getName()).append("</td>");
            }
        }

        builder.append("</table>");
        builder.append("</html>");
        return builder.toString();
    }

    @Override
    public LineChartConfig copy() {
        final List<RangeConfig> list = new ArrayList<>();

        final Map<SeriesConfig, List<RangeConfig>> dataset = dataset();
        for (Map.Entry<SeriesConfig, List<RangeConfig>> entry : dataset.entrySet()) {
            final SeriesConfig series = entry.getKey().copy();
            for (RangeConfig config : entry.getValue()) {
                list.add(RangeConfig.copy(config, series));
            }
        }
        return new LineChartConfig(ColumnConfig.copy(domain), list, drawShapes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineChartConfig)) return false;
        LineChartConfig that = (LineChartConfig) o;
        final boolean equals = Objects.equals(ranges, that.ranges);
        return drawShapes == that.drawShapes && Objects.equals(domain, that.domain) && equals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(domain, ranges, drawShapes);
    }
}
