package org.kdb.inside.brains.lang.binding;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Toggleable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BindConnectionAction extends AnAction implements Toggleable {
    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Presentation presentation = e.getPresentation();

        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        final EditorsBindingService service = project.getService(EditorsBindingService.class);
        presentation.setEnabled(service.getStrategy() == EditorsBindingStrategy.MANUAL && service.isBindable());
        Toggleable.setSelected(presentation, service.hasBinding());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Presentation presentation = e.getPresentation();
        if (project == null) {
            return;
        }

        final EditorsBindingService service = project.getService(EditorsBindingService.class);
        final boolean bind = !service.hasBinding();

        service.toggleBinding(bind);
        Toggleable.setSelected(presentation, bind);
    }
}
