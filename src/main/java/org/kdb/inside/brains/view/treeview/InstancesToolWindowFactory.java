package org.kdb.inside.brains.view.treeview;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NotNull;

public class InstancesToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        if (!(toolWindow instanceof ToolWindowEx)) {
            return;
        }

        final ToolWindowEx tw = (ToolWindowEx) toolWindow;
        tw.setDefaultContentUiType(ToolWindowContentUiType.TABBED);

        // Lazy init
        DumbService.getInstance(project).runWhenSmart(() -> project.getService(InstancesToolWindow.class).initToolWindow(tw));
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // TODO: Any way to check that it's KDB project here?
        return true;
    }
}
