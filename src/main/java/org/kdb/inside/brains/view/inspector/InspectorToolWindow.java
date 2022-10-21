package org.kdb.inside.brains.view.inspector;

import com.intellij.ide.CopyProvider;
import com.intellij.ide.structureView.ModelListener;
import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.structureView.newStructureView.TreeActionWrapper;
import com.intellij.ide.structureView.newStructureView.TreeModelWrapper;
import com.intellij.ide.util.FileStructurePopup;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.ide.util.treeView.smartTree.SmartTreeStructure;
import com.intellij.ide.util.treeView.smartTree.TreeAction;
import com.intellij.ide.util.treeView.smartTree.TreeElementWrapper;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.tree.TreeUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionListener;
import org.kdb.inside.brains.core.KdbConnectionManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.ide.structureView.newStructureView.StructureViewComponent.registerAutoExpandListener;


@State(name = "KdbInstanceInspector", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class InspectorToolWindow extends SimpleToolWindowPanel implements PersistentStateComponent<InspectorToolState>, KdbConnectionListener, InstanceScanner.ScanListener, DataProvider, Disposable {
    public static final String PLACE = "Kdb.InstanceInspectorToolbar";

    private final Project project;
    private static final DateTimeFormatter STATUS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private InstanceConnection connection;
    private final InstanceScanner scanner;
    private final KdbConnectionManager connectionManager;
    private final Tree tree;
    private final CopyProvider copyProvider;
    private final SmartTreeStructure smartStructure;
    private final StructureTreeModel<SmartTreeStructure> structureModel;
    private final InspectorTreeModel inspectorModel = new InspectorTreeModel();
    private final Map<InstanceConnection, InstanceElement> instancesCache = new HashMap<>();

    private final JLabel statusBar = new JLabel("", JLabel.RIGHT);
    private final InspectorToolState settings = new InspectorToolState(this::rebuild);
    private boolean visible;
    private boolean disposed;

    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    public InspectorToolWindow(@NotNull Project project) {
        super(true);
        this.project = project;
        this.scanner = new InstanceScanner(project, this);
        connectionManager = KdbConnectionManager.getManager(project);

        final TreeModelWrapper myTreeModelWrapper = new TreeModelWrapper(inspectorModel, settings);
        Disposer.register(this, myTreeModelWrapper);

        smartStructure = new SmartTreeStructure(project, myTreeModelWrapper) {
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

            @Override
            public String toString() {
                return "inspector tree structure(model=" + inspectorModel + ")";
            }
        };

        structureModel = new StructureTreeModel<>(smartStructure, this);
        tree = new Tree(new AsyncTreeModel(structureModel, this));
        tree.setRootVisible(true);
        tree.setCellRenderer(new NodeRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);

        copyProvider = new CopyProvider() {
            @Override
            public void performCopy(@NotNull DataContext dataContext) {
                final TreePath[] selectionPaths = tree.getSelectionPaths();
                if (selectionPaths == null) {
                    return;
                }

                final StringBuilder b = new StringBuilder();
                for (TreePath path : selectionPaths) {
                    final String name = getCanonicalName(path);
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
        };

        registerAutoExpandListener(tree, inspectorModel);

        final ModelListener modelListener = this::rebuild;
        myTreeModelWrapper.addModelListener(modelListener);

        Disposer.register(this, myTreeModelWrapper);
        Disposer.register(this, () -> {
            myTreeModelWrapper.removeModelListener(modelListener);
        });

        TreeUtil.installActions(tree);

        new TreeSpeedSearch(tree, treePath -> {
            Object userObject = TreeUtil.getLastUserObject(treePath);
            return userObject != null ? FileStructurePopup.getSpeedSearchText(userObject) : null;
        });

        setContent(ScrollPaneFactory.createScrollPane(tree));
        setToolbar(createToolbar());
        createStatusBar();

        updateEmptyText(null);

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                return executeSelectedPath(tree.getPathForLocation(e.getPoint().x, e.getPoint().y));
            }
        }.installOn(tree);

        tree.registerKeyboardAction(event -> executeSelectedPath(tree.getSelectionPath()), ENTER, JComponent.WHEN_FOCUSED);
    }

    @Nullable
    private static String getCanonicalName(TreePath path) {
        final Object[] objects = path.getPath();
        if (objects.length <= 1) {
            return null;
        }

        final int count = objects.length - 1;
        final Object last = StructureViewComponent.unwrapWrapper(objects[count]);
        if (!(last instanceof ExecutableElement)) {
            return null;
        }

        if (last instanceof TableElement && ((TableElement) last).isHistorical()) {
            return null;
        }

        final StringBuilder b = new StringBuilder();
        for (int i = 1; i < count; i++) {
            final Object o = StructureViewComponent.unwrapWrapper(objects[i]);
            if (o instanceof NamespaceElement) {
                b.append(".").append(((NamespaceElement) o).getName());
            }
        }

        final ExecutableElement ee = (ExecutableElement) last;
        return b.length() == 0 ? ee.getName() : b.append(".").append(ee.getName()).toString();
    }

    private void createStatusBar() {
        statusBar.setBorder(JBUI.Borders.empty(5));

        add(statusBar, BorderLayout.SOUTH);
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        final ContentManager cm = toolWindow.getContentManager();
        final Content content = cm.getFactory().createContent(this, null, false);
        cm.addContent(content);

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

        final AnAction refreshAction = new AnAction("Refresh Instance", "Reloads the instance structure", KdbIcons.Inspector.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                refreshInstance();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                final Presentation presentation = e.getPresentation();
                presentation.setEnabled(connectionManager.getActiveConnection() != null);
            }
        };

        result.add(refreshAction);
        result.addSeparator();

        final List<TreeAction[]> actionsList = List.of(inspectorModel.getSorters(), inspectorModel.getGroupers(), inspectorModel.getFilters());
        for (TreeAction[] actions : actionsList) {
            if (actions.length != 0) {
                result.addSeparator();
                for (TreeAction action : actions) {
                    result.add(new TreeActionWrapper(action, settings));
                }
            }
        }

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PLACE, result, true);
        toolbar.setTargetComponent(tree);
        return toolbar.getComponent();
    }

    private boolean executeSelectedPath(TreePath path) {
        if (path == null) {
            return false;
        }

        final String query = getCanonicalName(path);
        if (query == null) {
            return false;
        }
        KdbConsoleToolWindow.getInstance(project).execute(connection, query);
        return true;
    }

    @Override
    public void scanFailed(InstanceConnection connection, Exception exception) {
        inspectorModel.updateModel(null);
        updateEmptyText(exception);

    }

    @Override
    public void scanFinished(InstanceConnection connection, InstanceElement result) {
        inspectorModel.updateModel(result);
        instancesCache.put(connection, result);
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
        if (PlatformDataKeys.COPY_PROVIDER.is(dataId)) {
            return copyProvider;
        }

        if (CommonDataKeys.PROJECT.is(dataId)) {
            return project;
        }
        return null;
    }

    @Override
    public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
        if (!visible) {
            return;
        }

        connection = activated;

        final InstanceElement cached = instancesCache.get(connection);
        if (cached != null) {
            inspectorModel.updateModel(cached);
        } else {
            inspectorModel.updateModel(null);
            updateEmptyText(null);

            if (connection != null && KdbSettingsService.getInstance().getInspectorOptions().isScanOnConnect() && connection.getState() != InstanceState.DISCONNECTED) {
                refreshInstance();
            }
        }
    }

    @Override
    public void dispose() {
        visible = false;
        disposed = true;
        connectionManager.removeConnectionListener(this);
    }

    private void rebuild() {
        final InstanceElement instanceElement = inspectorModel.getInstanceElement();
        if (instanceElement == null) {
            statusBar.setText("");
        } else {
            statusBar.setText("Updated " + STATUS_FORMATTER.format(instanceElement.getResult().getTime()));
        }

        structureModel.getInvoker().invoke(() -> {
            smartStructure.rebuildTree();
            structureModel.invalidate();
        });
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
}
