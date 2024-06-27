package org.kdb.inside.brains.action.execute;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.view.console.KdbConsoleToolWindow;

public class ExecuteAction extends BgtAction {
    private final InstanceConnection myConnection;

    public ExecuteAction() {
        myConnection = null;
    }

    public ExecuteAction(InstanceConnection connection) {
        super(connection.getName(), connection.getSymbol(), null);
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

            final Editor editor = CommonDataKeys.EDITOR.getData(e.getDataContext());
            if (allowed && editor != null) {
                final InstanceConnection activeInstance = getConnection(e.getProject());
                presentation.setEnabled(activeInstance != null && activeInstance.isConnected());
            } else {
                presentation.setEnabled(false);
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final DataContext context = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(context);
        if (project == null) {
            return;
        }

        final String text = ReadAction.compute(() -> getExecutionContent(context));
        if (text == null || text.isEmpty()) {
            return;
        }

        final InstanceConnection connection = getConnection(project);
        if (connection != null && connection.isConnected()) {
            execute(project, connection, text, context);
        }
    }

    protected String getExecutionContent(DataContext context) {
        final Editor editor = CommonDataKeys.EDITOR.getData(context);
        if (editor == null) {
            return null;
        }

        final TextRange range = getExecutionRange(editor, context);
        if (range != null && !range.isEmpty()) {
            return editor.getDocument().getText(range);
        }
        return null;
    }

    protected TextRange getExecutionRange(Editor editor, DataContext context) {
        if (editor == null) {
            return null;
        }

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

    protected void execute(Project project, InstanceConnection connection, String text, DataContext context) {
        KdbConsoleToolWindow.getInstance(project).execute(connection, text);
    }
}
