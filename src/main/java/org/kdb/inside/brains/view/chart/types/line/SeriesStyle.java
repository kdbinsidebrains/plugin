package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jfree.chart.renderer.xy.*;

import javax.swing.*;
import java.awt.*;

import static org.kdb.inside.brains.view.chart.ChartColors.transparent;

public enum SeriesStyle {
    LINE("Line", KdbIcons.Chart.TypeLine) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            return new XYLineAndShapeRenderer(true, config.isDrawShapes());
        }
    },

    SPLINE("Spline", KdbIcons.Chart.TypeSpline) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYSplineRenderer renderer = new XYSplineRenderer();
            renderer.setDefaultShapesVisible(config.isDrawShapes());
            return renderer;
        }
    },

    STEPS("Steps", KdbIcons.Chart.TypeSteps) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYStepRenderer xyStepRenderer = new XYStepRenderer();
            xyStepRenderer.setDefaultShapesVisible(config.isDrawShapes());
            return xyStepRenderer;
        }
    },

    BAR("Bar", KdbIcons.Chart.TypeBar) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYBarRenderer renderer = new XYBarRenderer() {
                @Override
                public void setSeriesStroke(int series, Stroke stroke, boolean notify) {
                    super.setSeriesStroke(series, stroke, notify);
                    super.setSeriesOutlineStroke(series, stroke, notify);
                }

                @Override
                public void setSeriesPaint(int series, Paint paint, boolean notify) {
                    final Color c = (Color) paint;
                    super.setSeriesPaint(series, transparent(c, 40), notify);
                    super.setSeriesOutlinePaint(series, c, notify);
                }
            };
            renderer.setDrawBarOutline(true);
            renderer.setShadowVisible(false);
            renderer.setBarPainter(new StandardXYBarPainter());
            return renderer;
        }
    },

    AREA("Area", KdbIcons.Chart.TypeArea) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYAreaRenderer2 r = new XYAreaRenderer2() {
                @Override
                public void setSeriesStroke(int series, Stroke stroke, boolean notify) {
                    super.setSeriesStroke(series, stroke, notify);
                    super.setSeriesOutlineStroke(series, stroke, notify);
                }

                @Override
                public void setSeriesPaint(int series, Paint paint, boolean notify) {
                    final Color c = (Color) paint;
                    super.setSeriesPaint(series, transparent(c, 40), notify);
                    super.setSeriesOutlinePaint(series, c, notify);
                }
            };
            r.setOutline(true);
            return r;
        }
    },

    DIFF("Diff", KdbIcons.Chart.TypeDiff) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYDifferenceRenderer renderer = new XYDifferenceRenderer();
            renderer.setPositivePaint(JBColor.GREEN);
            renderer.setNegativePaint(JBColor.RED);
            renderer.setShapesVisible(config.isDrawShapes());
            return renderer;
        }
    },

    SCATTER("Scatter", KdbIcons.Chart.TypeScatter) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            final XYDotRenderer renderer = new XYDotRenderer();
            renderer.setDotWidth(5);
            renderer.setDotHeight(5);
            return renderer;
        }
    };

    private final Icon icon;
    private final String label;

    SeriesStyle(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public abstract XYItemRenderer createRenderer(LineChartConfig config);
}
