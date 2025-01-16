package org.kdb.inside.brains.view.chart;

import org.jfree.chart.renderer.xy.XYItemRenderer;

import java.awt.*;

public record RendererConfig(Color color, BasicStroke stroke) {
    public RendererConfig(XYItemRenderer renderer, int series) {
        this((Color) renderer.getSeriesPaint(series), (BasicStroke) renderer.getSeriesStroke(series));
    }

    public void update(XYItemRenderer renderer, int series) {
        renderer.setSeriesPaint(series, color);
        renderer.setSeriesStroke(series, stroke);
    }
}