package org.kdb.inside.brains.view.console;

import com.intellij.execution.actions.ClearConsoleAction;
import com.intellij.execution.console.GutterContentProvider;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.AbstractToggleUseSoftWrapsAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.IoErrorText;
import icons.KdbIcons;
import kx.c;
import org.apache.commons.io.FileUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.action.EdtToggleAction;
import org.kdb.inside.brains.action.PopupActionGroup;
import org.kdb.inside.brains.action.connection.ToggleConnectAction;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.settings.KdbSettingsListener;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.settings.SettingsBean;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.KdbToolWindowPanel;
import org.kdb.inside.brains.view.LineNumberGutterProvider;
import org.kdb.inside.brains.view.console.table.TableMode;
import org.kdb.inside.brains.view.console.table.TableResult;
import org.kdb.inside.brains.view.console.table.TableResultView;
import org.kdb.inside.brains.view.console.table.TabsTableResult;
import org.kdb.inside.brains.view.console.watch.VariableNode;
import org.kdb.inside.brains.view.console.watch.WatchesView;
import org.kdb.inside.brains.view.export.ExportDataProvider;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KdbConsolePanel extends KdbToolWindowPanel implements DataProvider, Disposable {
    private final TheSettingsListener settingsListener = new TheSettingsListener();

    private LanguageConsoleView console;
    private ConsoleSplitType activeSplitType;

    private final JBSplitter mainSplitter;
    private final JBSplitter watchesSplitter;
    private final TabsTableResult resultTabs;

    private final JBTabs consoleTabs;
    private final TabInfo consoleTab;
    private final List<String> watchesCache = new ArrayList<>();
    private boolean softWrap = true;
    private boolean showHistory = true;

    private final KdbScope scope;
    private final Project project;
    private final InstanceConnection connection;
    private final Consumer<KdbConsolePanel> panelKillerConsumer;

    private final KdbOutputFormatter formatter;
    private final GutterContentProvider gutterProvider;
    private final KdbConnectionManager connectionManager;

    private final TheScopeListener scopeListener = new TheScopeListener();
    private KdbInstance instanceCopy;
    private final TheKdbConnectionListener connectionListener = new TheKdbConnectionListener();

    private final KdbSettingsService settingsService;
    private boolean scrollToTheEnd = true;

    private boolean changeSplittingEvent = false;
    public static final DataKey<TabInfo> TAB_INFO_DATA_KEY = DataKey.create("KdbConsole.TabInfo");

    public KdbConsolePanel(Project project, InstanceConnection connection, ConsoleSplitType splitType, Consumer<KdbConsolePanel> panelKillerConsumer) {
        super(false);
        this.project = project;
        this.connection = connection;
        this.panelKillerConsumer = panelKillerConsumer;

        instanceCopy = connection.getInstance().copy();

        this.settingsService = KdbSettingsService.getInstance();
        settingsService.addSettingsListener(settingsListener);

        scope = this.connection.getInstance().getScope();
        if (scope != null) {
            scope.addScopeListener(scopeListener);
        }

        formatter = KdbOutputFormatter.getDefault();
        gutterProvider = new LineNumberGutterProvider();

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(connectionListener);

        watchesSplitter = createWatchesSplitter();

        consoleTab = createConsoleTab();
        consoleTabs = JBTabsFactory.createTabs(project, this);

        resultTabs = new TabsTableResult(project, this);
        resultTabs.addListener(new DockContainer.Listener() {
            @Override
            public void contentAdded(@NotNull Object key) {
                invalidateSplitting();
            }

            @Override
            public void contentRemoved(@NotNull Object key) {
                invalidateSplitting();
            }
        }, this);

        mainSplitter = createTabsSplitter();
//        mainSplitter.setSecondComponent(resultTabs);
//        mainSplitter.setFirstComponent(consoleTabs.getComponent());
        setContent(mainSplitter);

        changeSplitting(splitType);

        setToolbar(createMainToolbar().getComponent());
    }

    private JBSplitter createTabsSplitter() {
        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setResizeEnabled(true);
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setHonorComponentsPreferredSize(false);
        splitter.setAndLoadSplitterProportionKey("KdbConsole.Splitter.Tabs");
        return splitter;
    }

    private JBSplitter createWatchesSplitter() {
        JBSplitter splitter = new JBSplitter(false, 0.5f);
        splitter.setResizeEnabled(true);
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setHonorComponentsPreferredSize(false);
        splitter.setAndLoadSplitterProportionKey("KdbConsole.Splitter.Watches");
        return splitter;
    }

    private TabInfo createConsoleTab() {
        final String historyName = "KdbConsolePanel-" + connection.getName();

        final LanguageConsoleBuilder b = new LanguageConsoleBuilder();
        b.gutterContentProvider(gutterProvider);
        console = b.build(project, QLanguage.INSTANCE);

        LanguageConsoleBuilder.registerExecuteAction(console,
                text -> {
                    if (!showHistory) {
                        clearHistory();
                        gutterProvider.beforeEvaluate(console.getHistoryViewer());
                    }
                    processQuery(new KdbQuery(text));
                },
                historyName,
                historyName,
                view -> connection.isConnected()
        );

        printToConsole("Kdb console for instance: " + connection.getName() + ".\n", ConsoleViewContentType.SYSTEM_OUTPUT);

        watchesSplitter.setFirstComponent(console.getComponent());

        final SimpleToolWindowPanel consolePanel = new SimpleToolWindowPanel(false);
        consolePanel.setContent(watchesSplitter);

        final ActionManager am = ActionManager.getInstance();

        final DefaultActionGroup consoleActions = createConsoleActions();
        final ActionToolbar kdbConsoleActionToolbar = am.createActionToolbar("KdbConsoleActionToolbar", consoleActions, false);
        kdbConsoleActionToolbar.setTargetComponent(consolePanel);
        consolePanel.setToolbar(kdbConsoleActionToolbar.getComponent());

        final TabInfo tab = new TabInfo(consolePanel);
        tab.setText("Console");
        tab.setIcon(KdbIcons.Console.Console);
        tab.setObject(console);

        updateTabColor(tab);

        HistoryCopyHandler.redefineHistoricalViewer(console);

        return tab;
    }

    private void updateTabColor(TabInfo tab) {
        final Color inheritedColor = getInstance().getInheritedColor();
        tab.setTabColor(inheritedColor);

        final Color consoleColor = settingsService.getConsoleOptions().isConsoleBackground() ? inheritedColor : null;
        console.getHistoryViewer().setBackgroundColor(consoleColor);
        console.getConsoleEditor().setBackgroundColor(consoleColor);
    }

    @NotNull
    private DefaultActionGroup createConsoleActions() {
        final DefaultActionGroup actions = new DefaultActionGroup();

        final AnAction softWrapAction = new EdtToggleAction() {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return softWrap;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                softWrap = state;
                AbstractToggleUseSoftWrapsAction.toggleSoftWraps(((ConsoleViewImpl) console).getEditor(), null, softWrap);
            }
        };
        ActionUtil.copyFrom(softWrapAction, IdeActions.ACTION_EDITOR_USE_SOFT_WRAPS);
        actions.add(softWrapAction);

        final String message = ActionsBundle.message("action.EditorConsoleScrollToTheEnd.text");
        actions.add(new EdtToggleAction(message, message, AllIcons.RunConfigurations.Scroll_down) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return scrollToTheEnd;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                scrollToTheEnd = state;
                if (state) {
                    console.requestScrollingToEnd();
                }
            }
        });

        actions.add(new EdtToggleAction("Show History", "Show all history or only last one if disabled", KdbIcons.Console.ShowHistory) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return showHistory;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                showHistory = state;
                if (!showHistory) {
                    clearHistory();
                }
            }
        });

        actions.add(new ClearConsoleAction());
        return actions;
    }

    private ActionToolbar createMainToolbar() {
        final ActionManager am = ActionManager.getInstance();
        final DefaultActionGroup actions = new DefaultActionGroup();

        actions.add(new ToggleConnectAction(connection));
        actions.add(new EdtAction("Modify Instance", "Change the instance settings across the application", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final KdbInstance instance = getInstance();
                if (instance == null) {
                    return;
                }
                final InstanceEditorDialog instanceEditor = new InstanceEditorDialog(InstanceEditorDialog.Mode.UPDATE, project, instance);
                if (instanceEditor.showAndGet()) {
                    instance.updateFrom(instanceEditor.createInstance());
                }
            }
        });
        actions.add(new EdtAction("Cancel the Query", "Cancel current running query", AllIcons.Actions.Suspend) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connection.getQuery() != null);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                connection.cancelQuery();
            }
        });

        actions.addSeparator();
        actions.add(new EdtToggleAction("Show Watches Panel", "Show watches panel", AllIcons.Debugger.Watch) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return watchesSplitter.getSecondComponent() != null;
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                if (state) {
                    showWatches();
                } else {
                    hideWatches();
                }
            }
        });

        actions.addSeparator();

        actions.add(new EdtAction("Upload File to Instance", "Set content of a file into the instance variable", KdbIcons.Console.UploadFile) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                uploadFileToVariable();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connection.isConnected());
            }
        });
        actions.addSeparator();

        actions.add(new DumbAwareAction("Open KDB Table", "Opens previously saved KDB table in binary format", KdbIcons.Console.ImportBinary) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadBinaryFile();
            }
        });
        actions.addSeparator();

        final DefaultActionGroup g = new PopupActionGroup("Split Console and Result Tabs", KdbIcons.Console.Layout);
        g.add(new SplitAction("Tabs view", "Show console and results in tabs", KdbIcons.Console.LayoutNo, ConsoleSplitType.NO));
        g.addSeparator();
        g.add(new SplitAction("Split Down", "Show table view result tabs under the console", KdbIcons.Console.LayoutDown, ConsoleSplitType.DOWN));
        g.add(new SplitAction("Split Right", "Show table view result tabs on the right of the console", KdbIcons.Console.LayoutRight, ConsoleSplitType.RIGHT));

        actions.add(g);

        actions.addSeparator();
        actions.add(new DumbAwareAction("Close", "Closed connection and this console", KdbIcons.Console.Kill) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                connection.disconnect();
                panelKillerConsumer.accept(KdbConsolePanel.this);
            }
        });

        final ActionToolbar kdbConsoleMainToolbar = am.createActionToolbar("KdbConsoleMainToolbar", actions, false);
        kdbConsoleMainToolbar.setTargetComponent(this);
        return kdbConsoleMainToolbar;
    }

    private void showWatches() {
        if (getWatchesView() != null) {
            return;
        }

        final WatchesView view = new WatchesView(project, KdbConsolePanel.this, connection);
        view.replaceVariables(watchesCache);
        Disposer.register(KdbConsolePanel.this, view);
        watchesSplitter.setSecondComponent(view);
    }

    private void hideWatches() {
        final WatchesView oldWatchesView = getWatchesView();
        if (oldWatchesView == null) {
            return;
        }

        watchesCache.clear();
        oldWatchesView.getAllVariables().stream().map(VariableNode::getExpression).forEach(watchesCache::add);

        watchesSplitter.setSecondComponent(null);
        Disposer.dispose(oldWatchesView);
    }

    private WatchesView getWatchesView() {
        return (WatchesView) watchesSplitter.getSecondComponent();
    }

    private void invalidateSplitting() {
        changeSplitting(activeSplitType);
    }

    private void changeSplitting(ConsoleSplitType type) {
        if (changeSplittingEvent) {
            return;
        }

        changeSplittingEvent = true;
        try {
            if (type == ConsoleSplitType.NO) {
                if (activeSplitType != ConsoleSplitType.NO) {
                    consoleTabs.removeTab(consoleTab);
                    resultTabs.showConsole(consoleTab);

                    // remove first, when add to the new one
                    mainSplitter.setFirstComponent(null);
                    mainSplitter.setSecondComponent(resultTabs);
                }
            } else {
                if (activeSplitType != type) {
                    resultTabs.hideConsole();
                    consoleTabs.addTab(consoleTab);

                    final JComponent consoleComponent = consoleTabs.getComponent();
                    mainSplitter.setFirstComponent(consoleComponent);
                    mainSplitter.setSecondComponent(resultTabs.isEmpty() ? null : resultTabs);

                    final boolean verticalSplit = type == ConsoleSplitType.DOWN;
                    if (mainSplitter.getOrientation() != verticalSplit) {
                        mainSplitter.setOrientation(verticalSplit);
                    }
                }

                final boolean empty = resultTabs.isEmpty();
                final JComponent secondComponent = mainSplitter.getSecondComponent();
                if (empty && secondComponent != null) {
                    mainSplitter.setSecondComponent(null);
                } else if (!empty && secondComponent == null) {
                    mainSplitter.setSecondComponent(resultTabs);
                }
            }
            activeSplitType = type;
        } finally {
            changeSplittingEvent = false;
        }
    }

    private void loadBinaryFile() {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
        final VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile == null || !virtualFile.exists()) {
            return;
        }

        final String tabName = virtualFile.getNameWithoutExtension();
        final Application application = ApplicationManager.getApplication();
        new Task.Backgroundable(project, "Loading a table from " + tabName, false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Loading file");
                    final byte[] bytes = Files.readAllBytes(virtualFile.toNioPath());
                    indicator.setText("Deserializing content: " + FileUtils.byteCountToDisplaySize(bytes.length));
                    final Object deserialize = new c().deserialize(bytes);

                    final KdbQuery query = new KdbQuery("Loaded from file: " + virtualFile.getCanonicalPath());
                    final KdbResult result = new KdbResult();

                    final KdbResult complete = result.complete(deserialize);
                    final TableResult tr = TableResult.from(query, complete);
                    if (tr == null) {
                        throw new IllegalStateException("Incorrect object type: " + deserialize.getClass().getSimpleName());
                    }
                    application.invokeLater(() -> {
                        resultTabs.showTab(tabName, tr);
                        indicator.setText("");
                    });
                } catch (Exception ex) {
                    application.invokeLater(() -> Messages.showErrorDialog(project, "The file can't be loaded: " + ex.getMessage(), "Incorrect KDB Table File"));
                }
            }
        }.queue();
    }

    public void showTableResult(String name, TableResult result) {
        resultTabs.showTab(name, result, TableMode.COMPACT, -1);
    }

    public InstanceConnection getConnection() {
        return connection;
    }

    public void execute(String text) {
        execute(new KdbQuery(text), null);
    }

    private void execute(KdbQuery query, TableResultView resultView) {
        if (!showHistory) {
            clearHistory();
        }
        gutterProvider.beforeEvaluate(console.getHistoryViewer());
        printToConsole(query.getExpression() + "\n", ConsoleViewContentType.USER_INPUT);
        processQuery(query, resultView);
    }

    private KdbInstance getInstance() {
        return connection.getInstance();
    }

    private void selectConsole(boolean clearTableResult) {
        if (clearTableResult && settingsService.getConsoleOptions().isClearTableResult()) {
            resultTabs.clearTableResult();
        }
        resultTabs.selectConsole();
    }

    private void clearHistory() {
        console.getHistoryViewer().getDocument().setText("");
    }

    private void processQuery(KdbQuery query) {
        processQuery(query, null);
    }

    private void uploadFileToVariable() {
        final UploadFileDialog d = new UploadFileDialog(project);
        if (!d.showAndGet()) {
            return;
        }

        try {
            if (connection.getState() != InstanceState.CONNECTED) {
                throw new IOException("Instance in not connected");
            }
            final KdbResult res = connection.query(d.createQuery());
            if (res.isError()) {
                throw (Exception) res.getObject();
            }
            final Path path = d.getPath();
            printToConsole("File " + d.getPath().toString() + " of " + FileUtils.byteCountToDisplaySize(Files.size(path)) + " has been set to `" + d.getVariableName() + " variable.\n", ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
            selectConsole(false);
        } catch (Exception ex) {
            Messages.showErrorDialog(project, IoErrorText.message(ex), "File Can't Be Uploaded");
        }
    }

    private void processQuery(KdbQuery query, TableResultView resultView) {
        try {
            connection.query(query, result -> {
                if (result.isError()) {
                    final Exception ex = (Exception) result.getObject();
                    if (ex instanceof IOException) {
                        return;
                    }
                    printRoundtrip(result);

                    final String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
                    printToConsole(message + "\n", ConsoleViewContentType.ERROR_OUTPUT);
                    selectConsole(true);
                } else {
                    printRoundtrip(result);
                    printToConsole(formatter.resultToString(result, true, true) + "\n", ConsoleViewContentType.NORMAL_OUTPUT);

                    final TableResult tbl = TableResult.from(query, result);
                    if (tbl != null) {
                        resultTabs.updateTableResult(tbl, resultView, this::execute);
                    } else {
                        selectConsole(true);
                    }
                }

                final WatchesView watchesView = getWatchesView();
                if (watchesView != null) {
                    watchesView.refreshAllVariables();
                }
            });
        } catch (ConcurrentQueryException ex) {
            final int i = Messages.showOkCancelDialog(project, "The instance already has running query. Would you like to cancel it (it's not always possible to cancel a query)?", "The Instance Is Busy", "Do Nothing and Wait More Time", "Cancel The Query", AllIcons.General.WarningDialog);
            if (i == Messages.CANCEL) {
                connection.cancelQuery();
            }
        }
    }

    private void printRoundtrip(KdbResult result) {
        printToConsole("(" + result.getTime() + ", roundtrip: " + result.getRoundtripMillis() + "ms / " + result.getRoundtripNanos() + "ns)\n", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }

    @Override
    public void dispose() {
        settingsService.removeSettingsListener(settingsListener);
        connectionManager.removeConnectionListener(connectionListener);
        if (scope != null) {
            scope.removeScopeListener(scopeListener);
        }
        Disposer.dispose(console);
    }

    private void printInstanceError(Exception ex) {
        String msg;
        if (ex != null) {
            if (ex.getMessage() != null) {
                msg = "Connection has been lost: " + ex.getMessage();
            } else {
                msg = "Connection has been lost: " + ex.getClass().getSimpleName();
            }
        } else {
            msg = "Connection can't be established";
        }
        printToConsole(msg + ".\n", ConsoleViewContentType.ERROR_OUTPUT);
        selectConsole(true);
    }

    private void printInstanceConnecting() {
        // not required at this moment
    }

    private void printInstanceConnected() {
        printToConsole("Instance has been connected: " + getInstance().toSymbol() + ".\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    private void printInstanceDisconnected() {
        printToConsole("Instance has been disconnected.\n", ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    private void printToConsole(String msg, ConsoleViewContentType type) {
        console.print(msg, type);
        if (scrollToTheEnd) {
            console.requestScrollingToEnd();
        }
    }

    private void processInstanceChanged(KdbInstance instance) {
        if (!instanceCopy.toQualifiedSymbol().equals(instance.toQualifiedSymbol())) {
            connection.disconnect();
            connection.connect();
        }

        final KdbInstance oldInstance = instanceCopy;
        instanceCopy = instance.copy();
        firePropertyChange("instanceParameters", oldInstance, instanceCopy);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (LangDataKeys.CONSOLE_VIEW.is(dataId)) {
            return console;
        }
        if (TAB_INFO_DATA_KEY.is(dataId)) {
            return resultTabs.getSelectedInfo();
        }
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            final TabInfo selectedInfo = resultTabs.getSelectedInfo();
            if (selectedInfo != null && selectedInfo.getComponent() instanceof TableResultView) {
                return selectedInfo.getComponent();
            }
        }
        return super.getData(dataId);
    }

    void saveState(@NotNull Element e) {
        final WatchesView watchesView = getWatchesView();

        final Element o = new Element("options");
        o.setAttribute("showWatches", String.valueOf(watchesView != null));
        o.setAttribute("tabsProportion", String.valueOf(mainSplitter.getProportion()));
        o.setAttribute("watchesProportion", String.valueOf(watchesSplitter.getProportion()));
        o.setAttribute("softWrap", String.valueOf(softWrap));
        o.setAttribute("showHistory", String.valueOf(showHistory));
        o.setAttribute("scrollToTheEnd", String.valueOf(scrollToTheEnd));
        o.setAttribute("activeSplitType", activeSplitType.name());
        e.addContent(o);

        if (watchesView != null) {
            watchesCache.clear();
            watchesView.getAllVariables().stream().map(VariableNode::getExpression).forEach(watchesCache::add);
        }

        if (!watchesCache.isEmpty()) {
            final Element w = new Element("watches");
            watchesCache.forEach(n -> w.addContent(new Element("watch").setText(n)));
            e.addContent(w);
        }
    }

    void loadState(@NotNull Element state) {
        final Element options = state.getChild("options");
        if (options == null) {
            return;
        }

        watchesCache.clear();
        final Element watches = state.getChild("watches");
        if (watches != null) {
            watches.getChildren().stream()
                    .map(Element::getText)
                    .filter(s -> s != null && !s.isBlank())
                    .forEach(watchesCache::add);
        }

        final String showWatchesAttr = options.getAttributeValue("showWatches");
        if (showWatchesAttr != null) {
            if (Boolean.parseBoolean(showWatchesAttr)) {
                showWatches();
            } else {
                hideWatches();
            }
        }

        final String softWrapAttr = options.getAttributeValue("softWrap");
        if (softWrapAttr != null) {
            softWrap = Boolean.parseBoolean(softWrapAttr);
        }

        final String showOnlyLastAttr = options.getAttributeValue("showHistory");
        if (showOnlyLastAttr != null) {
            showHistory = Boolean.parseBoolean(showOnlyLastAttr);
        }

        final String scrollToTheEndAttr = options.getAttributeValue("scrollToTheEnd");
        if (scrollToTheEndAttr != null) {
            scrollToTheEnd = Boolean.parseBoolean(scrollToTheEndAttr);
        }

        try {
            final String tabsProportionAttr = options.getAttributeValue("tabsProportion");
            if (tabsProportionAttr != null) {
                mainSplitter.setProportion(Float.parseFloat(tabsProportionAttr));
            }
        } catch (Exception ignore) {
        }

        try {
            final String watchesProportionAttr = options.getAttributeValue("watchesProportion");
            if (watchesProportionAttr != null) {
                watchesSplitter.setProportion(Float.parseFloat(watchesProportionAttr));
            }
        } catch (Exception ignore) {
        }

        try {
            changeSplitting(ConsoleSplitType.valueOf(options.getAttributeValue("activeSplitType")));
        } catch (Exception ignore) {

        }
    }

    private class SplitAction extends EdtToggleAction {
        private final ConsoleSplitType splitType;

        public SplitAction(String text, String description, Icon icon, ConsoleSplitType splitType) {
            super(text, description, icon);
            this.splitType = splitType;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return splitType == activeSplitType;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            changeSplitting(state ? splitType : ConsoleSplitType.NO);
        }
    }

    private class TheScopeListener implements KdbScopeListener {
        @Override
        public void itemUpdated(KdbScope scope, InstanceItem item) {
            InstanceItem i = getInstance();
            while (i != null && i != item) {
                i = i.getParent();
            }
            if (i != null) {
                updateTabColor(consoleTab);
            }

            if (item == getInstance()) {
                processInstanceChanged(getInstance());
            }
        }
    }

    private class TheSettingsListener implements KdbSettingsListener {
        @Override
        public void settingsChanged(KdbSettingsService service, SettingsBean<?> bean) {
            if (bean instanceof ConsoleOptions) {
                updateTabColor(consoleTab);
            }
        }
    }

    private class TheKdbConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
            if (connection != KdbConsolePanel.this.connection) {
                return;
            }

            switch (newState) {
                case CONNECTING -> printInstanceConnecting();
                case CONNECTED -> printInstanceConnected();
                case DISCONNECTED -> {
                    final Exception error = connection.getDisconnectError();
                    if (error != null) {
                        printInstanceError(error);
                    } else {
                        printInstanceDisconnected();
                    }
                }
            }
        }
    }
}
