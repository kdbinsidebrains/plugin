package org.kdb.inside.brains.view.chart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChartColorsTest {
    @Test
    public void defaultColors() {
        assertEquals(60, ChartColors.DEFAULT_COLORS.length);
    }
}