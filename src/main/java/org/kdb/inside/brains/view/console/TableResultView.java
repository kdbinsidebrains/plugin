package org.kdb.inside.brains.view.console;

import com.intellij.find.FindModel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ShowChartAction;
import org.kdb.inside.brains.view.export.ClipboardExportAction;
import org.kdb.inside.brains.view.export.ExportDataProvider;
import org.kdb.inside.brains.view.export.ExportingType;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;

public class TableResultView extends NonOpaquePanel implements DataProvider, ExportDataProvider {
    private final JBTable myTable;
    private final TableResultSearchSession searchSession;

    private TableResult tableResult;
    private ToggleAction searchAction;

    private final JLabel statusTime = new JLabel();
    private final JLabel statusSize = new JLabel();
    private final JLabel statusQuery = new JLabel();

    private final Project project;
    private final ConsoleOptions options;
    private final KdbOutputFormatter formatter;
    private final BiConsumer<KdbQuery, TableResultView> repeater;


    public TableResultView(Project project, KdbOutputFormatter formatter, BiConsumer<KdbQuery, TableResultView> repeater) {
        this.project = project;
        this.formatter = formatter;
        this.repeater = repeater;
        this.options = KdbSettingsService.getInstance().getConsoleOptions();

        final DefaultTableCellRenderer valueColumnRenderer = formatter.createCellRenderer();

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
                    sorter.setStringConverter(new TableStringConverter() {
                        @Override
                        public String toString(TableModel model, int row, int column) {
                            final Object valueAt = model.getValueAt(row, column);
                            if (valueAt == null) {
                                return "";
                            }
                            return formatter.objectToString(valueAt);
                        }
                    });
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
                    new ClipboardExportAction(null, ExportingType.SELECTION, TableResultView.this).performExport(project, TableResultView.this);
                } else {
                    super.mouseReleased(e);
                }
            }
        });

        searchSession = new TableResultSearchSession(myTable, project, new FindModel());
        searchSession.getFindModel().addObserver(model -> resorting());

        final JPanel rightStatus = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rightStatus.add(statusTime);
        rightStatus.add(Box.createVerticalStrut(1));
        rightStatus.add(statusSize);

        final JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.add(statusQuery, BorderLayout.WEST);
        statusBar.add(rightStatus, BorderLayout.EAST);

        createSearchComponent();

        final ActionGroup contextMenu = createContextMenu();
        PopupHandler.installPopupHandler(myTable, contextMenu, "TableResultView.Context");

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.Toolbar", contextMenu, false);
        actionToolbar.setTargetComponent(this);

        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable, true);

        setLayout(new BorderLayout());
        add(searchSession.getComponent(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
        add(actionToolbar.getComponent(), BorderLayout.WEST);
    }

    private static boolean containsIgnoreCase(String str, String searchStr, boolean ignoreCase) {
        if (str == null || searchStr == null) {
            return false;
        }

        final int length = searchStr.length();
        if (length == 0) {
            return true;
        }

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(ignoreCase, i, searchStr, 0, length)) {
                return true;
            }
        }
        return false;
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

        group.addAll(ExportDataProvider.createActionGroup(project, this));

        group.addSeparator();

        group.add(new ShowChartAction("Show chart", "Open current table in Excel or compatible application", () -> ChartDataProvider.tableCopy(myTable)));

        return group;
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

    @Override
    public JBTable getTable() {
        return myTable;
    }

    private void createSearchComponent() {
        searchAction = new ToggleAction("_Search", "Search data in the table", AllIcons.Actions.Search) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return searchSession.isOpened();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) {
                    searchSession.open();
                } else {
                    searchSession.close();
                }
            }
        };
        searchAction.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)), myTable);
    }

    private void resorting() {
        final FindModel findModel = searchSession.getFindModel();

        final String text = findModel.getStringToFind();
        final TableRowSorter<? extends TableModel> rowSorter = (TableRowSorter<? extends TableModel>) myTable.getRowSorter();

        final RowFilter<TableModel, Integer> filter;
        if (text.isBlank()) {
            filter = null;
        } else {
            if (findModel.isRegularExpressions()) {
                final Matcher matcher = findModel.compileRegExp().matcher("");
                filter = new TableRowFilter(v -> matcher.reset(v).find());
            } else if (findModel.isWholeWordsOnly()) {
                final int flags = findModel.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
                final Pattern compile = Pattern.compile(".*\\b" + text + "\\b.*", flags);
                final Matcher matcher = compile.matcher("");
                filter = new TableRowFilter(v -> matcher.reset(v).find());
            } else {
                filter = new TableRowFilter(v -> containsIgnoreCase(v, text, !findModel.isCaseSensitive()));
            }
        }
        rowSorter.setRowFilter(filter);
        updateSizeStatus();
    }

    private void updateSizeStatus() {
        statusSize.setText(myTable.getRowCount() + " of " + myTable.getModel().getRowCount() + " rows");
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            return this;
        }
        return null;
    }

    @Override
    public String getExportName() {
        final Container parent = getParent();
        if (parent instanceof JBTabs) {
            final JBTabs jbTabs = (JBTabs) parent;
            for (TabInfo info : jbTabs.getTabs()) {
                if (info.getObject() == this) {
                    return info.getText();
                }
            }
        }
        return "Table Result";
    }

    @Override
    public Object getNativeObject() {
        return tableResult.getResult().getObject();
    }

    private void updateHeaderWidth() {
        final TableCellRenderer renderer = myTable.getTableHeader().getDefaultRenderer();
        final TableColumnModel columnModel = myTable.getColumnModel();
        for (int i = 0; i < myTable.getColumnCount(); ++i) {
            columnModel.getColumn(i).setPreferredWidth(renderer.getTableCellRendererComponent(myTable, myTable.getModel().getColumnName(i), false, false, 0, i).getPreferredSize().width);
        }
    }

    @FunctionalInterface
    private interface ValueFilter {
        boolean check(String value);
    }

    private static class TableRowFilter extends RowFilter<TableModel, Integer> {
        private final ValueFilter filter;

        public TableRowFilter(ValueFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> value) {
            int index = value.getValueCount();
            while (--index >= 0) {
                final String stringValue = value.getStringValue(index);
                if (filter.check(stringValue)) {
                    return true;
                }
            }
            return false;
        }
    }
}
