package org.kdb.inside.brains.core.credentials;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ComponentWithEmptyText;
import com.intellij.util.ui.IoErrorText;
import com.intellij.util.ui.StatusText;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CredentialPluginsPanel extends JPanel implements ComponentWithEmptyText {
    private JBTable myTable;
    private AbstractTableModel myTableModel;

    private static final String[] COLUMN_NAMES = {"Name", "Version", "Description"};
    private final List<PluginRef> plugins = new ArrayList<>();

    public CredentialPluginsPanel() {
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
                final PluginDescriptor plugin = plugins.get(rowIndex).descriptor;
                return switch (columnIndex) {
                    case 0 -> plugin.name();
                    case 1 -> plugin.version();
                    case 2 -> plugin.description();
                    default -> "";
                };
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
            plugins.remove(selected[i]);
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
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, true, true, false, false)
                .withDescription("Plugin jar file")
                .withFileFilter(f -> "jar".equals(f.getExtension()));

        final FileChooserDialog fileChooser = FileChooserFactory.getInstance().createFileChooser(descriptor, null, this);
        final VirtualFile[] choose = fileChooser.choose(null, VirtualFile.EMPTY_ARRAY);
        if (choose.length == 0) {
            return;
        }

        final VirtualFile virtualFile = choose[0];
        if (!virtualFile.exists() || !virtualFile.isValid()) {
            return;
        }
        final Path resource = virtualFile.toNioPath();

        try {
            final PluginDescriptor d = CredentialService.verifyPlugin(resource);
            final PluginRef ref = new PluginRef(d, resource);
            final int index = findPlugin(d.id());
            if (index < 0) {
                plugins.add(ref);
                myTableModel.fireTableRowsInserted(plugins.size() - 1, plugins.size() - 1);
            } else {
                plugins.set(index, ref);
                myTableModel.fireTableRowsUpdated(index, index);
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(this, IoErrorText.message(ex), "Incorrect Plugin Resource");
        }
    }

    private int findPlugin(String id) {
        int i = 0;
        for (PluginRef plugin : plugins) {
            if (plugin.is(id)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public List<Path> getPluginResources() {
        return plugins.stream().map(PluginRef::resource).toList();
    }

    public void updateCredentialPlugins(List<CredentialPlugin> credentialPlugins) {
        plugins.clear();
        credentialPlugins.stream().map(PluginRef::new).forEachOrdered(plugins::add);
        myTableModel.fireTableDataChanged();
    }

    private record PluginRef(PluginDescriptor descriptor, Path resource) {
        private PluginRef(CredentialPlugin plugin) {
            this(plugin.getDescriptor(), plugin.getResource());
        }

        private boolean is(String id) {
            return id.equals(descriptor.id());
        }
    }
}