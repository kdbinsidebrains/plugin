package org.kdb.inside.brains.view.console.table;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.TabInfo;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.action.BgtToggleAction;
import org.kdb.inside.brains.action.EdtToggleAction;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.FormatterOptions;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.chart.ChartActionGroup;
import org.kdb.inside.brains.view.console.NumericalOptions;
import org.kdb.inside.brains.view.console.TableOptions;
import org.kdb.inside.brains.view.export.ClipboardExportAction;
import org.kdb.inside.brains.view.export.ExportDataProvider;
import org.kdb.inside.brains.view.export.ExportingType;
import org.kdb.inside.brains.view.export.OpenInEditorAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

public class TableResultView extends NonOpaquePanel implements DataProvider, ExportDataProvider {
    private TableResult tableResult;

    private final AnAction repeatAction;
    private final ToggleAction searchAction;
    private final ToggleAction filterAction;
    private final ToggleAction showIndexAction;
    private final ShowTableOptionAction showThousandsAction;
    private final ShowTableOptionAction showScientificAction;
    private final ActionGroup exportActionGroup;
    private final ChartActionGroup chartActionGroup;

    private final Project project;
    private final TableOptions tableOptions;

    private final QTable myTable;

    private final Splitter splitter;
    private final JScrollPane tableScroll;

    private final TableResultSearchSession searchSession;

    private final KdbOutputFormatter formatter;

    private QSchemaViewPanel schemaViewPanel;
    private TableResultStatusPanel statusBar;

    public static final DataKey<TableResultView> DATA_KEY = DataKey.create("KdbConsole.TableResultView");

    public TableResultView(Project project) {
        this(project, TableMode.NORMAL);
    }

    public TableResultView(Project project, TableMode mode) {
        this(project, mode, null);
    }

    public TableResultView(Project project, TableMode mode, BiConsumer<KdbQuery, TableResultView> repeater) {
        this.project = project;

        final KdbSettingsService settingsService = KdbSettingsService.getInstance();

        this.formatter = createOutputFormatter();
        this.tableOptions = settingsService.getTableOptions();

        myTable = new QTable(project, tableOptions, formatter);

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
                magicMouseActions(e);
            }
        });

        repeatAction = createRepeatAction(repeater);
        searchAction = createSearchAction();
        filterAction = createFilterAction();
        showIndexAction = createShowIndexAction();
        showScientificAction = createShowScientificAction(settingsService.getNumericalOptions());
        showThousandsAction = createShowThousandsAction(tableOptions);
        chartActionGroup = new ChartActionGroup(project, () -> tableResult);
        exportActionGroup = ExportDataProvider.createActionGroup(project, this);

        tableScroll = ScrollPaneFactory.createScrollPane(myTable, true);
        if (tableOptions.isIndexColumn()) {
            tableScroll.setRowHeaderView(new RowNumberView(myTable));
        }

        setLayout(new BorderLayout());

        splitter = new Splitter(false, 1);
        splitter.setFirstComponent(tableScroll);
        splitter.setDividerPositionStrategy(Splitter.DividerPositionStrategy.KEEP_SECOND_SIZE);
        splitter.setLackOfSpaceStrategy(Splitter.LackOfSpaceStrategy.HONOR_THE_SECOND_MIN_SIZE);
        splitter.setHonorComponentsMinimumSize(true);
        splitter.setAllowSwitchOrientationByMouseClick(false);
        splitter.setShowDividerIcon(false);
        splitter.setShowDividerControls(false);

        add(splitter, BorderLayout.CENTER);

        searchSession = myTable.getSearchSession();
        searchSession.getFindModel().addObserver(m -> searchModelUpdated());
        add(searchSession.getComponent(), BorderLayout.NORTH);

        if (mode.isShowStatusBar()) {
            statusBar = new TableResultStatusPanel(myTable, formatter);
            add(statusBar, BorderLayout.SOUTH);
        }

        add(createLeftToolbar(), BorderLayout.WEST);
        add(createRightToolbar(), BorderLayout.EAST);

        PopupHandler.installPopupMenu(myTable, createPopupMenu(), "TableResultView.Context");
    }

    private AnAction createRepeatAction(BiConsumer<KdbQuery, TableResultView> repeater) {
        if (repeater == null) {
            return null;
        }

        return new BgtAction("_Repeat the Query", "Re-run the query related with this result", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (tableResult != null) {
                    repeater.accept(tableResult.query(), TableResultView.this);
                }
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(tableResult != null);
            }
        };
    }

    @NotNull
    private ShowTableOptionAction createShowScientificAction(NumericalOptions options) {
        return new ShowTableOptionAction("Show Scientific Notation", "Format as computerized scientific notation", KdbIcons.Console.TableScientific, KeyEvent.VK_E, options::isScientificNotation);
    }

    @NotNull
    private ShowTableOptionAction createShowThousandsAction(TableOptions options) {
        return new ShowTableOptionAction("Show Thousands Separator", "Format numbers with thousands separator", KdbIcons.Console.TableThousands, KeyEvent.VK_S, options::isThousandsSeparator);
    }

    @NotNull
    private ToggleAction createShowIndexAction() {
        final ToggleAction action = new EdtToggleAction("Show Index Column", "Show column with row indexes", KdbIcons.Console.TableIndex) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return isShowIndexColumn();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                setShowIndexColumn(state);
            }
        };
        action.registerCustomShortcutSet(KeyEvent.VK_I, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, myTable);
        return action;
    }

    @NotNull
    private ToggleAction createFilterAction() {
        final ToggleAction action = new BgtToggleAction("Filter Columns", "Filter columns list", KdbIcons.Console.ColumnFilter) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return splitter.getSecondComponent() != null;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) {
                    schemaViewPanel = new QSchemaViewPanel(myTable);
                    splitter.setSecondComponent(schemaViewPanel);
                    schemaViewPanel.requestFocus();
                } else {
                    splitter.setSecondComponent(null);
                    schemaViewPanel.dispose();
                    schemaViewPanel = null;
                }
            }
        };
        action.registerCustomShortcutSet(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, myTable);
        return action;
    }

    @NotNull
    private ToggleAction createSearchAction() {
        final ToggleAction action = new BgtToggleAction("_Search", "Search data in the table", AllIcons.Actions.Search) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return searchSession.isOpened();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) {
                    searchSession.open(false);
                } else {
                    searchSession.close();
                }
            }
        };
        action.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)), myTable);
        return action;
    }

    @NotNull
    private KdbOutputFormatter createOutputFormatter() {
        // We can't pass the supplier itself here as it's changed inside the action so use a wrapper
        return new KdbOutputFormatter(new FormatterOptions().withThousandAndScientific(() -> showThousandsAction.supplier.getAsBoolean(), () -> showScientificAction.supplier.getAsBoolean()));
    }

    @NotNull
    private JComponent createLeftToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();

        if (repeatAction != null) {
            group.add(repeatAction);
            group.addSeparator();
        }

        final DefaultActionGroup view = new PopupActionGroup("View Settings", AllIcons.Actions.Show);
        view.add(searchAction);
        view.addSeparator();
        view.add(showIndexAction);
        view.add(showThousandsAction);
        view.add(showScientificAction);

        group.add(view);
        group.addSeparator();
        group.addAll(exportActionGroup);
        group.addSeparator();
        group.add(chartActionGroup);

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.Actions", group, false);
        actionToolbar.setTargetComponent(this);
        return actionToolbar.getComponent();
    }

    @NotNull
    private JComponent createRightToolbar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(filterAction);

        final ActionToolbar filterToolbar = ActionManager.getInstance().createActionToolbar("TableResultView.Filter", group, false);
        filterToolbar.setTargetComponent(this);
        return filterToolbar.getComponent();
    }

    private ActionGroup createPopupMenu() {
        final DefaultActionGroup group = new DefaultActionGroup();

        if (repeatAction != null) {
            group.add(repeatAction);
            group.addSeparator();
        }
        group.add(searchAction);
        group.addSeparator();
        final DefaultActionGroup view = new PopupActionGroup("View Settings", AllIcons.Actions.Show);
        view.add(showIndexAction);
        view.add(showThousandsAction);
        view.add(showScientificAction);
        group.add(view);
        group.addSeparator();
        group.addAll(exportActionGroup);
        group.addSeparator();
        group.add(chartActionGroup);
        return group;
    }

    public boolean isShowIndexColumn() {
        return getNumberTable() != null;
    }

    public void setShowIndexColumn(boolean show) {
        final RowNumberView numberTable = getNumberTable();
        if (show) {
            if (numberTable == null) {
                tableScroll.setRowHeaderView(new RowNumberView(myTable));
            }
        } else {
            if (numberTable != null) {
                numberTable.dispose();
                tableScroll.setRowHeaderView(null);
            }
        }
    }

    private RowNumberView getNumberTable() {
        final JViewport rowHeader = tableScroll.getRowHeader();
        if (rowHeader == null) {
            return null;
        }
        return (RowNumberView) rowHeader.getView();
    }

    private void magicMouseActions(MouseEvent e) {
        if (e.getClickCount() != 2 || e.getButton() != MouseEvent.BUTTON1) {
            return;
        }

        if (e.isAltDown()) {
            new OpenInEditorAction(null, this).performExport(project, this);
        } else {
            if (expandSelectedCell()) {
                return;
            }
            new ClipboardExportAction(null, ExportingType.SELECTION, this).performExport(project, this);
        }
    }

    private boolean expandSelectedCell() {
        final int c = myTable.getSelectedColumn();
        final int r = myTable.getSelectedRow();
        if (c < 0 || r < 0) {
            return false;
        }

        final Object v = myTable.getValueAt(r, c);
        if (!isExpandable(v)) {
            return false;
        }

        final TabsTableResult owner = TabsTableResult.findParentTabs(this);
        if (owner == null) {
            return false;
        }

        final ExpandedTabDetails tabDetails = getExpandedTabDetails(r, c);
        final TableResult from = TableResult.from(new KdbQuery(tabDetails.description), KdbResult.with(v));
        owner.showTabAfter(tabDetails.name, from);
        return true;
    }

    private boolean isExpandable(Object o) {
        if (o == null) {
            return false;
        }
        if (KdbType.isNotEmptyList(o) && tableOptions.isExpandList()) {
            return true;
        }
        if (KdbType.isTable(o) && tableOptions.isExpandTable()) {
            return true;
        }
        return KdbType.isDict(o) && tableOptions.isExpandDict();
    }

    private ExpandedTabDetails getExpandedTabDetails(int r, int c) {
        final Object nativeObject = getNativeObject();
        if (nativeObject instanceof c.Dict && c == 1) {
            Object val = myTable.getValueAt(r, 0);
            if (val instanceof String s) {
                return new ExpandedTabDetails(s, "Expanded result of " + s);
            }
            if (val instanceof char[] ch) {
                final String s = new String(ch);
                return new ExpandedTabDetails(s, "Expanded result of " + s);
            }
        }
        return new ExpandedTabDetails("Expanded Result", "Expanded result at row=" + r + ", col=" + c);
    }

    public JComponent getFocusableComponent() {
        return myTable;
    }

    @Override
    public JBTable getTable() {
        return myTable;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DATA_KEY.is(dataId) || ExportDataProvider.DATA_KEY.is(dataId)) {
            return this;
        }
        return null;
    }

    @Override
    public String getExportName() {
        final Container parent = getParent();
        if (parent instanceof JBTabs jbTabs) {
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
        return tableResult.result().getObject();
    }

    @Override
    public KdbOutputFormatter getOutputFormatter() {
        return formatter;
    }

    public void showResult(@Nullable TableResult tableResult) {
        this.tableResult = tableResult;

        myTable.setTableResult(tableResult);

        searchModelUpdated();

        if (statusBar != null) {
            statusBar.showResult(tableResult);
        }
    }

    private void searchModelUpdated() {
        final RowNumberView numberTable = getNumberTable();
        if (numberTable != null) {
            numberTable.invalidate();
        }

        if (statusBar != null) {
            statusBar.invalidateRowsCount();
        }
    }

    record ExpandedTabDetails(String name, String description) {
    }

    private class ShowTableOptionAction extends EdtToggleAction {
        private BooleanSupplier supplier;

        public ShowTableOptionAction(String text, String description, Icon icon, int keyCode, BooleanSupplier supplier) {
            super(text, description, icon);
            this.supplier = supplier;
            registerCustomShortcutSet(keyCode, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, myTable);
        }

        public boolean isSelected() {
            return supplier.getAsBoolean();
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return isSelected();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            supplier = () -> state;
            myTable.repaint();

            if (statusBar != null) {
                statusBar.recalculateValues();
            }
        }
    }
}