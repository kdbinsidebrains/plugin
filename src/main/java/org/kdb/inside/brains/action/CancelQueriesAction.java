package org.kdb.inside.brains.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Pair;
import com.intellij.reference.SoftReference;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.intellij.util.IconUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbQuery;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CancelQueriesAction extends DumbAwareAction implements AnAction.TransparentUpdate {
    private WeakReference<JBPopup> myActivePopupRef = null;

    @Override
    public void update(@NotNull final AnActionEvent e) {
        Icon icon = getTemplatePresentation().getIcon();
        String description = getTemplatePresentation().getDescription();

        final Presentation presentation = e.getPresentation();
        final KdbConnectionManager manager = KdbConnectionManager.getManager(e.getProject());
        if (manager == null) {
            presentation.setEnabled(false);
            return;
        }

        final List<InstanceConnection> connections = getValidConnections(manager);
        final int stopCount = connections.size();
        if (stopCount > 1) {
            presentation.setText("Cancel a Query on Instances ...");
            icon = IconUtil.addText(icon, String.valueOf(stopCount));
        } else if (stopCount == 1) {
            presentation.setText("Cancel a Query on Instance " + connections.get(0).getName());
        } else {
            presentation.setText("There Is No Running Queries at This Moment.");
        }

        presentation.setIcon(icon);
        presentation.setEnabled(stopCount != 0);
        presentation.setDescription(description);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final KdbConnectionManager manager = KdbConnectionManager.getManager(e.getProject());
        if (manager == null) {
            return;
        }

        final List<InstanceConnection> connections = getValidConnections(manager);
        int stopCount = connections.size();
        if (stopCount == 0) {
            return;
        }

        if (stopCount == 1) {
            connections.get(0).cancelQuery();
            return;
        }

        Pair<List<HandlerItem>, HandlerItem> itemsPair = getItemsList(connections, manager.getActiveConnection());
        if (itemsPair == null) {
            return;
        }

        final List<HandlerItem> items = itemsPair.first;
        if (items.isEmpty()) {
            return;
        }

        final HandlerItem stopAllItem = new HandlerItem("Cancel all queries", AllIcons.Actions.Suspend, true) {
            @Override
            void stop() {
                for (InstanceConnection connection : connections) {
                    connection.cancelQuery();
                }
            }
        };

        JBPopup activePopup = SoftReference.dereference(myActivePopupRef);
        if (activePopup != null) {
            stopAllItem.stop();
            activePopup.cancel();
            return;
        }

        items.add(stopAllItem);

        IPopupChooserBuilder<HandlerItem> builder = JBPopupFactory.getInstance().createPopupChooserBuilder(items)
                .setMovable(true)
                .setRenderer(new GroupedItemsListRenderer<>(new HandlerItemRenderer()))
                .setTitle(items.size() == 1 ? "Confirm the Query Should Be Cancelled" : "Cance the Query on the Instance")
                .setNamerForFiltering(o -> o.displayName)
                .setItemsChosenCallback((valuesList) -> {
                    for (HandlerItem item : valuesList) {
                        item.stop();
                    }
                })
                .addListener(new JBPopupListener() {
                    @Override
                    public void onClosed(@NotNull LightweightWindowEvent event) {
                        myActivePopupRef = null;
                    }
                })
                .setRequestFocus(true);

        if (itemsPair.second != null) {
            builder.setSelectedValue(itemsPair.second, true);
        }

        JBPopup popup = builder.createPopup();

        myActivePopupRef = new WeakReference<>(popup);

        final Project project = e.getProject();
        final InputEvent inputEvent = e.getInputEvent();
        final DataContext dataContext = e.getDataContext();
        Component component = inputEvent != null ? inputEvent.getComponent() : null;
        if (component != null) {
            popup.showUnderneathOf(component);
        } else if (project == null) {
            popup.showInBestPositionFor(dataContext);
        } else {
            popup.showCenteredInCurrentWindow(project);
        }
    }

    @NotNull
    private List<InstanceConnection> getValidConnections(@NotNull KdbConnectionManager manager) {
        return manager.getConnections().stream().filter(c -> c.getQuery() != null).collect(Collectors.toList());
    }

    @Nullable
    private static Pair<List<HandlerItem>, HandlerItem> getItemsList(List<InstanceConnection> connections, @Nullable InstanceConnection activeConnection) {
        if (connections.isEmpty()) {
            return null;
        }

        HandlerItem selected = null;
        List<HandlerItem> items = new ArrayList<>(connections.size());
        for (final InstanceConnection connection : connections) {
            final KdbQuery query = connection.getQuery();
            if (query != null) {
                HandlerItem item = new HandlerItem(connection.getName(), KdbIcons.Node.Instance, false) {
                    @Override
                    void stop() {
                        connection.cancelQuery();
                    }
                };
                items.add(item);
                if (connection == activeConnection) {
                    selected = item;
                }
            }
        }
        return Pair.create(items, selected);
    }

    abstract static class HandlerItem {
        final Icon icon;
        final String displayName;
        final boolean hasSeparator;

        HandlerItem(String displayName, Icon icon, boolean hasSeparator) {
            this.displayName = displayName;
            this.icon = icon;
            this.hasSeparator = hasSeparator;
        }

        public String toString() {
            return displayName;
        }

        abstract void stop();
    }

    static class HandlerItemRenderer extends ListItemDescriptorAdapter<HandlerItem> {
        @Nullable
        @Override
        public String getTextFor(HandlerItem item) {
            return item.displayName;
        }

        @Override
        public Icon getIconFor(HandlerItem value) {
            return value.icon;
        }

        @Override
        public boolean hasSeparatorAboveOf(HandlerItem item) {
            return item.hasSeparator;
        }
    }
}
