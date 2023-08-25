package org.kdb.inside.brains.view.console.watch;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;

class WatchesTreeRenderer extends ColoredTreeCellRenderer {
    private final KdbOutputFormatter outputFormatter;
    private boolean showTypes;

    public WatchesTreeRenderer(boolean showTypes, KdbOutputFormatter outputFormatter) {
        this.outputFormatter = outputFormatter;
        this.showTypes = showTypes;
    }

    public void setShowTypes(boolean showTypes) {
        this.showTypes = showTypes;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof VariableNode node)) {
            return;
        }

        append(node.getExpression(), node.isChanged() ? SimpleTextAttributes.LINK_BOLD_ATTRIBUTES : SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);

        append(" = ");

        final VariableValue v = node.getValue();
        if (node.isUpdating() || v == null) {
            append("updating...", SimpleTextAttributes.GRAY_ATTRIBUTES);
        } else if (v.valid()) {
            final Object o = v.value();
            showType(o == null ? "null" : KdbType.typeOf(o).getTypeName());
            append(outputFormatter.objectToString(o));
        } else {
            final String msg;
            final Object err = v.value();
            if (err instanceof char[] ch) {
                msg = new String(ch);
            } else if (err instanceof String s) {
                msg = s;
            } else {
                msg = String.valueOf(err);
            }
            showType("`error");
            append(msg, SimpleTextAttributes.ERROR_ATTRIBUTES);
        }
    }

    private void showType(String type) {
        if (showTypes) {
            append(" {", SimpleTextAttributes.GRAY_ATTRIBUTES);
            append(type.toLowerCase(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            append("} ", SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
}