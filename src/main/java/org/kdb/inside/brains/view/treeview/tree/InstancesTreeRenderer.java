package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.text.DateFormatUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.*;

import javax.swing.*;
import java.awt.*;

public class InstancesTreeRenderer extends ColoredTreeCellRenderer {
    private boolean showConnectionDetails = true;

    private final KdbConnectionManager manager;

    public InstancesTreeRenderer(KdbConnectionManager manager) {
        this.manager = manager;
        this.myUsedCustomSpeedSearchHighlighting = true;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (value instanceof LoadingNode) {
            append(LoadingNode.getText());
            return;
        }

        final InstanceItem item = (InstanceItem) value;
        final InstancesTreeModel model = (InstancesTreeModel) tree.getModel();

        final boolean cutting = model.isCuttingItem(item);

        if (item instanceof KdbScope) {
            scopeRenderer((KdbScope) item);
        } else if (item instanceof PackageItem) {
            packageRenderer(item, cutting);
        } else if (item instanceof KdbInstance) {
            instanceRenderer((KdbInstance) item, cutting);
        }

        SpeedSearchUtil.applySpeedSearchHighlightingFiltered(tree, value, this, false, selected);
    }

    private void scopeRenderer(KdbScope scope) {
        changeIcon(scope, scope.getType().getIcon());
        append(scope.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }

    private void packageRenderer(InstanceItem item, boolean cutting) {
        changeIcon(item, KdbIcons.Node.Package);
        final SimpleTextAttributes a = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, cutting ? JBColor.GRAY : null);
        append(item.getName(), a);
    }

    private void instanceRenderer(KdbInstance instance, boolean cutting) {
        setToolTipText(instance.toString());

        Icon icon = KdbIcons.Node.Instance;
        int mainStyle = SimpleTextAttributes.STYLE_PLAIN;
        Color mainColour = cutting ? JBColor.GRAY : null;

        String comment = null;
        final InstanceConnection active = manager.getActiveConnection();
        final InstanceConnection connection = manager.getConnection(instance);
        if (connection != null) {
            final InstanceState state = connection.getState();
            if (state == InstanceState.DISCONNECTED) {
                final Exception error = connection.getDisconnectError();
                if (error != null) {
                    mainColour = JBColor.RED;
                    comment = error.getMessage();
                }
            } else if (state == InstanceState.CONNECTED) {
                mainStyle |= SimpleTextAttributes.STYLE_BOLD;
                comment = "Connected";
            } else if (state == InstanceState.CONNECTING) {
                mainStyle |= SimpleTextAttributes.STYLE_ITALIC;
                comment = "Connecting...";
            }

            if (active == connection) {
                mainStyle |= SimpleTextAttributes.STYLE_UNDERLINE;
            }

            if (connection.getQuery() != null) {
                icon = connection.isQueryCancelled() ? KdbIcons.Node.InstanceQueryCancelled : KdbIcons.Node.InstanceQueryRunning;
            }
        }
        changeIcon(instance, icon);

        append(instance.getName(), new SimpleTextAttributes(mainStyle, mainColour), true);
        if (showConnectionDetails) {
            append(" (" + instance + ")", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES, false);
        }

        if (comment != null) {
            append("    ");
            append(DateFormatUtil.formatDateTime(connection.getStateChangeTime()) + ", " + comment, SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES, false);
        }
    }

    private void changeIcon(InstanceItem item, Icon icon) {
        final Color color = item.getInheritedColor();
        if (color != null) {
            setIcon(new ColorItemIcon(color, icon));
        } else {
            setIcon(icon);
        }
    }

    void showConnectionDetails(boolean state) {
        showConnectionDetails = state;
    }

    boolean isShownConnectionDetails() {
        return showConnectionDetails;
    }

    private static class ColorItemIcon implements Icon {
        private final Color color;
        private final Icon original;

        protected ColorItemIcon(Color color, Icon original) {
            this.color = color;
            this.original = original;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            original.paintIcon(c, g, x, y);

            final int w = original.getIconWidth();
            final int h = original.getIconHeight();

            g.setColor(color);
            g.fillOval(w / 2 + 1, h / 2, w / 2, h / 2);
        }

        @Override
        public int getIconWidth() {
            return original.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return original.getIconHeight();
        }
    }
}
