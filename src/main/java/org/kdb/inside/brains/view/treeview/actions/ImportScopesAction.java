package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.IoErrorText;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.KdbScopeHelper;
import org.kdb.inside.brains.core.ScopeType;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ImportScopesAction extends DumbAwareAction {
    private static final Logger log = Logger.getInstance(ImportScopesAction.class);
    private final Consumer<KdbScope> scopeConsumer;
    private final Supplier<Collection<String>> namesSupplier;

    public ImportScopesAction(String text, String description, Consumer<KdbScope> scopeConsumer, Supplier<Collection<String>> namesSupplier) {
        this(text, description, true, scopeConsumer, namesSupplier);
    }

    public ImportScopesAction(String text, String description, boolean icon, Consumer<KdbScope> scopeConsumer, Supplier<Collection<String>> namesSupplier) {
        super(text, description, icon ? AllIcons.ToolbarDecorator.Import : null);
        this.scopeConsumer = scopeConsumer;
        this.namesSupplier = namesSupplier;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        performImport(e);
    }

    public void performImport(AnActionEvent e) {
        final Project project = e.getProject();

        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, true)
                .withFileFilter(f -> "xml".equalsIgnoreCase(f.getExtension()));
        final FileChooserDialog chooserDialog = FileChooserFactory.getInstance().createFileChooser(descriptor, project, null);

        final VirtualFile[] choose = chooserDialog.choose(project);
        for (VirtualFile file : choose) {
            // String that contains XML
            try {
                final Element rootNode = JDOMUtil.load(file.getInputStream());

                final List<KdbScope> kdbScopes = new KdbScopeHelper().readScopes(rootNode, ScopeType.LOCAL);

                final Collection<String> names = namesSupplier.get();
                for (KdbScope scope : kdbScopes) {
                    while (names.contains(scope.getName())) {
                        scope.setName(scope.getName() + " (Imported)");
                    }
                    scopeConsumer.accept(scope);
                }
            } catch (Exception ex) {
                log.error("Scopes can't be imported from: " + file, ex);
                Messages.showErrorDialog(project, IoErrorText.message(ex), "Scopes Can be Imported: " + file.getName());
            }
        }
    }
}
