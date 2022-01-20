package org.kdb.inside.brains.view.console.chart.line;

import org.jfree.chart.axis.ValueAxis;

public class SeriesConfig {
    private String name;
    private SeriesType type;
    private int lowerMargin = (int) (ValueAxis.DEFAULT_LOWER_MARGIN * 100);
    private int upperMargin = (int) (ValueAxis.DEFAULT_UPPER_MARGIN * 100);

    public SeriesConfig(String name, SeriesType type) {
        this.name = name;
        this.type = type;
    }


    public String getLabel() {
        return isEmpty() ? "" : name + " (" + type.getLabel() + ")";
    }

    public boolean isEmpty() {
        return name == null || name.trim().isEmpty();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SeriesType getType() {
        return type;
    }

    public void setType(SeriesType type) {
        this.type = type;
    }

    public int getLowerMargin() {
        return lowerMargin;
    }

    public void setLowerMargin(int lowerMargin) {
        this.lowerMargin = lowerMargin;
    }

    public int getUpperMargin() {
        return upperMargin;
    }

    public void setUpperMargin(int upperMargin) {
        this.upperMargin = upperMargin;
    }
}
