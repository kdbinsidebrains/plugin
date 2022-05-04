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
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
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
    private TableResult tableResult;
    private ToggleAction searchAction;

    private final JLabel statusTime = new JLabel();
    private final JLabel statusSize = new JLabel();
    private final JLabel statusQuery = new JLabel();

    private final Project project;
    private final KdbOutputFormatter formatter;
    private final BiConsumer<KdbQuery, TableResultView> repeater;

    private final JBTable myTable;
    private final TableResultSearchSession searchSession;

    public TableResultView(Project project, KdbOutputFormatter formatter, BiConsumer<KdbQuery, TableResultView> repeater) {
        this.project = project;
        this.formatter = formatter;
        this.repeater = repeater;

        final var options = KdbSettingsService.getInstance().getConsoleOptions();
        final var valueColumnRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (options.isStriped() && row % 2 == 0) {
                        c.setBackground(UIUtil.getDecoratedRowColor());
                    } else {
                        c.setBackground(table.getBackground());
                    }
                }
                return c;
            }

            @Override
            protected void setValue(Object value) {
                setText(valueToString(value));
            }

            private String valueToString(Object value) {
                if (value instanceof String && !options.isPrefixSymbols()) {
                    return String.valueOf(value);
                } else if (value instanceof char[] && !options.isWrapStrings()) {
                    return new String((char[]) value);
                } else if (value instanceof Character && !options.isWrapStrings()) {
                    return String.valueOf(value);
                }
                return formatter.objectToString(value);
            }
        };

        myTable = new JBTable(TableResult.EMPTY_MODEL) {
            @Override
            public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
                final Component c = super.prepareRenderer(renderer, row, column);

                final TableColumn tableColumn = getColumnModel().getColumn(column);
                tableColumn.setPreferredWidth(max(c.getPreferredSize().width + getIntercellSpacing().width + 10, tableColumn.getPreferredWidth()));

                final boolean keyColumn = getModel().isKeyColumn(column);
                if (keyColumn) {
                    c.setBackground(UIUtils.getKeyColumnColor(c.getBackground()));
                }
                return c;
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

        myTable.setStriped(false);
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
        searchSession.getFindModel().addObserver(this::modelBeenUpdated);

        createSearchComponent();

        final ActionGroup contextMenu = createContextMenu();
        PopupHandler.installPopupHandler(myTable, contextMenu, "TableResultView.Context");

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.Toolbar", contextMenu, false);
        actionToolbar.setTargetComponent(this);

        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable, true);

        setLayout(new BorderLayout());
        add(searchSession.getComponent(), BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        add(actionToolbar.getComponent(), BorderLayout.WEST);
    }

    @NotNull
    private JPanel createStatusBar() {
        final GridBag c = new GridBag()
                .setDefaultAnchor(0, GridBagConstraints.LINE_START)
                .setDefaultWeightX(0, 1)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(3, 10, 3, 3);


        final JPanel statusBar = new JPanel(new GridBagLayout());
        statusBar.add(statusQuery, c.next());
        statusBar.add(statusTime, c.next());
        statusBar.add(statusSize, c.next());
        return statusBar;
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

        group.add(new ShowChartAction("Show chart", "Open current table in Excel or compatible application", () -> ChartDataProvider.copy(myTable)));

        return group;
    }

    public TableResult getTableResult() {
        return tableResult;
    }

    public JComponent getFocusableComponent() {
        return myTable;
    }

    static TableRowFilter createFilter(FindModel model) {
        final String text = model.getStringToFind();
        if (text.isBlank()) {
            return null;
        }
        return createSimpleFilter(model);
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

    @NotNull
    static TableRowFilter createSimpleFilter(FindModel model) {
        final String text = model.getStringToFind();
        if (model.isRegularExpressions()) {
            final Matcher matcher = model.compileRegExp().matcher("");
            return new TableRowFilter(v -> matcher.reset(v).find());
        } else if (model.isWholeWordsOnly()) {
            final int flags = model.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
            final Pattern compile = Pattern.compile(".*\\b" + text + "\\b.*", flags);
            final Matcher matcher = compile.matcher("");
            return new TableRowFilter(v -> matcher.reset(v).find());
        } else {
            return new TableRowFilter(v -> containsIgnoreCase(v, text, !model.isCaseSensitive()));
        }
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

    public void showResult(TableResult tableResult) {
        this.tableResult = tableResult;

        if (tableResult == null) {
            myTable.setModel(TableResult.EMPTY_MODEL);
            statusTime.setText("");
            statusSize.setText("Empty");
            statusQuery.setText("");
            searchSession.setDelaySearchEnabled(false);
        } else {
            final TableResult.QTableModel tableModel = tableResult.getTableModel();
            myTable.setModel(tableModel);

            final KdbResult result = tableResult.getResult();

            final double v = result.getRoundtripMillis() / 1000d;
            final double v1 = ((int) (v * 100)) / 100d;
            statusTime.setText(formatter.formatTimestamp(new Timestamp(result.getFinishedMillis())) + " (" + v1 + "sec)");
            statusQuery.setText(tableResult.getQuery().getExpression());
            // 10_000 rows by 20 columns can be sorted fast. No reason for delay
            searchSession.setDelaySearchEnabled(tableModel.getRowCount() * tableModel.getColumnCount() > 200_000);

            updateSizeStatus();
            updateHeaderWidth();
            modelBeenUpdated(searchSession.getFindModel());
        }
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

    private void modelBeenUpdated(FindModel findModel) {
        final RowFilter<TableModel, Integer> filter = createFilter(findModel);
        ((TableRowSorter<? extends TableModel>) myTable.getRowSorter()).setRowFilter(filter);
        updateSizeStatus();
    }

    static class TableRowFilter extends RowFilter<TableModel, Integer> {
        private final ValueFilter filter;

        public TableRowFilter(ValueFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> value) {
            final TableModel model = value.getModel();
//            final String columnName = model.getColumnName();

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
