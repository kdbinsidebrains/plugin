package org.kdb.inside.brains.view.chart;

import org.jdom.Element;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ChartConfig {
    ChartType getChartType();


    KdbType getDomainType();

    List<ColumnDefinition> getRequiredColumns();


    boolean isInvalid();


    Element store();

    String toHumanString();

    default boolean isApplicable(ChartDataProvider dataProvider) {
        // We create a copy to compare only name and style.
        final Set<ColumnDefinition> required = getRequiredColumns().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        final Set<ColumnDefinition> exist = Stream.of(dataProvider.getColumns()).filter(Objects::nonNull).collect(Collectors.toSet());
        return exist.containsAll(required);
    }
}
