package org.kdb.inside.brains.view.chart.types.line;

import org.jdom.Element;
import org.jfree.chart.axis.ValueAxis;

import java.util.Objects;

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

    public static SeriesConfig restore(Element element) {
        final String name = element.getAttributeValue("name");
        final SeriesType type = SeriesType.valueOf(element.getAttributeValue("type"));

        final SeriesConfig seriesConfig = new SeriesConfig(name, type);
        final String lowerMargin = element.getAttributeValue("lowerMargin");
        if (lowerMargin != null) {
            seriesConfig.setLowerMargin(Integer.parseInt(lowerMargin));
        }
        final String upperMargin = element.getAttributeValue("upperMargin");
        if (upperMargin != null) {
            seriesConfig.setUpperMargin(Integer.parseInt(upperMargin));
        }
        return seriesConfig;
    }

    Element store() {
        final Element e = new Element("series");
        e.setAttribute("name", name);
        e.setAttribute("type", type.name());
        e.setAttribute("lowerMargin", String.valueOf(lowerMargin));
        e.setAttribute("upperMargin", String.valueOf(upperMargin));
        return e;
    }

    public SeriesConfig copy() {
        final SeriesConfig s = new SeriesConfig(name, type);
        s.lowerMargin = lowerMargin;
        s.upperMargin = upperMargin;
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesConfig)) return false;
        SeriesConfig that = (SeriesConfig) o;
        return lowerMargin == that.lowerMargin && upperMargin == that.upperMargin && Objects.equals(name, that.name) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, lowerMargin, upperMargin);
    }
}