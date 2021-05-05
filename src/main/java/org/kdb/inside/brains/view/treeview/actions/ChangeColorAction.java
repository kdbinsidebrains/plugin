package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.ui.ColorChooserService;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class ChangeColorAction extends AbstractInstancesAction {
    @Override
    public void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final Presentation presentation = e.getPresentation();
        presentation.setEnabled(!view.getSelectedItems().isEmpty());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final List<InstanceItem> items = view.getSelectedItems();
        if (items.isEmpty()) {
            return;
        }

        final Color color = items.stream().map(InstanceItem::getColor).filter(Objects::nonNull).findAny().orElse(null);
        final Color selectedColor = ColorChooserService.getInstance().showDialog(e.getProject(), view, "Change Item Color", color, false, null, false);
        items.forEach(i -> i.setColor(selectedColor));

        // Required to repaint toolbar if colour was chagned
//        ActivityTracker.getInstance().inc();
    }
}
