package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.ActionPromoter;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

import java.util.List;

public class ExecutionActionPromoter implements ActionPromoter {
    @Override
    public List<AnAction> promote(@NotNull List actions, @NotNull DataContext context) {
        final VirtualFile data = context.getData(CommonDataKeys.VIRTUAL_FILE);
        if (!QFileType.is(data)) {
            return null;
        }

        for (Object actionObj : actions) {
            final AnAction action = (AnAction) actionObj;
            if (action instanceof ExecuteAction || action instanceof ExecuteOnAction) {
                return List.of(action);
            }
        }
        return null;
    }
}
