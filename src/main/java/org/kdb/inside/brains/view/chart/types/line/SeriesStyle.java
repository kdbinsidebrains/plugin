package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.ui.JBColor;
import icons.KdbIcons;
import org.jfree.chart.renderer.xy.*;

import javax.swing.*;

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
            final XYBarRenderer renderer = new XYBarRenderer();
            renderer.setShadowVisible(false);
            renderer.setBarPainter(new StandardXYBarPainter());
            return renderer;
        }
    },

    AREA("Area", KdbIcons.Chart.TypeArea) {
        @Override
        public XYItemRenderer createRenderer(LineChartConfig config) {
            return new XYAreaRenderer2();
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
