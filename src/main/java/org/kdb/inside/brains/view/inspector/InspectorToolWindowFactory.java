package org.kdb.inside.brains.view.inspector;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NotNull;

public class InspectorToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (!(toolWindow instanceof ToolWindowEx)) {
            return;
        }

        final ToolWindowEx tw = (ToolWindowEx) toolWindow;
        tw.setDefaultContentUiType(ToolWindowContentUiType.TABBED);

        // Lazy init
        DumbService.getInstance(project).runWhenSmart(() -> project.getService(InspectorToolWindow.class).initToolWindow(tw));
    }
}
