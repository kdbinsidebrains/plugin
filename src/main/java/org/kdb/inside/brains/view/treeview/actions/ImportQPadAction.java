package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.IoErrorText;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.InstanceOptions;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.StructuralItem;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

public class ImportQPadAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        e.getPresentation().setEnabled(item instanceof StructuralItem);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        if (!(item instanceof StructuralItem s)) {
            return;
        }

        final FileChooserDescriptor chooser = new FileChooserDescriptor(true, false, false, true, false, false)
                .withTitle("Select QPad Configuration")
                .withDescription("Select QPad Servers.cfg configuration file for importing tree structure.")
                .withFileFilter(file -> "Servers.cfg".equalsIgnoreCase(file.getName()));

        final InstancesScopeView instancesScopeView = getInstancesScopeView(e);
        final FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(chooser, e.getProject(), instancesScopeView);

        final VirtualFile[] choose = fileChooser.choose(e.getProject());
        if (choose.length == 0) {
            return;
        }

        try {
            final String text = VfsUtil.loadText(choose[0]);
            importData(text, s);
        } catch (Exception ex) {
            Messages.showErrorDialog(instancesScopeView, IoErrorText.message(ex), "QPad Configs Can't Be Imported");
        }
    }

    protected static void importData(String text, StructuralItem s) {
        final String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("`")) {
                line = line.substring(1);
            }
            final String[] split = line.split("`");
            if (split.length < 2) {
                continue;
            }

            StructuralItem parent = s;
            for (int i = 1; i < split.length - 1; i++) {
                final String name = split[i];
                InstanceItem p = parent.findByName(name);
                if (p == null) {
                    p = parent.createPackage(name);
                }
                parent = (StructuralItem) p;
            }

            final String name = split[split.length - 1];
            final KdbInstance inst = KdbInstance.parseInstance(split[0]);
            if (inst != null) {
                parent.createInstance(name, inst.getHost(), inst.getPort(), inst.getCredentials(), InstanceOptions.INHERITED);
            }
        }
    }
}
