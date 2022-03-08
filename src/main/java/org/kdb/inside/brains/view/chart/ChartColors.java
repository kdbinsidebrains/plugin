package org.kdb.inside.brains.view.chart;

import com.intellij.ui.JBColor;

import java.awt.*;

public final class ChartColors {
    private ChartColors() {
    }

    public static final Color[] DEFAULT_COLORS = new Color[]{
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

    public static final JBColor POSITIVE = new JBColor(Color.decode("0x1c6b3b"), Color.decode("0x49754d"));
    public static final JBColor NEGATIVE = new JBColor(Color.decode("0xea0d3f"), Color.decode("0xe3415d"));

    public static final JBColor POSITIVE_40 = new JBColor(
            transparent(Color.decode("0x1c6b3b"), 40),
            transparent(Color.decode("0x49754d"), 40)
    );
    public static final JBColor NEGATIVE_40 = new JBColor(
            transparent(Color.decode("0xea0d3f"), 40),
            transparent(Color.decode("0xe3415d"), 40)
    );

    private static Color transparent(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // Cycled color wheel by index
    public static Color getDefaultColor(int index) {
        return DEFAULT_COLORS[index - (DEFAULT_COLORS.length * (index / DEFAULT_COLORS.length))];
    }
}