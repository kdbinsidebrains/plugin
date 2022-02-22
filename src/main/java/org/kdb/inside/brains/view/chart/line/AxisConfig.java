package org.kdb.inside.brains.view.chart.line;

import com.intellij.util.ui.ColorIcon;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.ColumnConfig;

import javax.swing.*;
import java.awt.*;

public class AxisConfig extends ColumnConfig {
    private ColorIcon icon;
    private SeriesConfig series;
    private Number width = 2.0;

    public AxisConfig(int index, String name, KdbType type, Color color) {
        super(index, name, type);
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

    public Number getWidth() {
        return width;
    }

    public void setWidth(Number width) {
        this.width = width;
    }

    public static boolean isRangeAllowed(KdbType type) {
        return isNumberType(type);
    }

    public static boolean isDomainAllowed(KdbType type) {
        return isNumberType(type) || isTemporalType(type);
    }

    public static ColorIcon createIcon(Color color) {
        if (color == null) {
            return null;
        }
        return new ColorIcon(25, 15, 20, 10, color, true);
    }
}
