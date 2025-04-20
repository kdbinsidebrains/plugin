package org.kdb.inside.brains.view.chart;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.ColorIcon;

import java.awt.*;
import java.util.stream.Stream;

public final class ChartColors {
    private ChartColors() {
    }

    private static final String colorsMatrixChatGpt = """
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
            Stream.of(colorsMatrixChatGpt.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replace("#", "0x").trim())
                    .map(Color::decode)
                    .toArray(Color[]::new);

    private static final String colorsMatrixChatOriginal = """
            #3366cc #dc3912 #ff9900 #109618 #990099 #0099c6 #dd4477
            #66aa00 #b82e2e #316395 #994499 #22aa99 #aaaa11 #6633cc
            #e67300 #8b0707 #651067 #329262 #5574a6 #3b3eac #b77322
            #16d620 #b91383 #f4359e #9c5935 #a9c413 #2a778d #668d1c
            #bea413 #0c5922 #743411
            """;

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

    public static ColorIcon createIcon(Color color) {
        if (color == null) {
            return null;
        }
        return new ColorIcon(25, 15, 20, 10, color, true);
    }


/*
    private static class ColorTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        private Color editingColor;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
            label.setIcon(ChartColors.createIcon((Color) value));
            return label;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            final JLabel res = (JLabel) getTableCellRendererComponent(table, value, true, false, row, column);

            editingColor = (Color) value;
            ApplicationManager.getApplication().invokeLater(() -> {
                final Color c = ColorChooser.chooseColor(table, IdeBundle.message("dialog.title.choose.color"), editingColor, true);
                if (c == null) {
                    cancelCellEditing();
                } else {
                    editingColor = c;
                    stopCellEditing();
                }
            });
            return res;
        }

        @Override
        public Object getCellEditorValue() {
            return editingColor;
        }
    }
*/

}