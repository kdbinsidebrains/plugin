package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.util.ui.IoErrorText;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.KdbScopeHelper;

import java.io.FileWriter;
import java.util.List;
import java.util.function.Supplier;

public class ExportScopesAction extends DumbAwareAction {
    private static final Logger log = Logger.getInstance(ExportScopesAction.class);
    private final Supplier<List<KdbScope>> scopesConsumer;

    public ExportScopesAction(String text, String description, Supplier<List<KdbScope>> scopesConsumer) {
        this(text, description, true, scopesConsumer);
    }

    public ExportScopesAction(String text, String description, boolean icon, Supplier<List<KdbScope>> scopesConsumer) {
        super(text, description, icon ? AllIcons.ToolbarDecorator.Export : null);
        this.scopesConsumer = scopesConsumer;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final List<KdbScope> scopes = scopesConsumer.get();
        final Presentation presentation = e.getPresentation();
        presentation.setEnabled(scopes != null && !scopes.isEmpty());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final List<KdbScope> scopes = scopesConsumer.get();
        if (scopes == null || scopes.isEmpty()) {
            return;
        }

        final Project project = e.getProject();

        final FileSaverDescriptor descriptor = new FileSaverDescriptor("Export Kdb Scopes", "Exporting " + scopes.size() + " scope(s) into external xml file", "xml");
        final FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);

        final VirtualFileWrapper file = dialog.save("KdbScopes");
        if (file == null) {
            return;
        }

        final MessageDialogBuilder.YesNo credentials = MessageDialogBuilder.yesNo("Export Credentials?", "Your credentials will be included in the exported file");
        final boolean ask = credentials.ask(project);

        final Element element = new KdbScopeHelper().writeScopes(scopes, ask);

        try (final FileWriter writer = new FileWriter(file.getFile())) {
            JDOMUtil.write(element, writer, "\n");
        } catch (Exception ex) {
            log.error("Scopes can't be exported to: " + file, ex);
            Messages.showErrorDialog(project, IoErrorText.message(ex), "Scopes Exporting Error");
        }
    }
}
