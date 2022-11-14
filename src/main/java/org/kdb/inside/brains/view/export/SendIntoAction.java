package org.kdb.inside.brains.view.export;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.KdbOutputFormatter;

public class SendIntoAction extends AnExportAction<String> {
    private final InstanceConnection connection;

    public SendIntoAction(ExportDataProvider dataProvider, InstanceConnection connection) {
        super(connection.getCanonicalName() + " (" + connection.getDetails() + ")", ExportingType.ALL_WITH_HEADER, dataProvider, null, null);
        this.connection = connection;
    }

    @Override
    protected boolean isCancelable() {
        return false;
    }

    @Override
    protected String getExportConfig(Project project, ExportDataProvider view) {
        final String a = "." + System.getProperty("user.name") + ".import";
        return Messages.showInputDialog(project, "New variable name", "Variable Name", null, a, new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
                return QLanguage.isIdentifier(inputString);
            }

            @Override
            public boolean canClose(String inputString) {
                return checkInput(inputString);
            }
        }, new TextRange(0, a.length()));
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, String name, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception {
        final KdbConnectionManager instance = KdbConnectionManager.getManager(project);
        if (instance == null) {
            return;
        }
        final Object obj = dataProvider.getNativeObject();
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
