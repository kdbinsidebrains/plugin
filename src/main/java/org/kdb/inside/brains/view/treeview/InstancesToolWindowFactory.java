package org.kdb.inside.brains.view.treeview;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowContentUiType;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbToolWindowFactory;

public class InstancesToolWindowFactory implements KdbToolWindowFactory {
    @Override
    public void createToolWindowContentEx(@NotNull Project project, @NotNull ToolWindowEx toolWindow) {
        toolWindow.setDefaultContentUiType(ToolWindowContentUiType.TABBED);
        project.getService(InstancesToolWindow.class).initToolWindow(toolWindow);
    }
}
