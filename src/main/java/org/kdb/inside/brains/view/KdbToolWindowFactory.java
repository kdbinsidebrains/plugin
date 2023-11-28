package org.kdb.inside.brains.view;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NotNull;

public interface KdbToolWindowFactory extends ToolWindowFactory, DumbAware {
    void createToolWindowContentEx(@NotNull Project project, @NotNull ToolWindowEx toolWindow);

    default void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (!(toolWindow instanceof ToolWindowEx tw)) {
            return;
        }
        createToolWindowContentEx(project, tw);
    }

    @Override
    default boolean isApplicable(@NotNull Project project) {
        return KdbToolWindowManager.isPluginEnabled(project);
    }
}
