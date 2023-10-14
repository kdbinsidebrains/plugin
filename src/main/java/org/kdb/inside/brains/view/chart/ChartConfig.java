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
    Element store();

    boolean isInvalid();

    ChartType getType();

    ChartConfig copy();

    String toHumanString();

    KdbType getDomainType();

    List<ColumnConfig> getColumns();

    default boolean isApplicable(ChartDataProvider dataProvider) {
        final Set<ColumnKey> required = getColumns().stream().filter(Objects::nonNull).map(ColumnKey::new).collect(Collectors.toSet());
        final Set<ColumnKey> exist = Stream.of(dataProvider.getColumns()).filter(Objects::nonNull).map(ColumnKey::new).collect(Collectors.toSet());
        return exist.containsAll(required);
    }
}
