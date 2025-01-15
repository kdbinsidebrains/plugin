package org.kdb.inside.brains.view.chart.types.line;

import org.junit.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SingleRangeTest {
    @Test
    public void label() {
        final ValuesDefinition values = new ValuesDefinition(new ColumnDefinition("c", KdbType.SYMBOL), new SeriesDefinition("s", SeriesStyle.AREA), Operation.MAX);

        assertEquals("c", new SingleRange(values).label());
        assertEquals("exp-c", new SingleRange(values, new ValueExpansion(new ColumnDefinition("ewrqwer", KdbType.SYMBOL), "exp")).label());
    }
}