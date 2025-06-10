package org.kdb.inside.brains.view.chart;

import com.intellij.ui.JBColor;

import java.awt.*;
import java.util.stream.Stream;

public final class ChartColors {
    private ChartColors() {
    }

    private static final String COLOR_MATRIX_CHART = """
                #FF5733 #33FF57 #3357FF #FF33A6 #A633FF #33FFF6
                #F6FF33 #5733FF #33A6FF #A6FF33 #57FF33 #FF6F33
                #6FFF33 #33FF6F #336FFF #6F33FF #FF336F #FF8C33
                #8CFF33 #33FF8C #338CFF #8C33FF #FF33C4 #FFB833
                #B8FF33 #33FFB8 #33B8FF #B833FF #FF33E9 #FFD233
                #D2FF33 #33FFD2 #33D2FF #D233FF #FF33FF #FF3333
                #FF6633 #FF9933 #FFCC33 #FFFF33 #CCFF33 #99FF33
                #66FF33 #33FF33 #33FF66 #33FF99 #33FFCC #33FFFF
                #33CCFF #3399FF #3366FF #3333FF #6633FF #9933FF
                #CC33FF #FF33FF #FF33CC #FF3399 #FF3366 #FF3333
            """;

    public static final Color[] DEFAULT_COLORS =
            Stream.of(COLOR_MATRIX_CHART.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replace("#", "0x").trim())
                    .map(Color::decode)
                    .toArray(Color[]::new);

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

    public static Color transparent(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    // Cycled colour wheel by index
    public static Color getDefaultColor(int index) {
        return DEFAULT_COLORS[index - (DEFAULT_COLORS.length * (index / DEFAULT_COLORS.length))];
    }
}