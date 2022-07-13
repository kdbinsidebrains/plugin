package org.kdb.inside.brains.core.credentials;

import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTypeDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.StatusText;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CredentialPluginsPanel extends JPanel implements ComponentWithEmptyText {
    private static final String[] COLUMN_NAMES = {"Name", "Version", "Description"};
    private final CredentialService credentialService;
    private final List<CredentialPlugin> plugins = new ArrayList<>();
    private JBTable myTable;
    private AbstractTableModel myTableModel;

    public CredentialPluginsPanel() {
        credentialService = CredentialService.getInstance();
        initTable();
        initPanel();
    }

    private void initTable() {
        myTableModel = new AbstractTableModel() {
            @Override
            public int getColumnCount() {
                return COLUMN_NAMES.length;
            }

            @Override
            public int getRowCount() {
                return plugins.size();
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public String getColumnName(int column) {
                return COLUMN_NAMES[column];
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                final CredentialPlugin plugin = plugins.get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        return plugin.getName();
                    case 1:
                        return plugin.getVersion();
                    case 2:
                        return plugin.getDescription();
                }
                return "";
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        myTable = new JBTable();
        myTable.setModel(myTableModel);
        myTable.setShowColumns(false);
        myTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

//        ScrollPaneFactory.createScrollPane(myTable);
    }

    protected void initPanel() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myTable)
                .setAddAction(button -> doAdd())
                .setRemoveAction(button -> doRemove())
                .setMoveUpAction(button -> doUp())
                .setMoveDownAction(button -> doDown());

        setLayout(new BorderLayout());
        add(decorator.createPanel(), BorderLayout.CENTER);
    }

    private void doAdd() {
        importPlugin();
    }

    private void doRemove() {
        final int[] selected = myTable.getSelectedRows();
        if (selected == null || selected.length == 0) return;

        Arrays.sort(selected);

        for (int i = selected.length - 1; i >= 0; i--) {
            int idx = selected[i];
            plugins.remove(idx);
        }

        myTableModel.fireTableDataChanged();

        int selection = selected[0];
        if (selection >= plugins.size()) {
            selection = plugins.size() - 1;
        }
        if (selection >= 0) {
            myTable.setRowSelectionInterval(selection, selection);
        }
    }

    private void doUp() {
        TableUtil.moveSelectedItemsUp(myTable);
    }

    private void doDown() {
        TableUtil.moveSelectedItemsDown(myTable);
    }

    @NotNull
    @Override
    public StatusText getEmptyText() {
        return myTable.getEmptyText();
    }

    private void importPlugin() {
        final FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(new FileTypeDescriptor("Plugin Jar File", "jar"), null, this);
        final VirtualFile[] choose = fileChooser.choose(null, VirtualFile.EMPTY_ARRAY);
        if (choose.length == 0) {
            return;
        }

        final VirtualFile virtualFile = choose[0];
        final URL url = VfsUtilCore.convertToURL(virtualFile.getUrl());
        if (url == null) {
            return;
        }

        try {
            final CredentialPlugin plugin = credentialService.verifyPlugin(url);

            final int index = findPlugin(plugin.getId());
            if (index < 0) {
                plugins.add(plugin);
                myTableModel.fireTableRowsInserted(plugins.size() - 1, plugins.size() - 1);
            } else {
                plugins.set(index, plugin);
                myTableModel.fireTableRowsUpdated(index, index);
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(this, ex.getMessage(), "Incorrect Plugin Resource");
        }
    }

    private int findPlugin(String id) {
        int i = 0;
        for (CredentialPlugin plugin : plugins) {
            if (id.equals(plugin.getId())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public List<CredentialPlugin> getCredentialPlugins() {
        return new ArrayList<>(plugins);
    }

    public void setCredentialPlugins(List<CredentialPlugin> credentialPlugins) {
        plugins.clear();
        plugins.addAll(credentialPlugins);
        myTableModel.fireTableDataChanged();
    }
}