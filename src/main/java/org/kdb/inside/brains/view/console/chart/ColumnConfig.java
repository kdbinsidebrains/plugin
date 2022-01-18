package org.kdb.inside.brains.view.console.chart;

import org.kdb.inside.brains.KdbType;

import java.awt.*;
import java.util.Set;

public class ColumnConfig {
    private final int index;
    private final String name;
    private final KdbType type;

    protected static final Set<KdbType> NUMBER_TYPES = Set.of(
            KdbType.BYTE,
            KdbType.SHORT,
            KdbType.INT,
            KdbType.LONG,
            KdbType.REAL,
            KdbType.FLOAT
    );

    protected static final Set<KdbType> TEMPORAL_TYPES = Set.of(
            KdbType.SECOND,
            KdbType.MINUTE,
            KdbType.MONTH,
            KdbType.TIME,
            KdbType.DATE,
            KdbType.DATETIME,
            KdbType.TIMESPAN,
            KdbType.TIMESTAMP
    );

    protected static final Color[] DEFAULT_COLORS = new Color[]{
            Color.decode("0x3366cc"),
            Color.decode("0xdc3912"),
            Color.decode("0xff9900"),
            Color.decode("0x109618"),
            Color.decode("0x990099"),
            Color.decode("0x0099c6"),
            Color.decode("0xdd4477"),
            Color.decode("0x66aa00"),
            Color.decode("0xb82e2e"),
            Color.decode("0x316395"),
            Color.decode("0x994499"),
            Color.decode("0x22aa99"),
            Color.decode("0xaaaa11"),
            Color.decode("0x6633cc"),
            Color.decode("0xe67300"),
            Color.decode("0x8b0707"),
            Color.decode("0x651067"),
            Color.decode("0x329262"),
            Color.decode("0x5574a6"),
            Color.decode("0x3b3eac"),
            Color.decode("0xb77322"),
            Color.decode("0x16d620"),
            Color.decode("0xb91383"),
            Color.decode("0xf4359e"),
            Color.decode("0x9c5935"),
            Color.decode("0xa9c413"),
            Color.decode("0x2a778d"),
            Color.decode("0x668d1c"),
            Color.decode("0xbea413"),
            Color.decode("0x0c5922"),
            Color.decode("0x743411")
    };

    public ColumnConfig(int index, String name, KdbType type) {
        this.index = index;
        this.name = name;
        this.type = type;
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

    // Cycled color wheel by index
    public static Color getDefaultColor(int index) {
        return DEFAULT_COLORS[index - (DEFAULT_COLORS.length * (index / DEFAULT_COLORS.length))];
    }
}
