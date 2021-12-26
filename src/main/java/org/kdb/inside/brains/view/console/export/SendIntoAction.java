package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.console.TableResultView;

public class SendIntoAction extends AnExportAction<String> {
    private final InstanceConnection connection;

    public SendIntoAction(TableResultView resultView, InstanceConnection connection) {
        super(connection.getCanonicalName() + " (" + connection.getDetails() + ")", ExportingType.ALL_WITH_HEADER, resultView, null, null);
        this.connection = connection;
    }

    @Override
    protected boolean isCancelable() {
        return false;
    }

    @Override
    protected String getExportConfig(Project project, TableResultView view) {
        final String a = "." + System.getProperty("user.name") + ".import";
        return Messages.showInputDialog(project, "New variable name", "Variable Name", null, a, null, new TextRange(0, a.length()));
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, TableResultView view, String name, @NotNull ProgressIndicator indicator) throws Exception {
        final KdbConnectionManager instance = KdbConnectionManager.getManager(project);
        if (instance == null) {
            return;
        }
        final Object obj = view.getTableResult().getResult().getObject();
        if (obj == null) {
            return;
        }

        indicator.setIndeterminate(true);

        if (connection.getState() != InstanceState.CONNECTED) {
            connection.connectAndWait();
        }

        if (connection.getState() == InstanceState.CONNECTED) {
            final KdbResult res = connection.query(new KdbQuery("set", name, obj));
            if (res.isError()) {
                throw (Exception) res.getObject();
            }
        }
    }
}
