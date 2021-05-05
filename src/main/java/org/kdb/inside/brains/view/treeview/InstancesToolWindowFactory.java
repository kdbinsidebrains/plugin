package org.kdb.inside.brains.view.treeview;

import com.intellij.openapi.components.ServiceManager;
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
        tw.setDefaultContentUiType(ToolWindowContentUiType.COMBO);
//        tw.getComponent().putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true");
//        tw.setTabActions();

//            DefaultActionGroup group = new DefaultActionGroup() {
//                {
//                    getTemplatePresentation().setText(IdeBundle.message("group.view.options"));
//                    setPopup(true);
//                    add(myAllTodos.createAutoScrollToSourceAction());
//                    addSeparator();
//                    addAll(myAllTodos.createGroupByActionGroup());
//                }
//            };
//            ((ToolWindowEx)toolWindow).setAdditionalGearActions(group);

        // Lazy init
        DumbService.getInstance(project).runWhenSmart(() -> ServiceManager.getService(project, InstancesToolWindow.class).initToolWindow(tw));
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // TODO: Any way to check that it's KDB project here?
        return true;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return false;
    }
}
