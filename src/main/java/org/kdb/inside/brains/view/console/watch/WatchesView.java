package org.kdb.inside.brains.view.console.watch;

import com.google.common.io.Resources;
import com.intellij.icons.AllIcons;
import com.intellij.ide.dnd.DnDEvent;
import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.dnd.DnDNativeTarget;
import com.intellij.openapi.CompositeDisposable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.BgtAction;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.KdbConsolePanel;
import org.kdb.inside.brains.view.console.table.TableResult;
import org.kdb.inside.brains.view.inspector.model.InstanceScanner;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class WatchesView extends JPanel implements DnDNativeTarget, DataProvider, Disposable {
    public static final DataKey<WatchesView> DATA_KEY = DataKey.create("kdb.watches.view");
    private static final Logger log = Logger.getInstance(WatchesView.class);
    private final KdbConsolePanel console;
    private final InstanceConnection connection;
    private final ComboBox<String> myComboBox;
    private final WatchesTreePanel watchesTreePanel;
    // This query can be defined in configs, for example, if 'value' is prohibited but for not it's not a case
    private final String query = getDefaultQuery();
    private final KdbOutputFormatter outputFormatter = KdbOutputFormatter.getDefault();
    private final CompositeDisposable myDisposables = new CompositeDisposable();
    private final Alarm updatingLabelAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, myDisposables);
    private boolean updating = false;

    public WatchesView(@NotNull Project project, @NotNull KdbConsolePanel console, @NotNull InstanceConnection connection) {
        this.console = console;
        this.connection = connection;

        // Must be first
        this.watchesTreePanel = createWatcherPanel(project);

        this.myComboBox = createComboBox(project);

        setLayout(new BorderLayout());
        add(myComboBox, BorderLayout.NORTH);
        add(watchesTreePanel.getMainPanel(), BorderLayout.CENTER);

        final WatchesTree tree = watchesTreePanel.getTree();
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    expandNodes(tree.getSelectedNodes());
                }
            }
        });

        Disposer.register(myDisposables, updatingLabelAlarm);
    }

    private static String getDefaultQuery() {
        try {
            final String name = "/org/kdb/inside/brains/watches.q";
            final URL resource = InstanceScanner.class.getResource(name);
            if (resource != null) {
                return Resources.toString(resource, StandardCharsets.UTF_8);
            }
            log.error("Scan query can't be loaded. Resource not found: " + name);
        } catch (Exception ex) {
            log.error("Scan query can't be loaded", ex);
        }
        return null;
    }

    public void addVariable() {
        editVariable(null);
    }

    public VariableNode addVariable(String expression) {
        return addVariable(expression, true);
    }

    public VariableNode addVariable(String expression, boolean navigate) {
        final VariableNode variable = getTree().addVariable(expression);
        if (variable == null) {
            return null;
        }

        refreshVariables(List.of(variable));

        final TreePath path = variable.getPath();
        if (navigate) {
            getTree().setSelectionPath(path);
        }
        return variable;
    }

    public void replaceVariables(List<String> watchesCache) {
        final List<VariableNode> nodes = getTree().replaceVariables(watchesCache);
        refreshVariables(nodes);
    }

    public void editVariable(@Nullable VariableNode node) {
        getTree().editVariable(node, m -> refreshVariables(List.of(m)));
    }

    public void removeAllVariables() {
        getTree().removeAllVariables();
    }

    public void removeVariables(List<VariableNode> variables) {
        getTree().removeVariables(variables);
    }

    public void refreshAllVariables() {
        refreshVariables(getTree().getAllVariables());
    }

    public void refreshSelectedVariables() {
        final List<VariableNode> selectedVariables = getSelectedVariables();
        if (!selectedVariables.isEmpty()) {
            refreshVariables(selectedVariables);
        }
    }

    public void moveWatchUp(List<VariableNode> nodes) {
        getTree().moveWatchUp(nodes);
    }

    public void moveWatchDown(List<VariableNode> nodes) {
        getTree().moveWatchDown(nodes);
    }

    public List<VariableNode> getAllVariables() {
        return getTree().getAllVariables();
    }

    public List<VariableNode> getSelectedVariables() {
        return getTree().getSelectedNodes();
    }

    public boolean isEmpty() {
        return getTree().getAllVariables().isEmpty();
    }

    public KdbOutputFormatter getOutputFormatter() {
        return outputFormatter;
    }

    public boolean isShowTypes() {
        return getTree().isShowTypes();
    }

    public void setShowTypes(boolean show) {
        getTree().setShowTypes(show);
    }

    public boolean isExpandable(VariableNode node) {
        final VariableValue value = node.getValue();
        if (value == null) {
            return false;
        }
        final KdbResult result = KdbResult.with(value.value());
        return TableResult.from(null, result) != null;
    }

    public void expandNodes(List<VariableNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        for (VariableNode node : nodes) {
            final VariableValue value = node.getValue();
            if (value != null) {
                final KdbQuery query = new KdbQuery("Watch: " + node.getExpression());
                final KdbResult result = KdbResult.with(value.value());
                final TableResult tableResult = TableResult.from(query, result);
                if (tableResult != null) {
                    console.showTableResult(query.getExpression(), tableResult);
                }
            }
        }
    }

    private WatchesTreePanel createWatcherPanel(Project project) {
        final WatchesTreePanel panel = new WatchesTreePanel(project, outputFormatter, myDisposables);

        final WatchesTree tree = panel.getTree();
        DnDManager.getInstance().registerTarget(this, tree);

        new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final Object contents = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
                if (contents instanceof String s) {
                    addVariable(s);
                }
            }
        }.registerCustomShortcutSet(CommonShortcuts.getPaste(), tree, myDisposables);

        return panel;
    }

    private ComboBox<String> createComboBox(Project project) {
        final VariableEditorComboBox box = new VariableEditorComboBox(project);
        final ComboBoxEditor editor = box.getEditor();

        final Runnable insertVariable = () -> {
            final String text = (String) editor.getItem();
            if (text.isBlank()) {
                return;
            }

            final VariableNode add = addVariable(text);
            if (add != null) {
                myComboBox.addItem(text);
            }
            editor.setItem("");
        };

        final JComponent editorComponent = (JComponent) editor.getEditorComponent();
        editorComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterStroke");
        editorComponent.getActionMap().put("enterStroke", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertVariable.run();
            }
        });

        final AnAction addToWatchesAction = new BgtAction("Add To Watches", null, AllIcons.Debugger.AddToWatch) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                insertVariable.run();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!((String) editor.getItem()).isBlank());
            }
        };

        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("KdbWatchPanelComboBox", new DefaultActionGroup(addToWatchesAction), true);
        toolbar.setReservePlaceAutoPopupIcon(false);
        toolbar.setTargetComponent(getTree());

        final JComponent component = toolbar.getComponent();
        component.setOpaque(false);
        box.setExtension(component);

        return box;
    }

    private void refreshVariables(List<VariableNode> variables) {
        try {
            if (updating) {
                return;
            }
            updating = true;

            // Change 'updating' status only in 1 second if it's still not set; otherwise it blinks too much
            updatingLabelAlarm.addRequest(() -> {
                if (updating) {
                    variables.forEach(VariableNode::updating);
                }
            }, 1000);

            final char[][] expressions = variables.stream().map(VariableNode::getExpression).map(String::toCharArray).toArray(char[][]::new);
            connection.query(new KdbQuery(query, new Object[]{expressions}), (r) -> {
                updating = false;
                updateResult(variables, r);
            });
        } catch (Exception ex) {
            updating = false;
            final VariableValue errMessage = new VariableValue(false, ex.getMessage());
            variables.forEach(v -> v.updated(errMessage));
        }
    }

    private void updateResult(List<VariableNode> variables, KdbResult result) {
        if (result.isError()) {
            final VariableValue errMessage = new VariableValue(false, ((Exception) result.getObject()).getMessage());
            variables.forEach(v -> v.updated(errMessage));
        } else {
            final Object[] res = (Object[]) result.getObject();
            for (int i = 0; i < res.length; i++) {
                final Object[] re = (Object[]) res[i];
                variables.get(i).updated(new VariableValue((Boolean) re[0], re[1]));
            }
        }
    }

    @Override
    public void dispose() {
        Disposer.dispose(myDisposables);
        DnDManager.getInstance().unregisterTarget(this, getTree());
    }

    private WatchesTree getTree() {
        return watchesTreePanel.getTree();
    }

    @Override
    public void drop(DnDEvent aEvent) {
        Object object = aEvent.getAttachedObject();
        if (object instanceof EventInfo) {
            final String text = ((EventInfo) object).getTextForFlavor(DataFlavor.stringFlavor);
            if (text != null && !text.isBlank()) {
                addVariable(text);
            }
        }
    }

    @Override
    public boolean update(DnDEvent aEvent) {
        final Object object = aEvent.getAttachedObject();

        boolean possible = false;
        if (object instanceof EventInfo) {
            possible = ((EventInfo) object).getTextForFlavor(DataFlavor.stringFlavor) != null;
        }
        aEvent.setDropPossible(possible, "Add To Watches");
        return true;
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (DATA_KEY.is(dataId)) {
            return this;
        }
        return null;
    }
}