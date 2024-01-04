package org.kdb.inside.brains.action.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.EdtToggleAction;

import java.awt.event.InputEvent;

public class NewUIRunMoreActions extends EdtToggleAction implements DumbAware {
    public NewUIRunMoreActions() {
        super("More Actions", null, AllIcons.Actions.More);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return Toggleable.isSelected(e.getPresentation());
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        if (!state) {
            return;
        }

        final InputEvent event = e.getInputEvent();
        if (event == null || event.getComponent() == null) {
            return;
        }

        var presentation = e.getPresentation();
        final ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("Kdb.MoreRunToolbarActionsGroup");
        var popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null, actionGroup, e.getDataContext(), false, true, false, () -> Toggleable.setSelected(presentation, false), 30, null);
        popup.showUnderneathOf(event.getComponent());
    }
}
