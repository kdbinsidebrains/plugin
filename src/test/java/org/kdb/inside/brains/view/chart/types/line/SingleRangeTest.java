package org.kdb.inside.brains.view.chart.types.line;

import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ChartColumn;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SingleRangeTest {
    @Test
    public void label() {
        final ValuesDefinition values = new ValuesDefinition(new ChartColumn("c", KdbType.SYMBOL), new SeriesDefinition("s", SeriesStyle.AREA), Operation.MAX);

        assertEquals("c", new SingleRange(values).label());
        assertEquals("exp-c", new SingleRange(values, new ValueExpansion(new ChartColumn("ewrqwer", KdbType.SYMBOL), "exp")).label());
    }
}