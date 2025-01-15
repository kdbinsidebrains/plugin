package org.kdb.inside.brains.view.chart.types.line;

import org.jdom.Element;
import org.jfree.chart.axis.ValueAxis;

public record SeriesDefinition(String name, SeriesStyle style, int lowerMargin, int upperMargin) {
    public static final int DEFAULT_LOWER_MARGIN = (int) (ValueAxis.DEFAULT_LOWER_MARGIN * 100);
    public static final int DEFAULT_UPPER_MARGIN = (int) (ValueAxis.DEFAULT_UPPER_MARGIN * 100);

    public SeriesDefinition(String name, SeriesStyle style) {
        this(name, style, DEFAULT_LOWER_MARGIN, DEFAULT_UPPER_MARGIN);
    }

    public static SeriesDefinition restore(Element element) {
        final String name = element.getAttributeValue("name");
        final SeriesStyle type = SeriesStyle.valueOf(element.getAttributeValue("style"));
        final int lowerMargin = Integer.parseInt(element.getAttributeValue("lowerMargin", String.valueOf(DEFAULT_LOWER_MARGIN)));
        final int upperMargin = Integer.parseInt(element.getAttributeValue("upperMargin", String.valueOf(DEFAULT_UPPER_MARGIN)));
        return new SeriesDefinition(name, type, lowerMargin, upperMargin);
    }

    Element store() {
        final Element e = new Element("series");
        e.setAttribute("name", name);
        e.setAttribute("style", style.name());
        e.setAttribute("lowerMargin", String.valueOf(lowerMargin));
        e.setAttribute("upperMargin", String.valueOf(upperMargin));
        return e;
    }
}