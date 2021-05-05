package org.kdb.inside.brains.view.console;

import com.intellij.find.SearchReplaceComponent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.export.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.function.BiConsumer;

import static java.lang.Math.max;

public class TableResultView extends NonOpaquePanel implements DataProvider {
    private JBTable myTable;

    private TableResult tableResult;

    private ToggleAction searchAction;
    private SearchReplaceComponent searchComponent;

    private final JLabel statusTime = new JLabel();
    private final JLabel statusSize = new JLabel();
    private final JLabel statusQuery = new JLabel();

    private final Project project;
    private final ConsoleOptions options;
    private final KdbOutputFormatter formatter;
    private final BiConsumer<KdbQuery, TableResultView> repeater;

    public static final DataKey<TableResultView> DATA_KEY = DataKey.create("KdbConsole.TableResultView");

    public TableResultView(Project project, KdbOutputFormatter formatter, BiConsumer<KdbQuery, TableResultView> repeater) {
        this.project = project;
        this.formatter = formatter;
        this.repeater = repeater;
        this.options = KdbSettingsService.getInstance().getConsoleOptions();
        init(project);
    }

    private void init(Project project) {
        final DefaultTableCellRenderer valueColumnRenderer = new QTableCellRenderer();

        myTable = new JBTable(TableResult.EMPTY_MODEL) {
            @Override
            public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
                final Component c = super.prepareRenderer(renderer, row, column);
                final TableColumn tableColumn = getColumnModel().getColumn(column);
                tableColumn.setPreferredWidth(max(c.getPreferredSize().width + getIntercellSpacing().width + 10, tableColumn.getPreferredWidth()));

                if (getModel().isKeyColumn(column)) {
                    c.setBackground(UIUtils.getKeyColumnColor(c.getBackground()));
                }
                return c;
            }

            @Override
            protected void createDefaultRenderers() {
                defaultRenderersByColumnClass = new UIDefaults(8, 0.75f);
            }

            @Override
            protected void createDefaultEditors() {
                defaultEditorsByColumnClass = new UIDefaults(3, 0.75f);
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return valueColumnRenderer;
            }

            @Override
            public TableResult.QTableModel getModel() {
                return (TableResult.QTableModel) super.getModel();
            }

            @Override
            public void setModel(@NotNull TableModel model) {
                super.setModel(model);

                if (model instanceof TableResult.QTableModel) {
                    final TableResult.QTableModel qModel = (TableResult.QTableModel) model;

                    final TableRowSorter<TableResult.QTableModel> sorter = new TableRowSorter<>(qModel);
                    for (int i = 0; i < qModel.columns.length; i++) {
                        final TableResult.QColumnInfo column = qModel.columns[i];
                        final Comparator<Object> comparator = column.getComparator();
                        if (comparator != null) {
                            sorter.setComparator(i, comparator);
                        }
                    }
                    setRowSorter(sorter);
                }
            }
        };

        myTable.setStriped(options.isStriped());
        myTable.setShowGrid(options.isShowGrid());

        myTable.setShowColumns(true);
        myTable.setColumnSelectionAllowed(true);

        myTable.setAutoCreateRowSorter(false);
        myTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Disable original handler
        myTable.setTransferHandler(new TransferHandler() {
            @Override
            public int getSourceActions(JComponent c) {
                return NONE;
            }
        });

        myTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    new ClipboardExportAction(null, ExportingType.SELECTION).performExport(project, TableResultView.this);
                } else {
                    super.mouseReleased(e);
                }
            }
        });

        final JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightStatus.add(statusTime);
        rightStatus.add(Box.createVerticalStrut(1));
        rightStatus.add(statusSize);

        final JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.add(statusQuery, BorderLayout.WEST);
        statusBar.add(rightStatus, BorderLayout.EAST);

        createSearchComponent(project);
        PopupHandler.installPopupHandler(myTable, createContextMenu(), "TableResultView.Context");

        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable, true);
//        scrollPane.setRowHeaderView();

        setLayout(new BorderLayout());
        add(searchComponent, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    public ActionGroup createContextMenu() {
        final DefaultActionGroup group = new DefaultActionGroup();

        if (repeater != null) {

            group.add(new AnAction("_Repeat the Query", "Re-run the query related with this result", AllIcons.Actions.Refresh) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    repeater.accept(tableResult.getQuery(), TableResultView.this);
                }
            });
            group.addSeparator();
        }

        group.add(searchAction);
        group.addSeparator();

        final DefaultActionGroup copyGroup = new DefaultActionGroup("Copy Special", true);
        copyGroup.add(new ClipboardExportAction("Copy Only Rows", "Copy the whole row values", ExportingType.ROWS));

        copyGroup.add(new ClipboardExportAction("Copy Rows with Header", "Copy the whole row values including column names", ExportingType.ROWS_WITH_HEADER));
        copyGroup.addSeparator();
        copyGroup.add(new ClipboardExportAction("Copy Only Columns", "Copy the whole columns values", ExportingType.COLUMNS));
        copyGroup.add(new ClipboardExportAction("Copy Columns with Header", "Copy the whole columns values including column names", ExportingType.COLUMNS_WITH_HEADER));

        final ClipboardExportAction copy_header = new ClipboardExportAction("_Copy", "Copy selected cells into the clipboard", AllIcons.Actions.Copy, ExportingType.SELECTION_WITH_HEADER);
        copy_header.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, myTable);

        final ClipboardExportAction copy_values = new ClipboardExportAction("Copy _Values", "Copy selected cells into the clipboard", null, ExportingType.SELECTION);
        copy_values.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, myTable);

        group.add(copy_header);
        group.add(copy_values);
        group.add(copyGroup);

        group.addSeparator();

        group.add(new ExcelExportAction("Open in _Excel", "Open curent table in Excel or compatible application", ExportingType.ALL_WITH_HEADER, false));

        group.addSeparator();

        final DefaultActionGroup exportGroup = new DefaultActionGroup("Export Data _Into ...", true);
        exportGroup.add(new CsvExportAction("CSV format", "Export current table into Comma Separated File format", ExportingType.ALL_WITH_HEADER));
        exportGroup.add(new ExcelExportAction("Excel xls format", "Export current table into Excel XLS format", ExportingType.ALL_WITH_HEADER, true));
        exportGroup.add(new BinaryExportAction("KDB binary format", "Binary KDB IPC file format. Can be imported directly into KDB.", ExportingType.ALL_WITH_HEADER));
        group.add(exportGroup);

        group.addSeparator();

        final ActionGroup sendTo = new ActionGroup("_Send Data Into ...", true) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return KdbConnectionManager.getManager(project).getConnections().stream().map(SendIntoAction::new).toArray(AnAction[]::new);
            }
        };
        group.add(sendTo);
        return group;
    }

    public JBTable getTable() {
        return myTable;
    }

    public TableResult getTableResult() {
        return tableResult;
    }

    public JComponent getFocusableComponent() {
        return myTable;
    }

    public void showResult(TableResult tableResult) {
        this.tableResult = tableResult;

        if (tableResult == null) {
            myTable.setModel(TableResult.EMPTY_MODEL);
            statusTime.setText("");
            statusSize.setText("Empty");
            statusQuery.setText("");
        } else {
            myTable.setModel(tableResult.getTableModel());

            final KdbResult result = tableResult.getResult();

            final double v = result.getRoundtripMillis() / 1000d;
            final double v1 = ((int) (v * 100)) / 100d;
            statusTime.setText(formatter.formatTimestamp(new Timestamp(result.getFinishedMillis())) + " (" + v1 + "sec)");
            statusQuery.setText(tableResult.getQuery().getExpression());

            resorting();
            updateSizeStatus();
            updateHeaderWidth();
        }
    }

    private void createSearchComponent(Project project) {
/*
        final AnAction a1 = new AnAction("a1", "asdasd", KdbIcons.Console.Kill) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };

        final AnAction a2 = new AnAction("a2", "asdasd", KdbIcons.Console.ExportExcel) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };
        final AnAction a3 = new AnAction("a3", "asdasd", KdbIcons.Console.Table) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };
        final AnAction a4 = new AnAction("a4", "asdasd", KdbIcons.Console.SaveCSV) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };
*/

        Runnable closeSearchAction = () -> {
            searchComponent.getSearchTextComponent().setText("");
            searchComponent.setVisible(false);
        };

        searchComponent = SearchReplaceComponent
                .buildFor(project, myTable)
/*
                .addSearchFieldActions(a1)
                .addPrimarySearchActions(a2)
                .addSecondarySearchActions(a3)
                .addExtraSearchActions(a4)
*/
                .withCloseAction(closeSearchAction)
                .build();
        searchComponent.setVisible(false);

        searchComponent.update("", "", false, false);

        searchComponent.addListener(new SearchReplaceComponent.Listener() {
            @Override
            public void searchFieldDocumentChanged() {
                resorting();
            }

            @Override
            public void replaceFieldDocumentChanged() {
            }

            @Override
            public void multilineStateChanged() {
            }
        });

        searchAction = new ToggleAction("_Search", "Search data in the table", AllIcons.Actions.Search) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return searchComponent.isVisible();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) {
                    searchComponent.setVisible(true);
                    searchComponent.getSearchTextComponent().requestFocusInWindow();
                } else {
                    closeSearchAction.run();
                }
            }
        };
        searchAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)), myTable);
    }

    private void resorting() {
        final String text = searchComponent.getSearchTextComponent().getText();
        final TableRowSorter<? extends TableModel> rowSorter = (TableRowSorter<? extends TableModel>) myTable.getRowSorter();
        if (text.isBlank()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter(text));
        }
        updateSizeStatus();
    }

    private void updateSizeStatus() {
        statusSize.setText(myTable.getRowCount() + " of " + myTable.getModel().getRowCount() + " rows");
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DATA_KEY.getName().equals(dataId)) {
            return this;
        }
        return null;
    }

    private void updateHeaderWidth() {
        final TableCellRenderer renderer = myTable.getTableHeader().getDefaultRenderer();
        final TableColumnModel columnModel = myTable.getColumnModel();
        for (int i = 0; i < myTable.getColumnCount(); ++i) {
            columnModel.getColumn(i).setPreferredWidth(renderer.getTableCellRendererComponent(myTable, myTable.getModel().getColumnName(i), false, false, 0, i).getPreferredSize().width);
        }
    }

    private class QTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        protected void setValue(Object value) {
            final String text;
            if (value instanceof String && !options.isPrefixSymbols()) {
                text = String.valueOf(value);
            } else if (value instanceof char[] && !options.isWrapStrings()) {
                text = new String((char[]) value);
            } else if (value instanceof Character && !options.isWrapStrings()) {
                text = String.valueOf(value);
            } else {
                text = formatter.convertObject(value);
            }
            setText(text);
        }
    }
}
