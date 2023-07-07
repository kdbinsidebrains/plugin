package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Supplier;

public abstract class EdtAction extends DumbAwareAction {
    public EdtAction() {
    }

    public EdtAction(@Nullable Icon icon) {
        super(icon);
    }

    public EdtAction(@Nullable @NlsActions.ActionText String text) {
        super(text);
    }

    public EdtAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText) {
        super(dynamicText);
    }

    public EdtAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    public EdtAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText, @NotNull Supplier<@NlsActions.ActionDescription String> dynamicDescription, @Nullable Icon icon) {
        super(dynamicText, dynamicDescription, icon);
    }

    public EdtAction(@NotNull Supplier<@NlsActions.ActionText String> dynamicText, @Nullable Icon icon) {
        super(dynamicText, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
