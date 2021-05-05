package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class ConnectionAction extends AbstractInstancesAction {
    protected abstract void update(@NotNull AnActionEvent e, KdbConnectionManager manager, List<KdbInstance> instances);

    protected abstract void actionPerformed(@NotNull AnActionEvent e, KdbConnectionManager manager, List<KdbInstance> instances);

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final Project project = e.getProject();
        if (project == null) {
            return;
        }
        actionPerformed(e, KdbConnectionManager.getManager(e.getProject()), collectAllInstances(view.getSelectedItems()));
    }

    @Override
    public final void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        update(e, KdbConnectionManager.getManager(e.getProject()), collectAllInstances(view.getSelectedItems()));
    }

    private List<KdbInstance> collectAllInstances(List<InstanceItem> items) {
        return items.stream().filter(p -> p instanceof KdbInstance).map(p -> (KdbInstance) p).collect(Collectors.toList());
    }

    public static void connectInstances(KdbConnectionManager manager, List<KdbInstance> instances) {
        final Iterator<KdbInstance> iterator = instances.iterator();

        // Activate first
        if (iterator.hasNext()) {
            manager.activate(iterator.next());
        }
        // but just register rest
        while (iterator.hasNext()) {
            manager.register(iterator.next());
        }
    }

    public static void disconnectInstances(KdbConnectionManager manager, List<KdbInstance> instances) {
        instances.stream().map(manager::getConnection).filter(Objects::nonNull).forEach(InstanceConnection::disconnect);
    }
}
