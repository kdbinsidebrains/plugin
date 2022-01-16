package org.kdb.inside.brains.view.console.chart.line;

import com.intellij.util.ui.ColorIcon;
import org.kdb.inside.brains.KdbType;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class ColumnConfig {
    private final int index;
    private final String name;
    private final KdbType type;

    private String group;
    private ColorIcon colorIcon;
    private LineStyle lineStyle = LineStyle.LINE;

    private static final Set<KdbType> NUMBER_TYPES = Set.of(
            KdbType.BYTE,
            KdbType.SHORT,
            KdbType.INT,
            KdbType.LONG,
            KdbType.REAL,
            KdbType.FLOAT
    );

    private static final Set<KdbType> TEMPORAL_TYPES = Set.of(
            KdbType.SECOND,
            KdbType.MINUTE,
            KdbType.MONTH,
            KdbType.TIME,
            KdbType.DATE,
            KdbType.DATETIME,
            KdbType.TIMESPAN,
            KdbType.TIMESTAMP
    );

    public ColumnConfig(int index, String name, KdbType type, Color color) {
        this.name = name;
        this.type = type;
        this.index = index;
        this.colorIcon = creaColorIcon(color);
    }

    public String getName() {
        return name;
    }

    public KdbType getType() {
        return type;
    }

    public int getIndex() {
        return index;
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

    public String getLabel() {
        return "<html>" + name + " <font color=\"gray\">(" + type.getTypeName().toLowerCase() + ")</font></html>";
    }

    public String getLabelWidth() {
        return "  " + name + " (" + type.getTypeName().toLowerCase() + ")  ";
    }

    public static boolean isNumberType(KdbType type) {
        return NUMBER_TYPES.contains(type);
    }

    public static boolean isTemporalType(KdbType type) {
        return TEMPORAL_TYPES.contains(type);
    }

    public static boolean isRangeAllowed(KdbType type) {
        return type != null && isNumberType(type);
    }

    public static boolean isDomainAllowed(KdbType type) {
        return type != null && (isNumberType(type) || isTemporalType(type));
    }
}
