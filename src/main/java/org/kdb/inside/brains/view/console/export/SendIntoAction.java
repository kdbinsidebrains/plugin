package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.console.TableResultView;

public class SendIntoAction extends AnExportAction {
    private final InstanceConnection connection;

    public SendIntoAction(InstanceConnection connection) {
        super(connection.getName(), connection.toString(), null, ExportingType.ALL_WITH_HEADER);
        this.connection = connection;
    }

    @Override
    protected void performAction(Project project, TableResultView view, ExportingType type) throws Exception {
        final String a = "." + System.getProperty("user.name") + ".import";
        final String name = Messages.showInputDialog(project, "New variable name", "Variable Name", null, a, null, new TextRange(0, a.length()));
        if (name == null) {
            return;
        }

        final KdbConnectionManager instance = KdbConnectionManager.getManager(project);
        if (instance == null) {
            return;
        }
        final Object obj = view.getTableResult().getResult().getObject();
        if (obj == null) {
            return;
        }

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
