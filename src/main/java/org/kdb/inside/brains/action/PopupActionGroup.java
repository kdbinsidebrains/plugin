package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PopupActionGroup extends DefaultActionGroup {
    public PopupActionGroup(@Nullable @NlsActions.ActionText String shortName, Icon icon) {
        super(shortName, true);

        final Presentation presentation = getTemplatePresentation();
        presentation.setIcon(icon);
        presentation.setPerformGroup(false);
        presentation.setHideGroupIfEmpty(false);
        presentation.setDisableGroupIfEmpty(false);
    }
}
