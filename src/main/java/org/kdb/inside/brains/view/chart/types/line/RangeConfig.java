package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.ColorIcon;
import org.jdom.Element;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnConfig;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

class RangeConfig extends ColumnConfig {
    private ColorIcon icon;
    private SeriesConfig series;
    private float width = 2.0f;

    public RangeConfig(String name, KdbType type, Color color) {
        super(name, type);
        this.icon = createIcon(color);
    }

    public Color getColor() {
        return icon == null ? null : icon.getIconColor();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setColor(Color color) {
        this.icon = createIcon(color);
    }

    public SeriesConfig getSeries() {
        return series;
    }

    public void setSeries(SeriesConfig group) {
        this.series = group == null || group.isEmpty() ? null : group;
    }

    public static RangeConfig copy(RangeConfig config, SeriesConfig series) {
        final RangeConfig r = new RangeConfig(config.getName(), config.getType(), config.icon.getIconColor());
        r.width = config.width;
        r.series = series;
        return r;
    }

    public static RangeConfig restore(Element element) {
        if (element == null) {
            return null;
        }
        final ColumnConfig restore = ColumnConfig.restore(element);
        final Color color = ColorUtil.fromHex(element.getAttributeValue("color"));
        final float width = Float.parseFloat(element.getAttributeValue("width"));

        final RangeConfig c = new RangeConfig(restore.getName(), restore.getType(), color);
        c.setWidth(width);
        return c;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void copyFrom(RangeConfig tItem) {
        setSeries(tItem.getSeries());
        setColor(tItem.getColor());
        setWidth(tItem.getWidth());
    }

    @Override
    public Element store() {
        final Element e = super.store();
        e.setAttribute("color", ColorUtil.toHex(icon.getIconColor()));
        e.setAttribute("width", String.valueOf(width));
        return e;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RangeConfig)) return false;
        if (!super.equals(o)) return false;
        RangeConfig that = (RangeConfig) o;
        return Float.compare(that.width, width) == 0 && Objects.equals(icon, that.icon) && Objects.equals(series, that.series);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), icon, series, width);
    }

    public static ColorIcon createIcon(Color color) {
        if (color == null) {
            return null;
        }
        return new ColorIcon(25, 15, 20, 10, color, true);
    }
}
