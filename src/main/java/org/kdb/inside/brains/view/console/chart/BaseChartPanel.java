package org.kdb.inside.brains.view.console.chart;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import java.awt.event.InputEvent;
import java.lang.reflect.Field;

public class BaseChartPanel extends ChartPanel {
    public BaseChartPanel(JFreeChart chart) {
        super(chart, false, true, true, true, true);
        fixPanMask();

        setMouseZoomable(true);
        setMouseWheelEnabled(true);
    }

    @SuppressWarnings("deprecation")
    private void fixPanMask() {
        try {
            final Field panMask = ChartPanel.class.getDeclaredField("panMask");
            panMask.setAccessible(true);
            panMask.set(this, InputEvent.BUTTON1_MASK);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
