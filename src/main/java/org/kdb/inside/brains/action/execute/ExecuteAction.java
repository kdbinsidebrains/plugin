package org.kdb.inside.brains.action.execute;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.view.console.KdbConsoleToolWindow;

public class ExecuteAction extends DumbAwareAction {
    private final InstanceConnection myConnection;

    public ExecuteAction() {
        myConnection = null;
    }

    public ExecuteAction(InstanceConnection connection) {
        super(connection.getName(), connection.getDetails(), null);
        myConnection = connection;
    }

    public static boolean isExecutedAllowed(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return false;
        }

        final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        return QFileType.is(file);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        if (org.kdb.inside.brains.action.ActionPlaces.KEYBOARD_SHORTCUT.equals(e.getPlace())) {
            presentation.setEnabled(true);
            presentation.setVisible(false);
        } else {

            final boolean allowed = isExecutedAllowed(e);
            if (ActionPlaces.MAIN_TOOLBAR.equals(e.getPlace()) || "popup".equals(e.getPlace())) {
                presentation.setVisible(true);
            } else {
                presentation.setVisible(allowed);
            }

            if (allowed) {
                final InstanceConnection activeInstance = getConnection(e.getProject());
                presentation.setEnabled(activeInstance != null && activeInstance.getState() == InstanceState.CONNECTED);
            } else {
                presentation.setEnabled(false);
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (editor == null || project == null) {
            return;
        }

        ReadAction.run(() -> {
            final InstanceConnection connection = getConnection(project);
            if (connection == null) {
                return;
            }

            final TextRange range = getExecutionRange(editor, dataContext);
            if (range != null && !range.isEmpty()) {
                execute(project, editor, connection, range);
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    protected TextRange getExecutionRange(Editor editor, DataContext context) {
        final CaretModel caretModel = editor.getCaretModel();

        final SelectionModel selectionModel = editor.getSelectionModel();
        if (selectionModel.hasSelection()) {
            return new TextRange(selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
        }

        final Document document = editor.getDocument();
        final LogicalPosition pos = caretModel.getLogicalPosition();
        return new TextRange(document.getLineStartOffset(pos.line), document.getLineEndOffset(pos.line));
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
