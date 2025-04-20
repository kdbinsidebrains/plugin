package org.kdb.inside.brains.view.chart;

import org.jdom.Element;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.HashSet;
import java.util.List;

public interface ChartConfig {
    ChartType getChartType();


    KdbType getDomainType();

    List<ChartColumn> getRequiredColumns();


    boolean isInvalid();


    Element store();

    String toHumanString();


    default boolean isApplicable(ChartDataProvider dataProvider) {
        return new HashSet<>(dataProvider.getColumns()).containsAll(getRequiredColumns());
    }
}
