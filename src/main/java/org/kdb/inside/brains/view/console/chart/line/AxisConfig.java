package org.kdb.inside.brains.view.console.chart.line;

import com.intellij.util.ui.ColorIcon;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.console.chart.ColumnConfig;

import javax.swing.*;
import java.awt.*;

public class AxisConfig extends ColumnConfig {
    private String group;
    private ColorIcon colorIcon;
    private LineStyle lineStyle = LineStyle.LINE;

    public AxisConfig(int index, String name, KdbType type, Color color) {
        super(index, name, type);
        this.colorIcon = creaColorIcon(color);
    }

    public Color getColor() {
        return colorIcon == null ? null : colorIcon.getIconColor();
    }

    public Icon getColorIcon() {
        return colorIcon;
    }

    public void setColor(Color color) {
        this.colorIcon = creaColorIcon(color);
    }

    public static ColorIcon creaColorIcon(Color color) {
        if (color == null) {
            return null;
        }
        return new ColorIcon(25, 15, 20, 10, color, true);
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public LineStyle getLineStyle() {
        return lineStyle;
    }

    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle;
    }

    public static boolean isRangeAllowed(KdbType type) {
        return type != null && isNumberType(type);
    }

    public static boolean isDomainAllowed(KdbType type) {
        return type != null && (isNumberType(type) || isTemporalType(type));
    }

}
