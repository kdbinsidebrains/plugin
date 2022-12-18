package org.kdb.inside.brains.view.inspector;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.icons.AllIcons;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.structureView.newStructureView.TreeActionWrapper;
import com.intellij.ide.structureView.newStructureView.TreeActionsOwner;
import com.intellij.ide.structureView.newStructureView.TreeModelWrapper;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ide.util.treeView.smartTree.SmartTreeStructure;
import com.intellij.ide.util.treeView.smartTree.TreeAction;
import com.intellij.ide.util.treeView.smartTree.TreeElementWrapper;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.*;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.tree.TreeVisitor;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.IoErrorText;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.tree.TreeUtil;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QExpression;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.index.QIndexService;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.KdbConsoleToolWindow;
import org.kdb.inside.brains.view.inspector.model.*;

import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.intellij.ide.structureView.newStructureView.StructureViewComponent.registerAutoExpandListener;


@State(name = "KdbInstanceInspector", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class InspectorToolWindow extends SimpleToolWindowPanel implements PersistentStateComponent<InspectorToolState>, KdbConnectionListener, InstanceScanner.ScanListener, DataProvider, Disposable {
    public static final String PLACE = "Kdb.InstanceInspectorToolbar";
    private final Project project;

    private InstanceConnection connection;
    private final KdbConnectionManager connectionManager;
    private final ModelWrapper modelWrapper;

    private final Tree tree;
    private final InstanceScanner scanner;
    private final CopyProvider copyProvider = new MyCopyProvider();
    private final RefreshAction refreshAction = new RefreshAction();
    private final MyAutoScrollToSourceHandler scrollToSourceHandler;
    private final InspectorToolState settings = new InspectorToolState();

    private final Map<InstanceConnection, CacheElement> instancesCache = new HashMap<>();

    private final JLabel statusBar = new JLabel("", JLabel.RIGHT);
    private boolean visible;
    private boolean disposed;

    private static final DateTimeFormatter STATUS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InspectorToolWindow(@NotNull Project project) {
        super(true);
        this.project = project;
        this.scanner = new InstanceScanner(project, this);
        this.connectionManager = KdbConnectionManager.getManager(project);

        this.modelWrapper = new ModelWrapper(project, settings, this);

        this.tree = new Tree(modelWrapper.asyncTreeMode);
        tree.setRootVisible(true);
        tree.setCellRenderer(new NodeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        scrollToSourceHandler = new MyAutoScrollToSourceHandler(tree);

        registerAutoExpandListener(tree, modelWrapper.treeModel);

        TreeUtil.installActions(tree);

        new TreeSpeedSearch(tree, path -> InspectorElement.unwrap(path).map(InspectorElement::getName).orElse(null), true);

        setContent(ScrollPaneFactory.createScrollPane(tree));
        setToolbar(createToolbar());
        createStatusBar();

        final DefaultActionGroup popup = createPopup();

        PopupHandler.installPopupHandler(tree, popup, "KdbInspectorPopup");

        updateEmptyText(null);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                return executeSelectedPath(tree.getSelectionPath());
            }
        }.installOn(tree);
    }

    @NotNull
    private DefaultActionGroup createPopup() {
        final DumbAwareAction scroll = new DumbAwareAction(ActionsBundle.messagePointer("action.EditSource.text"), ActionsBundle.messagePointer("action.EditSource.description"), AllIcons.Actions.EditSource) {
            public void update(@NotNull AnActionEvent e) {
                final Presentation presentation = e.getPresentation();
                presentation.setEnabled(isExecutableSelected());
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                scrollPathToSource(tree.getSelectionPath(), true);
            }
        };
        scroll.registerCustomShortcutSet(KeyEvent.VK_F4, 0, tree);

        final DumbAwareAction query = new DumbAwareAction("Query Item Value", "Query value of the element from the instance", KdbIcons.Instance.Execute) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                final Presentation presentation = e.getPresentation();
                presentation.setEnabled(isExecutableSelected());
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                executeSelectedPath(tree.getSelectionPath());
            }
        };
        query.registerCustomShortcutSet(KeyEvent.VK_ENTER, 0, tree);

        final DumbAwareAction diffAction = new DumbAwareAction("Diff Source Vs Instance", "Show difference of source code vs defined in the instance.", AllIcons.Actions.Diff) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                final FunctionElement el = getSelectedElement(FunctionElement.class);
                // Only function can be compared
                e.getPresentation().setEnabled(el != null && el.isFunction());
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                showDiffIfPossible(e.getProject());
            }
        };
        diffAction.registerCustomShortcutSet(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK, tree);

        final DefaultActionGroup popup = new DefaultActionGroup();
        popup.add(ActionManager.getInstance().getAction("$Copy"));
        popup.addSeparator();
        popup.add(scroll);
        popup.add(query);
        popup.addSeparator();
        popup.add(diffAction);

        return popup;
    }

    private void showDiffIfPossible(Project project) {
        final FunctionElement el = getSelectedElement(FunctionElement.class);
        if (el == null) {
            return;
        }
        final String canonicalName = el.getCanonicalName();


        new Task.Backgroundable(project, "Loading function diff", false, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final KdbResult query = connection.query(new KdbQuery(canonicalName));
                    final Object object = query.getObject();
                    if (object instanceof c.Function) {
                        final String text = ((c.Function) object).getContent();
                        ApplicationManager.getApplication().invokeLater(() -> showDiffResult(project, canonicalName, text));
                    } else {
                        ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project, "Response is not a function", "Function Definition Can't Be Loaded: " + canonicalName));
                    }
                } catch (Exception ex) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project, IoErrorText.message(ex), "Function Definition Can't Be Loaded: " + canonicalName));
                }
            }
        }.queue();
    }

    @NotNull
    private String getSourceContent(String canonicalName) {
        final QVarDeclaration declaration = findVarDeclaration(canonicalName);
        if (declaration == null) {
            return "";
        }

        final PsiElement parent = declaration.getParent();
        if (!(parent instanceof QAssignmentExpr)) {
            return "";
        }

        final QAssignmentExpr assignment = (QAssignmentExpr) parent;
        final QExpression expression = assignment.getExpression();
        if (expression == null) {
            return "";
        }
        return expression.getText();
    }

    private void showDiffResult(Project project, String canonicalName, String instSource) {
        final DiffContentFactory diff = DiffContentFactory.getInstance();
        final DocumentContent inst = diff.create(project, instSource);
        final DocumentContent source = diff.create(project, getSourceContent(canonicalName));

        final SimpleDiffRequest simpleDiffRequest = new SimpleDiffRequest(canonicalName + " diff", source, inst, "Source definition", "Instance definition");
        DiffManager.getInstance().showDiff(project, simpleDiffRequest);
    }

    private boolean isExecutableSelected() {
        return getSelectedElement(ExecutableElement.class) != null;
    }

    @Nullable
    private static String getCanonicalName(TreePath path, boolean executable) {
        final Optional<InspectorElement> unwrap = InspectorElement.unwrap(path, executable ? ExecutableElement.class : InspectorElement.class);
        if (unwrap.isEmpty()) {
            return null;
        }

        final InspectorElement element = unwrap.get();
        if (executable && element instanceof TableElement && ((TableElement) element).isHistorical()) {
            return null;
        }
        return element.getCanonicalName();
    }

    private <T extends InspectorElement> T getSelectedElement(Class<T> type) {
        return InspectorElement.unwrap(tree.getSelectionPath(), type).orElse(null);
    }

    private void createStatusBar() {
        statusBar.setBorder(JBUI.Borders.empty(5));

        add(statusBar, BorderLayout.SOUTH);
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        final ContentManager cm = toolWindow.getContentManager();
        final Content content = cm.getFactory().createContent(this, null, false);
        cm.addContent(content);

        visible = toolWindow.isVisible();

        connectionManager.addConnectionListener(this);

        project.getMessageBus().connect(this).subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow tw) {
                if (toolWindow == tw) {
                    visible = true;
                    connectionActivated(connection, connectionManager.getActiveConnection());
                }
            }

            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                if (visible && !toolWindow.isVisible()) {
                    connectionActivated(connection, null);
                    visible = false;
                }
            }
        });
    }

    @NotNull
    private JComponent createToolbar() {
        final DefaultActionGroup result = new DefaultActionGroup();

        result.add(refreshAction);
        result.addSeparator();
        result.addAll(modelWrapper.createActions());
        result.addSeparator();
        result.add(scrollToSourceHandler.createToggleAction());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PLACE, result, true);
        toolbar.setTargetComponent(tree);
        return toolbar.getComponent();
    }

    private boolean executeSelectedPath(TreePath path) {
        if (path == null) {
            return false;
        }

        final String name = getCanonicalName(path, true);
        if (name == null) {
            return false;
        }

        KdbConsoleToolWindow.getInstance(project).execute(connection, name);
        return true;
    }

    @Override
    public void scanFailed(InstanceConnection connection, Exception exception) {
        updateEmptyText(exception);
        modelWrapper.updateElement(null);
    }

    @Override
    public void scanFinished(InstanceConnection connection, KdbResult result) {
        updateEmptyText(null);
        modelWrapper.updateElement(new InstanceElement(connection, result));
    }

    private void updateEmptyText(Exception ex) {
        final StatusText emptyText = tree.getEmptyText();
        emptyText.setText("");
        if (ex == null) {
            emptyText.appendText("Please connect to an instance to scan it's structure", StatusText.DEFAULT_ATTRIBUTES, e -> refreshInstance());
        } else {
            emptyText.appendText("Instance structure can't be loaded: " + ex.getMessage(), SimpleTextAttributes.ERROR_ATTRIBUTES);
            emptyText.appendSecondaryText("Try to load it once again", StatusText.DEFAULT_ATTRIBUTES, e -> refreshInstance());
        }
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (CommonDataKeys.PROJECT.is(dataId)) {
            return project;
        }

        if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
            return copyProvider;
        }

        return super.getData(dataId);
    }

    @Override
    public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
        if (!visible) {
            return;
        }

        final InstanceElement element = modelWrapper.getElement();
        if (element != null) {
            instancesCache.computeIfAbsent(connection, c -> new CacheElement(element)).storeState(tree);
        }

        connection = activated;

        final CacheElement cached = instancesCache.get(connection);
        if (cached != null) {
            modelWrapper.updateElement(cached.element).onProcessed(cached::restoreState);
        } else {
            modelWrapper.updateElement(null);
            if (connection != null && KdbSettingsService.getInstance().getInspectorOptions().isScanOnConnect() && connection.getState() != InstanceState.DISCONNECTED) {
                refreshInstance();
            }
        }
        updateEmptyText(null);
    }

    @Override
    public void dispose() {
        visible = false;
        disposed = true;
        instancesCache.clear();
        modelWrapper.dispose();
        connectionManager.removeConnectionListener(this);
    }

    @Nullable
    private QVarDeclaration getDeclaration(@Nullable TreePath path) {
        if (path == null) {
            return null;
        }
        final String canonicalName = getCanonicalName(path, true);
        if (canonicalName == null || canonicalName.isEmpty()) {
            return null;
        }
        return findVarDeclaration(canonicalName);
    }

    @Nullable
    private QVarDeclaration findVarDeclaration(String canonicalName) {
        final QIndexService instance = QIndexService.getInstance(project);
        try {
            final Optional<QVarDeclaration> first = instance.findGlobalDeclarations(canonicalName, GlobalSearchScope.allScope(project)).stream().findFirst();
            return first.orElse(null);
        } catch (IndexNotReadyException ignore) {
            return null;
        }
    }

    private void scrollPathToSource(@Nullable TreePath path, boolean requestFocus) {
        final QVarDeclaration declaration = getDeclaration(path);
        if (declaration != null) {
            declaration.navigate(requestFocus);
        }
    }

    private void refreshInstance() {
        if (connection == null) {
            return;
        }
        scanner.scanInstance(connection);
    }

    @Override
    public @Nullable InspectorToolState getState() {
        return settings;
    }

    @Override
    public void loadState(@NotNull InspectorToolState state) {
        settings.copyFom(state);
    }

    static List<ExecutableElement> getSuggestions(String prefix, InstanceElement ie) {
        return getSuggestions(prefix, (InspectorElement) ie);
    }

    static List<ExecutableElement> getSuggestions(String prefix, InspectorElement ie) {
        final List<ExecutableElement> res = new ArrayList<>();
        for (InspectorElement child : ie.getChildren()) {
            final String canonicalName = child.getCanonicalName();
            if (canonicalName.startsWith(prefix) || (child instanceof NamespaceElement && prefix.startsWith(canonicalName))) {
                if (child instanceof NamespaceElement) {
                    res.addAll(getSuggestions(prefix, child));
                } else if (child instanceof ExecutableElement) {
                    res.add((ExecutableElement) child);
                }
            }
        }
        return res;
    }

    public List<ExecutableElement> getSuggestions(String prefix) {
        final InstanceElement ie = modelWrapper.getElement();
        if (ie == null || ie.getChildren().length == 0 || prefix.isEmpty()) {
            return List.of();
        }
        return getSuggestions(prefix, ie);
    }

    private static class NameResolver extends TreeVisitor.ByComponent<String, InspectorElement> {
        private final boolean onlyPath;

        public NameResolver(@NotNull String name, boolean onlyPath) {
            super(name, o -> (InspectorElement) StructureViewComponent.unwrapWrapper(o));
            this.onlyPath = onlyPath;
        }

        @Override
        protected boolean matches(@NotNull InspectorElement element, @NotNull String name) {
            if (!onlyPath || element instanceof NamespaceElement) {
                return name.equals(element.getCanonicalName());
            }
            return false;
        }

        @Override
        protected boolean contains(@NotNull InspectorElement element, @NotNull String name) {
            if (element instanceof RootElement || element instanceof InstanceElement) {
                return true;
            }

            final String canonicalName = element.getCanonicalName();
            if (name.startsWith(canonicalName)) {
                final int length = canonicalName.length();
                return length == name.length() || name.charAt(length) == '.';
            }
            return false;
        }
    }

    private final class CacheElement {
        private String selectedPath;
        private List<String> expandedPaths;

        private final InstanceElement element;

        public CacheElement(InstanceElement element) {
            this.element = element;
        }

        public void storeState(Tree tree) {
            selectedPath = getCanonicalName(tree.getSelectionPath());
            expandedPaths = TreeUtil.collectExpandedPaths(tree).stream().map(this::getCanonicalName).collect(Collectors.toList());
        }

        public void restoreState(AsyncTreeModel model) {
            if (expandedPaths != null) {
                for (String expandedPath : expandedPaths) {
                    restoreElement(model, new NameResolver(expandedPath, true), TreeUtil::promiseExpand);
                }
            }

            if (selectedPath != null) {
                restoreElement(model, new NameResolver(selectedPath, false), TreeUtil::selectPath);
            }
        }

        private void restoreElement(AsyncTreeModel model, NameResolver resolver, BiConsumer<Tree, TreePath> consumer) {
            model.accept(resolver).onProcessed(p -> {
                if (p != null) {
                    consumer.accept(tree, p);
                }
            });
        }

        private String getCanonicalName(TreePath path) {
            return InspectorElement.unwrap(path).map(InspectorElement::getCanonicalName).orElse(null);
        }
    }

    private class ModelWrapper implements Disposable {
        private final TreeActionsOwner actionsOwner;
        private final AsyncTreeModel asyncTreeMode;
        private final InspectorTreeModel treeModel;
        private final SmartTreeStructure smartStructure;
        private final StructureTreeModel<SmartTreeStructure> structureModel;

        public ModelWrapper(Project project, InspectorToolState settings, Disposable disposable) {
            treeModel = new InspectorTreeModel();

            actionsOwner = new TreeActionsOwner() {
                @Override
                public void setActionActive(String name, boolean state) {
                    settings.setEnabled(name, state);
                    rebuild();
                }

                @Override
                public boolean isActionActive(String name) {
                    return settings.isEnabled(name);
                }
            };

            smartStructure = new SmartTreeStructure(project, new TreeModelWrapper(treeModel, actionsOwner)) {
                @Override
                public void rebuildTree() {
                    if (disposed) {
                        return;
                    }
                    super.rebuildTree();
                }

                @Override
                public boolean isToBuildChildrenInBackground(@NotNull final Object element) {
                    return getRootElement() == element;
                }

                @NotNull
                @Override
                protected TreeElementWrapper createTree() {
                    return StructureViewComponent.createWrapper(myProject, myModel.getRoot(), myModel);
                }
            };

            structureModel = new StructureTreeModel<>(smartStructure, this);
            asyncTreeMode = new AsyncTreeModel(structureModel, this);

            Disposer.register(disposable, this);
        }

        public ActionGroup createActions() {
            final DefaultActionGroup group = new DefaultActionGroup();
            final List<TreeAction[]> actionsList = List.of(treeModel.getSorters(), treeModel.getGroupers(), treeModel.getFilters());
            for (TreeAction[] actions : actionsList) {
                if (actions.length != 0) {
                    group.addSeparator();
                    for (TreeAction action : actions) {
                        if (action == null) {
                            group.addSeparator();
                        } else {
                            group.add(new TreeActionWrapper(action, actionsOwner));
                        }
                    }
                }
            }
            return group;
        }

        public InstanceElement getElement() {
            return treeModel.getInstanceElement();
        }

        public Promise<AsyncTreeModel> updateElement(InstanceElement element) {
            treeModel.updateModel(element);
            return rebuild().then(o -> asyncTreeMode);
        }

        public Promise<?> rebuild() {
            final InstanceElement instanceElement = getElement();
            if (instanceElement == null) {
                statusBar.setText("");
            } else {
                statusBar.setText("Updated: " + STATUS_FORMATTER.format(instanceElement.getResult().getTime()));
            }

            return structureModel.getInvoker().invoke(() -> {
                smartStructure.rebuildTree();
                structureModel.invalidate();
            });
        }

        @Override
        public void dispose() {
            treeModel.dispose();
            asyncTreeMode.dispose();
            structureModel.dispose();
        }
    }

    private final class RefreshAction extends DumbAwareAction {
        RefreshAction() {
            super("Refresh Instance", "Reloads the instance structure", KdbIcons.Inspector.Refresh);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            refreshInstance();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            presentation.setEnabled(connectionManager.getActiveConnection() != null);
        }
    }

    private final class MyCopyProvider implements CopyProvider {
        @Override
        public void performCopy(@NotNull DataContext dataContext) {
            final TreePath[] selectionPaths = tree.getSelectionPaths();
            if (selectionPaths == null) {
                return;
            }

            final StringBuilder b = new StringBuilder();
            for (TreePath path : selectionPaths) {
                final String name = getCanonicalName(path, false);
                if (b.length() != 0) {
                    b.append(System.lineSeparator());
                }
                b.append(name);
            }
            CopyPasteManager.getInstance().setContents(new TextTransferable(b));
        }

        @Override
        public boolean isCopyEnabled(@NotNull DataContext dataContext) {
            return tree.getSelectionCount() != 0;
        }

        @Override
        public boolean isCopyVisible(@NotNull DataContext dataContext) {
            return tree.getSelectionCount() != 0;
        }
    }

    private final class MyAutoScrollToSourceHandler extends AutoScrollToSourceHandler {
        public MyAutoScrollToSourceHandler(Tree tree) {
            install(tree);
        }

        @Override
        protected boolean isAutoScrollMode() {
            return !project.isDisposed() && settings.isAutoScroll();
        }

        @Override
        protected void setAutoScrollMode(boolean state) {
            settings.setAutoScroll(state);
        }

        @Override
        protected void scrollToSource(@NotNull Component tree) {
            scrollPathToSource(((JTree) tree).getSelectionPath(), false);
        }
    }
}