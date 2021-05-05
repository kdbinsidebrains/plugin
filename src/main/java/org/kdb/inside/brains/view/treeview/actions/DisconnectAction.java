package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbInstance;

import java.util.List;

public class DisconnectAction extends ConnectionAction {
    @Override
    protected void update(@NotNull AnActionEvent e, KdbConnectionManager manager, List<KdbInstance> instances) {
        final Presentation presentation = e.getPresentation();
        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            presentation.setVisible(!instances.isEmpty());
        }
        presentation.setEnabled(instances.stream().map(manager::getConnection).anyMatch(c -> c != null && c.getState().isDisconnectable()));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, KdbConnectionManager manager, List<KdbInstance> instances) {
        disconnectInstances(manager, instances);
    }
}
