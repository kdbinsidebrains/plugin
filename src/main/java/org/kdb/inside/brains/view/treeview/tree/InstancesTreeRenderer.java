package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.ui.*;
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
    private final InstancesSearchSession searchSession;

    public InstancesTreeRenderer(KdbConnectionManager manager, InstancesSearchSession searchSession) {
        this.manager = manager;
        this.searchSession = searchSession;
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
        } else if (item instanceof KdbInstance inst) {
            instanceRenderer(inst, cutting);
            searchSession.customizeSearchItem(inst, this, selected);
        }
        SpeedSearchUtil.applySpeedSearchHighlightingFiltered(tree, value, (SimpleColoredComponent) this, false, selected);
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
                mainStyle |= SimpleTextAttributes.STYLE_BOLD_UNDERLINE;
            }

            if (connection.getQuery() != null) {
                icon = connection.isQueryCancelled() ? KdbIcons.Node.InstanceQueryCancelled : KdbIcons.Node.InstanceQueryRunning;
            }
        }
        changeIcon(instance, icon);

        append(instance.getName(), new SimpleTextAttributes(mainStyle, mainColour), true);
        if (showConnectionDetails) {
            append(" (" + instance.toSymbol() + ")", SimpleTextAttributes.GRAYED_SMALL_ATTRIBUTES, true);
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

    private record ColorItemIcon(Color color, Icon original) implements Icon {
        private static final Rectangle R;

        static {
            final Icon baseIcon = KdbIcons.Node.Instance;
            final int w = baseIcon.getIconWidth();
            final int h = baseIcon.getIconHeight();
            R = new Rectangle(w / 2 + 1, h / 2, w / 2, h / 2);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            original.paintIcon(c, g, x, y);

            g.setColor(color);
            g.fillOval(R.x, R.y, R.width, R.height);
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
