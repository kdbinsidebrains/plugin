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
import com.intellij.ide.util.treeView.smartTree.Group;
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
import com.intellij.util.messages.MessageBusConnection;
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
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QExpression;
import org.kdb.inside.brains.psi.QVarDeclaration;
import org.kdb.inside.brains.psi.index.QIndexService;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.KdbToolWindowPanel;
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
import static org.kdb.inside.brains.action.ActionPlaces.INSPECTOR_VIEW_TOOLBAR;


@State(name = "KdbInspectorView", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class InspectorToolWindow extends KdbToolWindowPanel implements PersistentStateComponent<InspectorToolState>, KdbConnectionListener, DataProvider, Disposable {
    private final Project project;
    private final Map<InstanceConnection, CacheElement> instancesCache = new HashMap<>();
    private boolean visible;

    private final KdbConnectionManager connectionManager;
    private InstanceScanner scanner;
    private InstanceConnection activeConnection;
    private Tree tree;
    private ModelWrapper modelWrapper;

    private final CopyProvider copyProvider = new MyCopyProvider();
    private final InspectorToolState settings = new InspectorToolState();

    private final JLabel statusBar = new JLabel("", JLabel.RIGHT);
    private MessageBusConnection toolWindowListener;

    private static final DateTimeFormatter STATUS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public InspectorToolWindow(@NotNull Project project) {
        super(true);
        this.project = project;
        this.connectionManager = KdbConnectionManager.getManager(project);

        createStatusBar();
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        final ContentManager cm = toolWindow.getContentManager();
        final Content content = cm.getFactory().createContent(this, null, false);
        cm.addContent(content);
        Disposer.register(this, content);

        this.visible = toolWindow.isVisible();

        this.modelWrapper = new ModelWrapper(project, settings);

        this.tree = new Tree(modelWrapper.asyncTreeMode);
        tree.setRootVisible(true);
        tree.setCellRenderer(new NodeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        this.scanner = new InstanceScanner(project, new InstanceScanner.ScanListener() {
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
        });


        registerAutoExpandListener(tree, modelWrapper.treeModel);

        TreeUtil.installActions(tree);

        new TreeSpeedSearch(tree, true, path -> InspectorElement.unwrap(path).map(InspectorElement::getCanonicalName).orElse(null));

        setToolbar(createToolbar());
        setContent(ScrollPaneFactory.createScrollPane(tree));

        final DefaultActionGroup popup = createPopup();

        PopupHandler.installPopupMenu(tree, popup, "KdbInspectorPopup");

        updateEmptyText(null);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                return executeSelectedPath(tree.getSelectionPath());
            }
        }.installOn(tree);

        connectionManager.addConnectionListener(this);
        activeConnection = connectionManager.getActiveConnection();

        toolWindowListener = project.getMessageBus().connect();
        toolWindowListener.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow tw) {
                if (toolWindow == tw) {
                    visible = true;
                    connectionActivated(activeConnection, connectionManager.getActiveConnection());
                }
            }

            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                if (visible && !toolWindow.isVisible()) {
                    connectionActivated(activeConnection, null);
                    visible = false;
                }
            }
        });
    }

    private @NotNull JComponent createToolbar() {
        final DefaultActionGroup result = new DefaultActionGroup();
        result.add(new RefreshAction());
        result.addSeparator();
        result.addAll(modelWrapper.createActions());
        result.addSeparator();
        result.add(new MyAutoScrollToSourceHandler(tree).createToggleAction());

        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(INSPECTOR_VIEW_TOOLBAR, result, true);
        toolbar.setTargetComponent(tree);

        return toolbar.getComponent();
    }

    @NotNull
    private DefaultActionGroup createPopup() {
        final DumbAwareAction scroll = new EdtAction(ActionsBundle.messagePointer("action.EditSource.text"), ActionsBundle.messagePointer("action.EditSource.description"), AllIcons.Actions.EditSource) {
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

        final DumbAwareAction query = new EdtAction("Query Item Value", "Query value of the element from the instance", KdbIcons.Instance.Execute) {
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

        final DumbAwareAction diffAction = new EdtAction("Diff Source Vs Instance", "Show difference of source code vs defined in the instance.", AllIcons.Actions.Diff) {
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

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
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
                    final KdbResult query = activeConnection.query(new KdbQuery(canonicalName));
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
        if (!(parent instanceof QAssignmentExpr assignment)) {
            return "";
        }

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

    private boolean executeSelectedPath(TreePath path) {
        if (path == null) {
            return false;
        }

        final String name = getCanonicalName(path, true);
        if (name == null) {
            return false;
        }

        KdbConsoleToolWindow.getInstance(project).execute(activeConnection, name);
        return true;
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
            instancesCache.computeIfAbsent(activeConnection, c -> new CacheElement(element)).storeState(tree);
        }

        activeConnection = activated;

        final CacheElement cached = instancesCache.get(activeConnection);
        if (cached != null) {
            modelWrapper.updateElement(cached.element).onProcessed(cached::restoreState);
        } else {
            modelWrapper.updateElement(null);
            if (activeConnection != null && KdbSettingsService.getInstance().getInspectorOptions().isScanOnConnect() && activeConnection.getState() != InstanceState.DISCONNECTED) {
                refreshInstance();
            }
        }
        updateEmptyText(null);
    }

    @Override
    public void dispose() {
        visible = false;
        instancesCache.clear();
        modelWrapper.dispose();
        toolWindowListener.dispose();
        scanner.dispose();
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
            return instance.getFirstGlobalDeclarations(canonicalName, GlobalSearchScope.allScope(project));
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
        if (activeConnection == null) {
            return;
        }
        scanner.scanInstance(activeConnection);
    }

    @Override
    public @Nullable InspectorToolState getState() {
        return settings;
    }

    @Override
    public void loadState(@NotNull InspectorToolState state) {
        settings.copyFom(state);
    }

    public static InspectorToolWindow getExist(@NotNull Project project) {
        return project.getServiceIfCreated(InspectorToolWindow.class);
    }

    static InspectorElement getElement(String name, InspectorElement ie) {
        for (InspectorElement child : ie.getChildren()) {
            final String canonicalName = child.getCanonicalName();
            if (canonicalName.equals(name)) {
                return child;
            }

            if (child instanceof NamespaceElement && name.startsWith(canonicalName)) {
                final InspectorElement element = getElement(name, child);
                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    static List<ExecutableElement> getSuggestions(String prefix, InspectorElement element) {
        return getSuggestions(prefix, element, ExecutableElement.class);
    }

    static <T extends ExecutableElement> List<T> getSuggestions(String prefix, InspectorElement element, Class<? extends T> type) {
        final List<T> res = new ArrayList<>();
        collectSuggestions(prefix, element, type, res);
        return res;
    }

    static <T extends ExecutableElement> void collectSuggestions(String prefix, InspectorElement ielement, Class<? extends T> type, List<T> res) {
        for (InspectorElement child : ielement.getChildren()) {
            final String canonicalName = child.getCanonicalName();
            if (canonicalName.startsWith(prefix) || (child instanceof NamespaceElement && prefix.startsWith(canonicalName))) {
                if (child instanceof NamespaceElement) {
                    collectSuggestions(prefix, child, type, res);
                } else if (type.isAssignableFrom(child.getClass())) {
                    res.add(type.cast(child));
                }
            }
        }
    }

    public boolean containsElement(String qualifiedName) {
        return getElement(qualifiedName) != null;
    }

    public InspectorElement getElement(String qualifiedName) {
        final InstanceElement ie = modelWrapper.getElement();
        if (ie == null || ie.getChildren().length == 0 || qualifiedName == null || qualifiedName.isEmpty()) {
            return null;
        }
        return getElement(qualifiedName, ie);
    }

    public <T extends ExecutableElement> List<T> getSuggestions(String prefix, Class<? extends T> type) {
        final InstanceElement ie = modelWrapper.getElement();
        if (ie == null || ie.getChildren().length == 0) {
            return List.of();
        }
        return getSuggestions(prefix, ie, type);
    }

    private static class NameResolver extends TreeVisitor.ByComponent<String, CanonicalElement> {
        private final boolean expand;

        public NameResolver(@NotNull String name, boolean expand) {
            super(name, o -> (CanonicalElement) StructureViewComponent.unwrapWrapper(o));
            this.expand = expand;
        }

        @Override
        protected boolean matches(@NotNull CanonicalElement element, @NotNull String name) {
            final String canonicalName = element.getCanonicalName();
            if (expand) {
                if (element instanceof NamespaceElement || element instanceof Group) {
                    return name.equals(canonicalName);
                }
                return false;
            }
            return name.equals(canonicalName);
        }

        @Override
        protected boolean contains(@NotNull CanonicalElement element, @NotNull String name) {
            if (element instanceof ExecutableElement) {
                return false;
            }

            if (element instanceof RootElement || element instanceof InstanceElement) {
                return true;
            }

            if (element instanceof Group) {
                return !expand && ((Group) element).getChildren().stream().map(e -> (CanonicalElement) e).map(CanonicalElement::getCanonicalName).anyMatch(s -> s.equals(name));
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
                    restoreElement(new NameResolver(expandedPath, true), TreeUtil::promiseExpand);
                }
            }

            if (selectedPath != null) {
                restoreElement(new NameResolver(selectedPath, false), TreeUtil::promiseSelect);
            }
        }

        private void restoreElement(NameResolver resolver, BiConsumer<Tree, TreeVisitor> consumer) {
            consumer.accept(tree, resolver);
        }

        private String getCanonicalName(TreePath path) {
            if (path == null) {
                return null;
            }
            final Object lastPathComponent = path.getLastPathComponent();
            final Object o = StructureViewComponent.unwrapWrapper(lastPathComponent);
            if (o instanceof CanonicalElement) {
                return ((CanonicalElement) o).getCanonicalName();
            }
            return null;
        }
    }

    private class ModelWrapper implements Disposable {
        private final TreeActionsOwner actionsOwner;
        private final AsyncTreeModel asyncTreeMode;
        private final InspectorTreeModel treeModel;
        private final SmartTreeStructure smartStructure;
        private final StructureTreeModel<SmartTreeStructure> structureModel;

        public ModelWrapper(Project project, InspectorToolState settings) {
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
                    if (!visible) {
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
            asyncTreeMode = new AsyncTreeModel(structureModel, false, this);
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
                structureModel.invalidateAsync();
            });
        }

        @Override
        public void dispose() {
            treeModel.dispose();
            asyncTreeMode.dispose();
            structureModel.dispose();
        }
    }

    private final class RefreshAction extends BgtAction {
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
                if (!b.isEmpty()) {
                    b.append(System.lineSeparator());
                }
                b.append(name);
            }
            CopyPasteManager.getInstance().setContents(new TextTransferable(b));
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
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