package org.kdb.inside.brains.core.credentials.plugin;

import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTypeDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AddEditRemovePanel;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CredentialPluginsPanel extends AddEditRemovePanel<CredentialPluginsPanel.Item> {
    public CredentialPluginsPanel() {
        super(new TheTableModel(), new ArrayList<>());
    }

    @Override
    protected JBTable createTable() {
        final JBTable jbTable = new JBTable() {
            @Override
            public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
                final Component component = super.prepareRenderer(renderer, row, column);
                final Item item = getData().get(row);
                if (item.error) {
                    component.setBackground(JBColor.RED);
                }
                return component;
            }
        };
        jbTable.setShowColumns(true);
        return jbTable;
    }

    @Override
    protected @Nullable CredentialPluginsPanel.Item addItem() {
        return editItem(null);
    }

    @Override
    protected @Nullable CredentialPluginsPanel.Item editItem(Item o) {
        final FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(new FileTypeDescriptor("Plugin Jar File", "jar"), null, this);
        final VirtualFile[] choose = fileChooser.choose(null, o == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{VfsUtil.findFileByURL(o.resource)});
        if (choose.length == 0) {
            return null;
        }

        final VirtualFile virtualFile = choose[0];
        final URL url = VfsUtilCore.convertToURL(virtualFile.getUrl());
        if (url == null) {
            return null;
        }

        try {
/*
            CredentialPlugin.load(url).destroy();
            final Path configDir = PathManager.getConfigDir().resolve("KdbInsideBrains").resolve("credentials");
            if (Files.notExists(configDir)) {
                Files.createDirectories(configDir);
            }
            Files.copy(url.openStream(), configDir);
*/
//            return new Item(CredentialPlugin.load(configDir.toUri().toURL()));
            return new Item(CredentialPlugin.load(url));
        } catch (Exception ex) {
            Messages.showErrorDialog(this, ex.getMessage(), "Incorrect Plugin Resource");
        }
        return null;
    }

    @Override
    protected boolean removeItem(Item o) {
        return true;
    }

    @Override
    public boolean isUpDownSupported() {
        return true;
    }

    public List<URL> getCredentialPlugin() {
        return getData().stream().map(i -> i.resource).collect(Collectors.toList());
    }

    public void setCredentialPlugins(List<URL> credentialPlugins) {
        List<Item> items = new ArrayList<>();
        for (URL url : credentialPlugins) {
            try {
                items.add(new Item(CredentialPlugin.load(url)));
            } catch (Exception ex) {
                items.add(new Item(url, ex));
            }
        }
        setData(items);
    }

    private static class TheTableModel extends TableModel<Item> {
        private final String[] myNames = {"Name", "Version", "Class or Error"};

        @Override
        public int getColumnCount() {
            return myNames.length;
        }

        @Override
        public Object getField(Item o, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return o.name;
                case 1:
                    return o.version;
                case 2:
                    return o.description;
            }
            return "";
        }

        @Override
        public String getColumnName(int column) {
            return myNames[column];
        }
    }

    protected static class Item {
        String name;
        String version;
        String description;
        URL resource;
        boolean error;

        public Item(URL resource, Exception ex) {
            name = ex.getClass().getSimpleName();
            version = "error";
            description = ex.getMessage();
            this.resource = resource;
            error = true;
        }

        public Item(CredentialPlugin plugin) {
            this.name = plugin.getName();
            this.version = plugin.getVersion();
            this.description = plugin.getProvider().getClass().getName();
            this.resource = plugin.getResource();
            this.error = false;
        }
    }
}