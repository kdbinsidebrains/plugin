package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.view.console.KdbConsoleToolWindow;

public class ExecuteAction extends AnAction implements DumbAware {
    private final InstanceConnection myConnection;

    public ExecuteAction() {
        myConnection = null;
    }

    public ExecuteAction(InstanceConnection connection) {
        super(connection.getName(), connection.getDetails(), null);
        myConnection = connection;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Presentation presentation = e.getPresentation();
        if (ActionPlaces.KEYBOARD_SHORTCUT.equals(e.getPlace())) {
            return;
        }

        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        final VirtualFile vf = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);

        if (project == null || !QFileType.is(vf)) {
            presentation.setEnabled(false);
            return;
        }
        final InstanceConnection activeInstance = getConnection(project);
        presentation.setEnabled(activeInstance != null && activeInstance.getState() == InstanceState.CONNECTED);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (editor == null || project == null) {
            return;
        }

        final LogicalPosition pos = editor.getCaretModel().getLogicalPosition();
        ReadAction.run(() -> {
            final TextRange textRange;
            final SelectionModel selectionModel = editor.getSelectionModel();

            if (selectionModel.hasSelection()) {
                textRange = new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
            } else {
                final Document document = editor.getDocument();
                textRange = new TextRange(document.getLineStartOffset(pos.line), document.getLineEndOffset(pos.line));
            }

            final InstanceConnection connection = getConnection(project);
            if (connection != null) {
                execute(project, editor, connection, textRange);
            }
        });
    }

    protected @Nullable InstanceConnection getConnection(Project project) {
        if (myConnection != null) {
            return myConnection;
        }
        return KdbConnectionManager.getManager(project).getActiveConnection();
    }

    protected void execute(Project project, Editor editor, InstanceConnection connection, TextRange range) {
        KdbConsoleToolWindow.getInstance(project).execute(connection, editor.getDocument().getText(range));
    }
}
