package org.kdb.inside.brains.view.treeview.scope;

import com.intellij.openapi.options.newEditor.SettingsDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbScope;

public class ScopesEditorDialog extends SettingsDialog {
    private KdbScope selectedScope;

    private final ScopesEditorPanel scopesEditorPanel;

    public ScopesEditorDialog(Project project, @NotNull ScopesEditorPanel scopesEditorPanel) {
        super(project, "KdbInstancesScope", scopesEditorPanel, true, false);
        this.scopesEditorPanel = scopesEditorPanel;
    }

    @Override
    public void doOKAction() {
        selectedScope = scopesEditorPanel.getSelectedScope();
        super.doOKAction();
    }

    public static KdbScope showDialog(final Project project, @Nullable final KdbScope scope) {
        final ScopesEditorPanel configurable = new ScopesEditorPanel(project);
        final ScopesEditorDialog dialog = new ScopesEditorDialog(project, configurable);
        if (scope != null) {
            configurable.selectNodeInTree(scope.getName());
        }
        dialog.setSize(700, 500);
        dialog.show();

        return dialog.selectedScope;
    }
}
