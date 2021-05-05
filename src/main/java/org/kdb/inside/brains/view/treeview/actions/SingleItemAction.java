package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

import java.util.List;

public abstract class SingleItemAction extends AbstractInstancesAction {
    public abstract void update(@NotNull AnActionEvent e, InstanceItem item);

    public abstract void actionPerformed(@NotNull AnActionEvent e, InstanceItem item);

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final List<InstanceItem> selectedItems = view.getSelectedItems();
        if (selectedItems.size() != 1) {
            if (ActionPlaces.isPopupPlace(e.getPlace())) {
                e.getPresentation().setVisible(false);
            }
        } else {
            update(e, selectedItems.get(0));
        }
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final List<InstanceItem> selectedItems = view.getSelectedItems();
        if (selectedItems.size() == 1) {
            actionPerformed(e, selectedItems.get(0));
        }
    }
}
