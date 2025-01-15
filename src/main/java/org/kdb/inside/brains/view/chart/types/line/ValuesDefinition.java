package org.kdb.inside.brains.view.chart.types.line;

import org.jdom.Element;
import org.kdb.inside.brains.view.chart.ColumnDefinition;

public record ValuesDefinition(ColumnDefinition column, SeriesDefinition series, Operation operation) {
    static ValuesDefinition restore(Element element, SeriesDefinition series) {
        if (element == null) {
            return null;
        }
        final ColumnDefinition column = ColumnDefinition.restore(element);
        final Operation operation = Operation.valueOf(element.getAttributeValue("operation", Operation.SUM.name()));
        return new ValuesDefinition(column, series, operation);
    }

    Element store() {
        final Element e = column.store();
        e.setAttribute("operation", operation.name());
        return e;
    }
}