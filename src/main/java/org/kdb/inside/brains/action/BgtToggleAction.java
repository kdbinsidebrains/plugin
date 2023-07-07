package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

public abstract class BgtToggleAction extends DumbAwareToggleAction {
    public BgtToggleAction() {
    }

    public BgtToggleAction(@Nullable @NlsActions.ActionText String text) {
        super(text);
    }

    public BgtToggleAction(@NotNull Supplier<String> text) {
        super(text);
    }

    public BgtToggleAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    public BgtToggleAction(@NotNull Supplier<String> dynamicText, @NotNull Supplier<String> dynamicDescription, @Nullable Icon icon) {
        super(dynamicText, dynamicDescription, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
