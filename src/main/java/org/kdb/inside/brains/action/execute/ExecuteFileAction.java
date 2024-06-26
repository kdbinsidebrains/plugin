package org.kdb.inside.brains.action.execute;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.core.InstanceConnection;

public class ExecuteFileAction extends ExecuteAction {
    public void update(@NotNull AnActionEvent e) {
        final PsiFile file = CommonDataKeys.PSI_FILE.getData(e.getDataContext());

        final Presentation presentation = e.getPresentation();
        presentation.setVisible(QFileType.is(file));

        final InstanceConnection c = getConnection(e.getProject());
        presentation.setEnabled(c != null && c.isConnected());

        if (file != null) {
            presentation.setText("Execute '" + file.getName() + "'");
        }
    }

    @Override
    protected String getExecutionContent(DataContext context) {
        final PsiFile data = CommonDataKeys.PSI_FILE.getData(context);
        if (!QFileType.is(data)) {
            return null;
        }
        try {

            final String text = data.getText();
            // end of the file comment that we should exclude as it's parsed by KDB
            final int i = getEndOfFileIndex(text);
            return i < 0 ? text : text.substring(0, i);
        } catch (Exception ex) {
            return null;
        }
    }

    private int getEndOfFileIndex(String text) {
        final int i = text.indexOf("\\\r");
        if (i < 0) {
            return text.indexOf("\\\n");
        }
        return i;
    }
}
