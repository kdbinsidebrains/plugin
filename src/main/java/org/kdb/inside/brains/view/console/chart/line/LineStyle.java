package org.kdb.inside.brains.view.console.chart.line;

import icons.KdbIcons;

import javax.swing.*;

public enum LineStyle {
    LINE("Line", KdbIcons.Chart.StyleLine),
    SHAPE("Shape", KdbIcons.Chart.StyleShape),
    FULL("Both", KdbIcons.Chart.StyleFull);

    private final Icon icon;
    private final String label;

    LineStyle(String label, Icon icon) {
        this.label = label;
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }


    public boolean isLineVisible() {
        return this == LINE || this == FULL;
    }

    public boolean isShapeVisible() {
        return this == SHAPE || this == FULL;
    }
}