package org.kdb.inside.brains.view.chart.types.line;

import com.google.common.collect.Lists;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartColumn;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LineChartConfig(ChartColumn domain,
                              List<ValuesDefinition> values,
                              List<ChartColumn> expansions,
                              boolean drawShapes) implements ChartConfig {
    @Override
    public KdbType getDomainType() {
        return domain.type();
    }

    @Override
    public ChartType getChartType() {
        return ChartType.LINE;
    }

    @Override
    public boolean isInvalid() {
        return domain == null || values.isEmpty();
    }

    public boolean isDrawShapes() {
        return drawShapes;
    }

    public LinkedHashMap<SeriesDefinition, List<SingleRange>> dataset(ChartDataProvider provider) {
        final LinkedHashMap<SeriesDefinition, List<SingleRange>> res = new LinkedHashMap<>();
        if (expansions.isEmpty()) {
            return values.stream().map(SingleRange::new).collect(Collectors.groupingBy(SingleRange::getSeries, LinkedHashMap::new, Collectors.toList()));
        } else {
            final List<List<ValueExpansion>> expansionsLists = Lists.cartesianProduct(expansions.stream().map(e -> Stream.of(provider.getSymbols(e)).distinct().map(s -> new ValueExpansion(e, s)).toList()).toList());
            for (List<ValueExpansion> expansions : expansionsLists) {
                for (ValuesDefinition v : values) {
                    res.computeIfAbsent(v.series(), l -> new ArrayList<>()).add(new SingleRange(v, expansions));
                }
            }
        }
        return res;
    }

    @Override
    public List<ChartColumn> getRequiredColumns() {
        final List<ChartColumn> c = new ArrayList<>();
        c.add(domain);
        for (ValuesDefinition value : values) {
            c.add(value.column());
        }
        c.addAll(expansions);
        return c;
    }

    @Override
    public @NotNull Element store() {
        final Element e = new Element(ChartType.LINE.getTagName());
        e.setAttribute("drawShapes", String.valueOf(drawShapes));
        e.addContent(domain.store().setName("domain"));

        final Element seriesEl = new Element("series");
        e.addContent(seriesEl);

        final LinkedHashMap<SeriesDefinition, List<ValuesDefinition>> series = values.stream().collect(Collectors.groupingBy(ValuesDefinition::series, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<SeriesDefinition, List<ValuesDefinition>> entry : series.entrySet()) {
            final SeriesDefinition key = entry.getKey();
            final List<ValuesDefinition> value = entry.getValue();

            final Element store = key.store();
            for (ValuesDefinition valuesConfig : value) {
                store.addContent(valuesConfig.store());
            }
            seriesEl.addContent(store);
        }

        final Element expansionsEl = new Element("expansions");
        e.addContent(expansionsEl);
        for (ChartColumn expansion : expansions) {
            expansionsEl.addContent(expansion.store());
        }
        return e;
    }

    @NotNull
    public static LineChartConfig restore(@NotNull Element element) {
        final ChartColumn domain = ChartColumn.restore(element.getChild("domain"));

        final List<ValuesDefinition> ranges = element.getChild("series").getChildren().stream().flatMap(e -> {
            final SeriesDefinition sc = SeriesDefinition.restore(e);
            return e.getChildren().stream().map(el -> ValuesDefinition.restore(el, sc));
        }).collect(Collectors.toList());

        final List<ChartColumn> expansions = element.getChild("expansions").getChildren().stream().map(ChartColumn::restore).toList();

        final boolean drawShapes = Boolean.parseBoolean(element.getAttributeValue("drawShapes"));

        return new LineChartConfig(domain, ranges, expansions, drawShapes);
    }

    @Override
    public String toHumanString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        builder.append("<h2>Line chart</h2>");
        builder.append("<table>");
        builder.append("<tr><th align=\"left\">Domain:</th><td>").append(domain.name()).append("</td>");
        builder.append("<tr><th align=\"left\">Draw Shapes:</th><td>").append(drawShapes ? "Yes" : "No").append("</td>");

        builder.append("<tr><th align=\"left\"><u>Series</u></th><th align=\"left\"><u>Columns</u></th><td>");
        final LinkedHashMap<SeriesDefinition, List<ValuesDefinition>> dataset = values.stream().collect(Collectors.groupingBy(ValuesDefinition::series, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<SeriesDefinition, List<ValuesDefinition>> entry : dataset.entrySet()) {
            final SeriesDefinition key = entry.getKey();
            boolean first = true;
            for (ValuesDefinition range : entry.getValue()) {
                builder.append("<tr><th align=\"left\">");
                if (first) {
                    builder.append(key.name());
                    first = false;
                }
                builder.append("</th><td>").append(range.column().name()).append("</td>");
            }
        }

        if (expansions != null) {
            builder.append("<tr><th align=\"left\"><u>Expansions</u></th><th align=\"left\"><u>Columns</u></th><td>");
            for (ChartColumn expansion : expansions) {
                builder.append("<tr><th align=\"left\">").append("</th><td>").append(expansion.name()).append("</td>");
            }
        }
        builder.append("</table>");
        builder.append("</html>");
        return builder.toString();
    }
}
