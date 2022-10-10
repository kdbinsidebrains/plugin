package org.kdb.inside.brains.view.inspector;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.ActionPlaces;

import javax.swing.*;

public class InspectorToolWindow extends SimpleToolWindowPanel implements Disposable, DumbAware {
    private final Project project;

    public InspectorToolWindow(Project project) {
        super(true);
        this.project = project;
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        final ContentManager cm = toolWindow.getContentManager();

        final Content content = cm.getFactory().createContent(this, "", false);
        cm.addContent(content);

        setToolbar(createToolbar());
    }

    private @NotNull JComponent createToolbar() {
        final ActionGroup group = new DefaultActionGroup(
                new AnAction("Refresh Instance", "Reloads the instance structure", KdbIcons.Inspector.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refreshInstance();
                    }
                }
        );

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.INSPECTOR_VIEW_TOOLBAR, group, true);
        actionToolbar.setTargetComponent(this);
        return actionToolbar.getComponent();
    }

    private void refreshInstance() {
    }

    @Override
    public void dispose() {

    }
}
