package org.kdb.inside.brains.view.console;

import com.intellij.codeEditor.printing.PrintAction;
import com.intellij.execution.console.BaseConsoleExecuteActionHandler;
import com.intellij.execution.console.GutterContentProvider;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.actions.ScrollToTheEndToolbarAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.docking.DockableContent;
import com.intellij.ui.docking.impl.DockManagerImpl;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsEx;
import com.intellij.ui.tabs.JBTabsFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.action.ToggleConnectAction;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.ide.PopupActionGroup;
import org.kdb.inside.brains.ide.runner.LineNumberGutterProvider;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.export.ExportDataProvider;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class KdbConsolePanel extends SimpleToolWindowPanel implements DataProvider, Disposable {
    private final JBTabsEx tabs;
    private final JBTabs consoleTabs;

    private final TabInfo consoleTab;
    private final JBSplitter splitter;

    private TabInfo tableResultTab;
    private ConsoleSplitType activeSplitType;

    private LanguageConsoleView console;

    private boolean showOnlyLast = false;
    private boolean scrollToTheEnd = true;

    private final KdbScope scope;
    private final Project project;
    private final InstanceConnection connection;
    private final Consumer<KdbConsolePanel> panelKillerConsumer;

    private final KdbOutputFormatter formatter;
    private final GutterContentProvider gutterProvider;
    private final KdbConnectionManager connectionManager;

    private final AnAction renameAction = new RenameTabAction();
    private final AnAction openInFrameAction = new OpenInFrameAction();
    private final TheScopeListener scopeListener = new TheScopeListener();
    private final TheKdbConnectionListener connectionListener = new TheKdbConnectionListener();

    private final List<FrameWrapper> openedFrames = new ArrayList<>();

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

        tabs = (JBTabsEx) JBTabsFactory.createTabs(project, this);
        // We can't use Supplier here as it's been Getter before and some versions are not compatiable anymore.
        tabs.setPopupGroup(new ActionGroup() {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return buildTabsPopupGroup().getChildren(e);
            }
        }, "KdbConsoleTabsMenu", true);

        tabs.addTabMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (tabs.getTargetInfo() == consoleTab) {
                    return;
                }
                if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
                    IdeEventQueue.getInstance().blockNextEvents(e);
                    closeTab(tabs.findInfo(e));
                } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    ActionManager.getInstance().tryToExecute(renameAction, e, e.getComponent(), "", true);
                }
            }
        });

        tabs.getPresentation().setTabDraggingEnabled(true);
        tabs.getComponent().setTransferHandler(new TransferHandler(null) {
            public boolean importData(JComponent comp, Transferable t) {
                System.out.println("importData: " + t);
/*
                if (myFileDropHandler.canHandleDrop(t.getTransferDataFlavors())) {
                    myFileDropHandler.handleDrop(t, myProject, myWindow);
                    return true;
                }
*/
                return false;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                System.out.println("canImport: " + comp);
                return false;
//                return myFileDropHandler.canHandleDrop(transferFlavors);
            }
        });

        splitter = createSplitter();
        consoleTabs = JBTabsFactory.createTabs(project, this);
        splitter.setFirstComponent(consoleTabs.getComponent());

        consoleTab = createConsoleTab();

        tabs.addTab(consoleTab, 0);
        setContent(tabs.getComponent());


        changeSplitting(splitType);
        setToolbar(createMainToolbar().getComponent());
    }

    private JBSplitter createSplitter() {
        JBSplitter splitter = new JBSplitter(true, 0.5f);
        splitter.setResizeEnabled(true);
        splitter.setHonorComponentsMinimumSize(true);
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

        final AnAction[] consoleActions = createConsoleActions();
        final ActionToolbar kdbConsoleActionToolbar = am.createActionToolbar("KdbConsoleActionToolbar", new DefaultActionGroup(consoleActions), false);
        kdbConsoleActionToolbar.setTargetComponent(consolePanel);
        consolePanel.setToolbar(kdbConsoleActionToolbar.getComponent());

        final TabInfo tab = new TabInfo(consolePanel);
        tab.setText("Console");
        tab.setIcon(KdbIcons.Console.Console);
        tab.setObject(console);
        tab.setTabColor(connection.getInstance().getInheritedColor());

        return tab;
    }

    private @NotNull ActionGroup buildTabsPopupGroup() {
        final TabInfo info = tabs.getTargetInfo();
        if (info == null) {
            return ActionGroup.EMPTY_GROUP;
        }

        final boolean resultView = info.getObject() instanceof TableResultView;
        if (resultView) {
            return buildResultViewPopup();
        } else {
            return buildConsolePopup();
        }
    }

    private @NotNull ActionGroup buildConsolePopup() {
        return ActionGroup.EMPTY_GROUP;
    }

    private @NotNull ActionGroup buildResultViewPopup() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(renameAction);
        group.add(openInFrameAction);
        return group;
    }

    @NotNull
    private AnAction[] createConsoleActions() {
        final AnAction[] consoleActions = console.createConsoleActions();
        for (int i = 0; i < consoleActions.length; i++) {
            AnAction consoleAction = consoleActions[i];

            if (consoleAction instanceof ScrollToTheEndToolbarAction) {
                final Presentation tp = consoleAction.getTemplatePresentation();
                consoleActions[i] = new ToggleAction(tp.getText(), tp.getDescription(), tp.getIcon()) {
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
                };
            } else if (consoleAction instanceof PrintAction) {
                consoleActions[i] = new ToggleAction("Show History", "Show all history or only last one if disabled", KdbIcons.Console.ShowOnlyLast) {
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
                };
            }
        }
        return consoleActions;
    }

    private ActionToolbar createMainToolbar() {
        final ActionManager am = ActionManager.getInstance();
        final DefaultActionGroup consoleActions = new DefaultActionGroup();

        consoleActions.add(new ToggleConnectAction(connection));
        consoleActions.add(new DumbAwareAction("Modify Instance", "Change the instance settings across the application", AllIcons.General.Settings) {
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
        consoleActions.add(new DumbAwareAction("Cancel the Query", "Cancel current running query", AllIcons.Actions.Suspend) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(connection.getQuery() != null);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                connection.cancelQuery();
            }
        });

        consoleActions.addSeparator();

        consoleActions.add(new AnAction("Load KDB Table", "Loads previouly saved KDB table in binary format", KdbIcons.Console.ImportBinary) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                loadBinaryFile();
            }
        });
        consoleActions.addSeparator();

        final DefaultActionGroup g = new PopupActionGroup("Split Console and Result Tabs", KdbIcons.Console.Layout);
        g.add(new SplitAction("Tabs view", "Show console and results in tabs", KdbIcons.Console.LayoutNo, ConsoleSplitType.NO));
        g.addSeparator();
        g.add(new SplitAction("Split Down", "Show table view result tabs under the console", KdbIcons.Console.LayoutDown, ConsoleSplitType.DOWN));
        g.add(new SplitAction("Split Right", "Show table view result tabs on the right of the console", KdbIcons.Console.LayoutRight, ConsoleSplitType.RIGHT));

        consoleActions.add(g);

        consoleActions.addSeparator();
        consoleActions.add(new DumbAwareAction("Close", "Closed connection and this console", KdbIcons.Console.Kill) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                connection.disconnect();
                panelKillerConsumer.accept(KdbConsolePanel.this);
            }
        });

        final ActionToolbar kdbConsoleMainToolbar = am.createActionToolbar("KdbConsoleMainToolbar", consoleActions, false);
        kdbConsoleMainToolbar.setTargetComponent(this);
        return kdbConsoleMainToolbar;
    }

    private void changeSplitting(ConsoleSplitType type) {
        final boolean splat = getContent() == splitter;
        if (type == ConsoleSplitType.NO || (splat && tabs.getTabCount() == 0)) {
            if (splat) {
                consoleTabs.removeTab(consoleTab);
                tabs.addTab(consoleTab, 0);
                splitter.setSecondComponent(null);
                setContent(tabs.getComponent());
            }
        } else {
            if (activeSplitType == ConsoleSplitType.NO || (!splat && tabs.getTabCount() > 1)) {
                tabs.removeTab(consoleTab);
                consoleTabs.addTab(consoleTab);
                splitter.setSecondComponent(tabs.getComponent());
                setContent(splitter);
            }
            splitter.setOrientation(type == ConsoleSplitType.DOWN);
        }
        activeSplitType = type;
    }

    private void loadBinaryFile() {
        final VirtualFile virtualFile = FileChooser.chooseFile(new FileChooserDescriptor(true, false, false, false, false, false), project, null);
        if (virtualFile != null && virtualFile.exists()) {
            try {
                final KdbQuery query = new KdbQuery("Loaded from file: " + virtualFile.getCanonicalPath());
                final KdbResult result = new KdbResult();

                final byte[] bytes = VfsUtil.loadBytes(virtualFile);
                final Object deserialize = new c().deserialize(bytes);

                final KdbResult complete = result.complete(deserialize);
                final TableResult tr = TableResult.from(query, complete);
                if (tr == null) {
                    throw new IllegalStateException("Incorrect object type: " + deserialize.getClass().getSimpleName());
                }

                final TabInfo tab = createNewResultViewTab(virtualFile.getNameWithoutExtension(), tr);
                tabs.addTab(tab);
                tabs.select(tab, false);
                changeSplitting(activeSplitType);
            } catch (Exception ex) {
                Messages.showErrorDialog(project, "The file can't be loaded: " + ex.getMessage(), "Incorrect KDB Table File");
            }
        }
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

    private void showConsoleResult() {
        if (tabs.getSelectedInfo() != consoleTab) {
            tabs.select(consoleTab, false);
        }
    }

    private void showTableResult(TableResultView resultView, TableResult tableResult) {
        if (resultView != null) {
            resultView.showResult(tableResult);

            final TabInfo info = tabs.findInfo(resultView);
            if (info != null) {
                tabs.select(info, false);
            }
        } else {
            if (tableResultTab == null) {
                tableResultTab = createNewResultViewTab("Table Result", tableResult);

                tabs.addTab(tableResultTab, activeSplitType == ConsoleSplitType.NO ? 1 : 0);
                changeSplitting(activeSplitType);
            } else {
                ((TableResultView) tableResultTab.getObject()).showResult(tableResult);
            }
            tabs.select(tableResultTab, false);
        }
    }

    private void closeTab(TabInfo info) {
        if (info == null || !(info.getObject() instanceof TableResultView)) {
            return;
        }
        if (tableResultTab == info) {
            tableResultTab = null;
        }
        tabs.removeTab(info);
        changeSplitting(activeSplitType);
    }

    private TabInfo createNewResultViewTab(String name, TableResult tableResult) {
        final TableResultView tableResultView = new TableResultView(project, formatter, false, this::execute);
        tableResultView.showResult(tableResult);

        final TabInfo info = new TabInfo(tableResultView);
        info.setText(name);
        info.setObject(tableResultView);

        info.setIcon(KdbIcons.Console.Table);
        info.setPreferredFocusableComponent(tableResultView.getFocusableComponent());

        final AnAction closeAction = new AnAction("Close", "Close current result tab", AllIcons.Actions.Close) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                closeTab(info);
            }
        };
        closeAction.getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
        info.setTabLabelActions(new DefaultActionGroup(closeAction), "KdbConsolePanel");

/*        info.setDragOutDelegate(new TabInfo.DragOutDelegate() {
            private DragSession mySession;


            @Override
            public void dragOutStarted(@NotNull MouseEvent mouseEvent, @NotNull TabInfo info) {
                TabInfo previousSelection = info.getPreviousSelection();
                Image img = JBTabsImpl.getComponentImage(info);
                if (previousSelection == null) {
                    previousSelection = tabs.getToSelectOnRemoveOf(info);
                }
                int dragStartIndex = tabs.getIndexOf(info);
                boolean isPinnedAtStart = info.isPinned();
                info.setHidden(true);
                if (previousSelection != null) {
                    tabs.select(previousSelection, true);
                }

//                myFile = (VirtualFile) info.getObject();
//                myFile.putUserData(EditorWindow.DRAG_START_INDEX_KEY, dragStartIndex);
//                myFile.putUserData(EditorWindow.DRAG_START_LOCATION_HASH_KEY, System.identityHashCode(myTabs));
//                myFile.putUserData(EditorWindow.DRAG_START_PINNED_KEY, isPinnedAtStart);
//                Presentation presentation = new Presentation(info.getText());
//                if (DockManagerImpl.REOPEN_WINDOW.isIn(myFile)) {
//                    presentation.putClientProperty(DockManagerImpl.REOPEN_WINDOW, DockManagerImpl.REOPEN_WINDOW.get(myFile, true));
//                }
//                presentation.setIcon(info.getIcon());
//                EditorComposite composite = myWindow.getComposite(myFile);
//                FileEditor[] editors = composite != null ? composite.getAllEditors().toArray(FileEditor.EMPTY_ARRAY) : FileEditor.EMPTY_ARRAY;
//                boolean isNorthPanelAvailable = DockManagerImpl.isNorthPanelAvailable(editors);

                final Presentation presentation = new Presentation(info.getText());
                presentation.setIcon(info.getIcon());

                final DockableView view = new DockableView(img, (TableResultView) info.getObject(), presentation, new Dimension(), info.isPinned());
                mySession = getDockManager().createDragSession(mouseEvent, view);
            }

            private DockManager getDockManager() {
                return DockManager.getInstance(project);
            }

            @Override
            public void processDragOut(@NotNull MouseEvent event, @NotNull TabInfo source) {
                mySession.process(event);
            }

            @Override
            public void dragOutFinished(@NotNull MouseEvent event, TabInfo source) {
                boolean copy = UIUtil.isControlKeyDown(event) || mySession.getResponse(event) == DockContainer.ContentResponse.ACCEPT_COPY;
                if (!copy) {
//                    myFile.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, Boolean.TRUE);
//                    FileEditorManagerEx.getInstanceEx(myProject).closeFile(myFile, myWindow);
                } else {
                    source.setHidden(false);
                }
                mySession.process(event);
                mySession = null;
            }

            @Override
            public void dragOutCancelled(TabInfo source) {
                source.setHidden(false);
                if (mySession != null) {
                    mySession.cancel();
                }
                mySession = null;
            }
        });*/

        return info;
    }

    public static class DockableView implements DockableContent<TableResultView> {
        final Image myImg;
        private final Presentation myPresentation;
        private final Dimension myPreferredSize;
        private final boolean myPinned;
        private final boolean myNorthPanelAvailable;
        private final TableResultView myResultView;

        public DockableView(Image img,
                            TableResultView resultView,
                            Presentation presentation,
                            Dimension preferredSize,
                            boolean isFilePinned) {
            this(img, resultView, presentation, preferredSize, isFilePinned, DockManagerImpl.isNorthPanelVisible(UISettings.getInstance()));
        }

        public DockableView(Image img,
                            TableResultView resultView,
                            Presentation presentation,
                            Dimension preferredSize,
                            boolean isFilePinned,
                            boolean isNorthPanelAvailable) {
            myImg = img;
            myResultView = resultView;
            myPresentation = presentation;
            myPreferredSize = preferredSize;
            myPinned = isFilePinned;
            myNorthPanelAvailable = isNorthPanelAvailable;
        }

        @NotNull
        @Override
        public TableResultView getKey() {
            return myResultView;
        }

        @Override
        public Image getPreviewImage() {
            return myImg;
        }

        @Override
        public Dimension getPreferredSize() {
            return myPreferredSize;
        }

        @Override
        public String getDockContainerType() {
            return "kdb-tablerevultview-contains";
        }

        @Override
        public Presentation getPresentation() {
            return myPresentation;
        }

        @Override
        public void close() {
        }
    }

    private void clearHistory() {
        console.getHistoryViewer().getDocument().setText("");
    }

    private void processQuery(KdbQuery query) {
        processQuery(query, null);
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
                    showConsoleResult();
                } else {
                    printRoundtrip(result);
                    printToConsole(formatter.resultToString(result, true, true) + "\n", ConsoleViewContentType.NORMAL_OUTPUT);

                    final TableResult tbl = TableResult.from(query, result);
                    if (tbl != null) {
                        showTableResult(resultView, tbl);
                    } else {
                        showConsoleResult();
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
        openedFrames.forEach(FrameWrapper::close);

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
        showConsoleResult();
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
            return tabs.getSelectedInfo();
        }
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            final TabInfo selectedInfo = tabs.getSelectedInfo();
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

    private class RenameTabAction extends AnAction {
        public RenameTabAction() {
            super("Rename/Pin", "Rename the result set to keep it in memory", null);
            registerCustomShortcutSet(KeyEvent.VK_R, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KdbConsolePanel.this);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final TabInfo info = tabs.getTargetInfo();
            if (info == null) {
                return;
            }

            final Set<String> collect = tabs.getTabs().stream().map(TabInfo::getText).collect(Collectors.toSet());
            UIUtils.createNameDialog(project, "New name", info.getText(), e.getDataContext(), name -> !collect.contains(name), name -> {
                info.setText(name);
                if (info == tableResultTab) {
                    tableResultTab = null;
                }
            });
        }
    }

    private class OpenInFrameAction extends AnAction {
        public OpenInFrameAction() {
            super("Open in Frame", "Open the tab in a separate frame", null);
            registerCustomShortcutSet(KeyEvent.VK_F, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, KdbConsolePanel.this);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final TabInfo info = tabs.getTargetInfo();
            if (info == null) {
                return;
            }

            tabs.removeTab(info);
            if (info == tableResultTab) {
                tableResultTab = null;
            }
            changeSplitting(activeSplitType);

            final FrameWrapper frame = new FrameWrapper(project, "KdbConsole.TableResultFrame", false, info.getText(), info.getComponent());
            openedFrames.add(frame);
            frame.setOnCloseHandler(() -> {
                openedFrames.remove(frame);
                return true;
            });
            frame.show();
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
