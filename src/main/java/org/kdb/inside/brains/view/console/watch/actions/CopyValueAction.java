package org.kdb.inside.brains.view.console.watch.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.VariableValue;
import org.kdb.inside.brains.view.console.watch.WatchesView;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.stream.Collectors;

public class CopyValueAction extends AnWatchesTreeAction {
    @Override
    protected void perform(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        final List<VariableNode> variables = watchesView.getSelectedVariables();
        if (variables.isEmpty()) {
            return;
        }

        final KdbOutputFormatter outputFormatter = watchesView.getOutputFormatter();
        final String content = variables.stream().map(VariableNode::getValue).map(VariableValue::value).map(outputFormatter::objectToString).collect(Collectors.joining(System.lineSeparator()));
        CopyPasteManager.getInstance().setContents(new StringSelection(content));
    }

    @Override
    protected boolean isEnabled(@NotNull AnActionEvent e, @NotNull WatchesView watchesView) {
        return !watchesView.getSelectedVariables().isEmpty();
    }
}
