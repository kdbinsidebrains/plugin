package org.kdb.inside.brains.view.console.chart.line;

import icons.KdbIcons;
import org.jfree.chart.renderer.xy.*;

import javax.swing.*;
import java.util.List;

public enum SeriesType {
    LINE("Line", KdbIcons.Chart.TypeLine) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            return new XYLineAndShapeRenderer(true, config.isDrawShapes());
        }
    },

    SPLINE("Spline", KdbIcons.Chart.TypeSpline) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            final XYSplineRenderer renderer = new XYSplineRenderer();
            renderer.setDefaultShapesVisible(config.isDrawShapes());
            return renderer;
        }
    },

    STEPS("Steps", KdbIcons.Chart.TypeSteps) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            final XYStepRenderer xyStepRenderer = new XYStepRenderer();
            xyStepRenderer.setDefaultShapesVisible(config.isDrawShapes());
            return xyStepRenderer;
        }
    },

    BAR("Bar", KdbIcons.Chart.TypeBar) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            final XYBarRenderer renderer = new XYBarRenderer();
            renderer.setShadowVisible(false);
            renderer.setBarPainter(new StandardXYBarPainter());
            return renderer;
        }
    },

    AREA("Area", KdbIcons.Chart.TypeArea) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            return new XYAreaRenderer2();
        }
    },

    DIFF("Diff", KdbIcons.Chart.TypeDiff) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            final XYDifferenceRenderer renderer = new XYDifferenceRenderer();
            final int size = axes.size();
            if (size > 0) {
                renderer.setPositivePaint(axes.get(0).getColor());
            }
            if (size > 1) {
                renderer.setNegativePaint(axes.get(1).getColor());
            }
            renderer.setShapesVisible(config.isDrawShapes());
            return renderer;
        }
    },

    SCATTER("Scatter", KdbIcons.Chart.TypeScatter) {
        @Override
        public XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes) {
            final XYDotRenderer renderer = new XYDotRenderer();
            renderer.setDotWidth(5);
            renderer.setDotHeight(5);
            return renderer;
        }
    };

    private final Icon icon;
    private final String label;

    SeriesType(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

    public abstract XYItemRenderer createRenderer(ChartConfig config, List<AxisConfig> axes);
}
