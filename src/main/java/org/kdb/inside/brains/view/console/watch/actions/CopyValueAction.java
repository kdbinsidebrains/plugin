package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.WatchesView;

import java.awt.datatransfer.StringSelection;
import java.util.List;

public class CopyValueAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> variables = watchesView.getSelectedVariables();
        if (variables.isEmpty()) {
            return;
        }

        final KdbOutputFormatter outputFormatter = watchesView.getOutputFormatter();

        final StringBuilder b = new StringBuilder();
        for (VariableNode variable : variables) {
            final String s = outputFormatter.objectToString(variable.getValue().value());
            b.append(variable.getExpression()).append(" = ").append(s).append(System.lineSeparator());
        }
        if (!b.isEmpty()) {
            b.setLength(b.length() - System.lineSeparator().length());
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(b.toString()));
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        return !watchesView.getSelectedVariables().isEmpty();
    }
}
