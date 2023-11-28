package org.kdb.inside.brains.view.console;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbToolWindowFactory;

public class KdbConsoleToolWindowFactory implements KdbToolWindowFactory {
    @Override
    public void createToolWindowContentEx(@NotNull Project project, @NotNull ToolWindowEx toolWindow) {
        KdbConsoleToolWindow.getInstance(project).initToolWindow(toolWindow);
    }
}