package org.kdb.inside.brains.action.newui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NewUIRunMoreActions extends ToggleAction implements DumbAware {
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
        var presentation = e.getPresentation();
        var component = e.getInputEvent().getComponent();
        if (!(component instanceof JComponent)) {
            return;

        }
        final ActionGroup actionGroup = (ActionGroup) ActionManager.getInstance().getAction("Kdb.MoreRunToolbarActionsGroup");
        var popup = JBPopupFactory.getInstance().createActionGroupPopup(
                null, actionGroup, e.getDataContext(), false, true, false, () -> Toggleable.setSelected(presentation, false), 30, null);
        popup.showUnderneathOf(component);
    }
}
