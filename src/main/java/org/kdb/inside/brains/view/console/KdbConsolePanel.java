package org.kdb.inside.brains.view.console;

import com.intellij.codeEditor.printing.PrintAction;
import com.intellij.execution.console.BaseConsoleExecuteActionHandler;
import com.intellij.execution.console.GutterContentProvider;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.NextOccurenceAction;
import com.intellij.ide.actions.PreviousOccurenceAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.action.ToggleConnectAction;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.ide.PopupActionGroup;
import org.kdb.inside.brains.ide.runner.LineNumberGutterProvider;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.table.TableResult;
import org.kdb.inside.brains.view.console.table.TableResultView;
import org.kdb.inside.brains.view.console.table.TabsTableResult;
import org.kdb.inside.brains.view.export.ExportDataProvider;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class KdbConsolePanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private final TabsTableResult resultTabs;
    private LanguageConsoleView console;
    private ConsoleSplitType activeSplitType;
    private boolean showOnlyLast = false;

    private final JBTabs consoleTabs;
    private final TabInfo consoleTab;
    private boolean scrollToTheEnd = true;

    private final JBSplitter splitter;

    private final KdbScope scope;
    private final Project project;
    private final InstanceConnection connection;
    private final Consumer<KdbConsolePanel> panelKillerConsumer;

    private final KdbOutputFormatter formatter;
    private final GutterContentProvider gutterProvider;
    private final KdbConnectionManager connectionManager;

    private final TheScopeListener scopeListener = new TheScopeListener();
    private final TheKdbConnectionListener connectionListener = new TheKdbConnectionListener();

    public static final DataKey<TabInfo> TAB_INFO_DATA_KEY = DataKey.create("KdbConsole.TabInfo");

    public KdbConsolePanel(Project project, InstanceConnection connection, ConsoleSplitType splitType, Consumer<KdbConsolePanel> panelKillerConsumer) {
        super(false);
        this.project = project;
        this.connection = connection;
        this.panelKillerConsumer = panelKillerConsumer;

        scope = this.connection.getInstance().getScope();
        if (scope != null) {
            scope.addScopeListener(scopeListener);
        }

        formatter = KdbOutputFormatter.getInstance();
        gutterProvider = new LineNumberGutterProvider();

        connectionManager = KdbConnectionManager.getManager(project);
        connectionManager.addConnectionListener(connectionListener);

        splitter = createSplitter();
        consoleTabs = JBTabsFactory.createTabs(project, this);
        splitter.setFirstComponent(consoleTabs.getComponent());

        consoleTab = createConsoleTab();
        resultTabs = new TabsTableResult(project, this);
        resultTabs.addListener(new DockContainer.Listener() {
            @Override
            public void contentAdded(@NotNull Object key) {
                changeSplitting();
            }

            @Override
            public void contentRemoved(Object key) {
                changeSplitting();
            }
        }, this);

        resultTabs.showConsole(consoleTab);
        setContent(resultTabs);

        changeSplitting(splitType);
        setToolbar(createMainToolbar().getComponent());
    }

    private JBSplitter createSplitter() {
        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setResizeEnabled(true);
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setHonorComponentsPreferredSize(false);
        splitter.setAndLoadSplitterProportionKey("KdbConsole.Tabs.Splitter");
        return splitter;
    }

    private TabInfo createConsoleTab() {
        final LanguageConsoleBuilder b = new LanguageConsoleBuilder();
        b.executionEnabled(view -> connection != null && connection.getState() == InstanceState.CONNECTED);

        b.initActions(new BaseConsoleExecuteActionHandler(true) {
            @Override
            protected void execute(@NotNull String text, @NotNull LanguageConsoleView console) {
                if (showOnlyLast) {
                    clearHistory();
                    gutterProvider.beforeEvaluate(console.getHistoryViewer());
                }
                processQuery(new KdbQuery(text));
            }
        }, "KdbConsolePanel-" + connection.getName());

        b.gutterContentProvider(gutterProvider);

        console = b.build(project, QLanguage.INSTANCE);

        printToConsole("Kdb console for instance: " + connection.getName() + ".\n", ConsoleViewContentType.SYSTEM_OUTPUT);

        final SimpleToolWindowPanel consolePanel = new SimpleToolWindowPanel(false);
        consolePanel.setContent(console.getComponent());

        final ActionManager am = ActionManager.getInstance();

        final DefaultActionGroup consoleActions = createConsoleActions();
        final ActionToolbar kdbConsoleActionToolbar = am.createActionToolbar("KdbConsoleActionToolbar", consoleActions, false);
        kdbConsoleActionToolbar.setTargetComponent(consolePanel);
        consolePanel.setToolbar(kdbConsoleActionToolbar.getComponent());

        final TabInfo tab = new TabInfo(consolePanel);
        tab.setText("Console");
        tab.setIcon(KdbIcons.Console.Console);
        tab.setObject(console);
        tab.setTabColor(connection.getInstance().getInheritedColor());

        return tab;
    }

    @NotNull
    private DefaultActionGroup createConsoleActions() {
        final DefaultActionGroup actions = new DefaultActionGroup();

        for (AnAction action : console.createConsoleActions()) {
            if (action instanceof PreviousOccurenceAction || action instanceof NextOccurenceAction) {
                continue;
            }

            if (action instanceof ScrollToTheEndToolbarAction) {
                final Presentation tp = action.getTemplatePresentation();
                actions.add(new ToggleAction(tp.getText(), tp.getDescription(), tp.getIcon()) {
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
            } else if (action instanceof PrintAction) {
                actions.add(new ToggleAction("Show History", "Show all history or only last one if disabled", KdbIcons.Console.ShowOnlyLast) {
                    @Override
                    public boolean isSelected(@NotNull AnActionEvent e) {
                        return !showOnlyLast;
                    }

                    @Override
                    public void setSelected(@NotNull AnActionEvent e, boolean state) {
                        showOnlyLast = !state;
                        if (showOnlyLast) {
                            clearHistory();
                        }
                    }
                });
            } else {
                actions.add(action);
            }
        }
        return actions;
    }

    private ActionToolbar createMainToolbar() {
        final ActionManager am = ActionManager.getInstance();
        final DefaultActionGroup actions = new DefaultActionGroup();

        actions.add(new ToggleConnectAction(connection));
        actions.add(new DumbAwareAction("Modify Instance", "Change the instance settings across the application", AllIcons.General.Settings) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final KdbInstance instance = connection.getInstance();
                if (instance == null) {
                    return;
                }
                final InstanceEditorDialog instanceEditor = new InstanceEditorDialog(InstanceEditorDialog.Mode.UPDATE, project, instance);
                if (instanceEditor.showAndGet()) {
                    instance.updateFrom(instanceEditor.createInstance());
                }
            }
        });
        actions.add(new DumbAwareAction("Cancel the Query", "Cancel current running query", AllIcons.Actions.Suspend) {
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

        actions.add(new DumbAwareAction("Upload File to Instance", "Set content of a file into the instance variable", KdbIcons.Console.UploadFile) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                uploadFileToVariable();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connection.getState() == InstanceState.CONNECTED);
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

    private void changeSplitting() {
        changeSplitting(activeSplitType);
    }

    private void changeSplitting(ConsoleSplitType type) {
        final boolean splat = getContent() == splitter;
        if (type == ConsoleSplitType.NO || (splat && resultTabs.getTabCount() == 0)) {
            if (splat) {
                consoleTabs.removeTab(consoleTab);
                resultTabs.showConsole(consoleTab);
                splitter.setSecondComponent(null);
                setContent(resultTabs);
            }
        } else {
            if (activeSplitType == ConsoleSplitType.NO || (!splat && resultTabs.getTabCount() > 1)) {
                resultTabs.hideConsole(consoleTab);
                consoleTabs.addTab(consoleTab);
                splitter.setSecondComponent(resultTabs);
                setContent(splitter);
            }
            splitter.setOrientation(type == ConsoleSplitType.DOWN);
        }
        activeSplitType = type;
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

    public InstanceConnection getConnection() {
        return connection;
    }

    public void execute(String text) {
        execute(new KdbQuery(text), null);
    }

    private void execute(KdbQuery query, TableResultView resultView) {
        if (showOnlyLast) {
            clearHistory();
        }
        gutterProvider.beforeEvaluate(console.getHistoryViewer());
        printToConsole(query.getExpression() + "\n", ConsoleViewContentType.USER_INPUT);
        processQuery(query, resultView);
    }

    private void selectConsole() {
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
            selectConsole();
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
                    selectConsole();
                } else {
                    printRoundtrip(result);
                    printToConsole(formatter.resultToString(result, true, true) + "\n", ConsoleViewContentType.NORMAL_OUTPUT);

                    final TableResult tbl = TableResult.from(query, result);
                    if (tbl != null) {
                        resultTabs.updateTableResult(tbl, resultView, this::execute);
                    } else {
                        selectConsole();
                    }
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
        resultTabs.dispose();

        connectionManager.removeConnectionListener(connectionListener);
        if (scope != null) {
            scope.removeScopeListener(scopeListener);
        }
        console.dispose();
        console = null;
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
        selectConsole();
    }

    private void printInstanceConnecting() {
        // not required at this moment
    }

    private void printInstanceConnected() {
        printToConsole("Instance has been connected: " + connection.getInstance().toSymbol() + ".\n", ConsoleViewContentType.SYSTEM_OUTPUT);
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

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
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

    private class SplitAction extends ToggleAction {
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
            InstanceItem i = connection.getInstance();
            while (i != null && i != item) {
                i = i.getParent();
            }
            if (i != null) {
                consoleTab.setTabColor(connection.getInstance().getInheritedColor());
            }
        }
    }

    private class TheKdbConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionCreated(InstanceConnection connection) {

        }

        @Override
        public void connectionRemoved(InstanceConnection connection) {

        }

        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {

        }

        @Override
        public void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
            if (connection != KdbConsolePanel.this.connection) {
                return;
            }

            switch (newState) {
                case CONNECTING:
                    printInstanceConnecting();
                    break;
                case CONNECTED:
                    printInstanceConnected();
                    break;
                case DISCONNECTED:
                    final Exception error = connection.getDisconnectError();
                    if (error != null) {
                        printInstanceError(error);
                    } else {
                        printInstanceDisconnected();
                    }
                    break;
            }
        }
    }
}
