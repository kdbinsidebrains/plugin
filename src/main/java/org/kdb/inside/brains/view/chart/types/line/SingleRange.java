package org.kdb.inside.brains.view.chart.types.line;

import java.util.List;
import java.util.stream.Collectors;

public record SingleRange(ValuesDefinition values, List<ValueExpansion> expansion, String label) {
    public SingleRange(ValuesDefinition values) {
        this(values, List.of(), values.column().name());
    }

    public SingleRange(ValuesDefinition values, ValueExpansion expansion) {
        this(values, List.of(expansion));
    }

    public SingleRange(ValuesDefinition values, List<ValueExpansion> expansion) {
        this(values, expansion, expansion.stream().map(ValueExpansion::value).collect(Collectors.joining("-")) + "-" + values.column().name());
    }

    public SeriesDefinition getSeries() {
        return values.series();
    }
}