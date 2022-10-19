package org.kdb.inside.brains.view.inspector;

import com.intellij.ide.structureView.newStructureView.StructureViewComponent;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.StatusText;
import com.intellij.util.ui.tree.TreeUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.*;
import org.kdb.inside.brains.view.console.KdbConsoleToolWindow;
import org.kdb.inside.brains.view.inspector.model.ExecutableElement;
import org.kdb.inside.brains.view.inspector.model.InspectorTreeModel;
import org.kdb.inside.brains.view.inspector.model.NamespaceElement;

import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;

public class InspectorToolWindow implements Disposable {
    private final Project project;
    private static final String SCAN_QUERY = "" +
            "{\n" +
            "    / fix for root namespace\n" +
            "    v:$[x=y; x; ` sv x,y];\n" +
            "    / list of all inner namespaces - we get all keys and if the first value is (::) - that's namespace\n" +
            "    ns:l where (::)~'(first')(value') ` sv'v,'l:key[v] except `;\n" +
            "    / get all functions, tables, variables\n" +
            "    r:{system y,\" \",x}[string v;] each \"fav\";\n" +
            "    / return a dict with: table `name`size`meta\n" +
            "    tbs:{v:$[x=`; y; ` sv x,y]; (y;count value v;0!meta v)}[v;] each r[1];\n" +
            "    / variables are anything except namespaces and tables. We get `name`type for each\n" +
            "    vrs:{(y;type get $[x=`; y; ` sv x,y])}[v;] each r[2] except ns,r[1];\n" +
            "    / return final result\n" +
            "    (y;r[0];tbs;vrs;.z.s[v;] each ns)\n" +
            " }[`; `]";
    private final InspectorTreeModel structureViewModel = new InspectorTreeModel();
    private final KdbConnectionManager connectionManager;

    private final KdbConnectionListener connectionListener = new TheKdbConnectionListener();
    private InstanceConnection connection;

    public InspectorToolWindow(@NotNull Project project) {
        this.project = project;
        connectionManager = KdbConnectionManager.getManager(project);
    }

    public void initToolWindow(ToolWindowEx toolWindow) {
        final ContentManager cm = toolWindow.getContentManager();

        connection = connectionManager.getActiveConnection();
        connectionManager.addConnectionListener(connectionListener);

        StructureViewComponent component = new StructureViewComponent(null, structureViewModel, project, false) {
            @Override
            protected @NotNull ActionGroup createActionGroup() {
                final DefaultActionGroup actionGroup = new DefaultActionGroup();
                actionGroup.add(new AnAction("Refresh Instance", "Reloads the instance structure", KdbIcons.Inspector.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        refreshInstance();
                    }

                    @Override
                    public void update(@NotNull AnActionEvent e) {
                        final Presentation presentation = e.getPresentation();
                        presentation.setEnabled(connectionManager.getActiveConnection() != null);
                    }
                });
                actionGroup.addSeparator();
                actionGroup.addAll(super.createActionGroup());
                return actionGroup;
            }

            @Override
            protected boolean showScrollToFromSourceActions() {
                return false;
            }
        };

        final Tree tree = (Tree) component.getTree();
        final StatusText emptyText = tree.getEmptyText();
        emptyText.setText("Please connect to an instance ").appendText("to scan it's structure", StatusText.DEFAULT_ATTRIBUTES, e -> refreshInstance());
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent e) {
                return processDoubleClick(tree.getPathForLocation(e.getPoint().x, e.getPoint().y));
            }
        }.installOn(tree);

        final Content content = cm.getFactory().createContent(component, "", false);
        cm.addContent(content);
    }

    private boolean processDoubleClick(TreePath path) {
        if (path == null) {
            return false;
        }
        final AbstractTreeNode<?> node = TreeUtil.getAbstractTreeNode(path);
        if (node == null) {
            return false;
        }

        final Object value = node.getValue();
        if (value instanceof ExecutableElement) {
            final ExecutableElement ee = (ExecutableElement) value;
            KdbConsoleToolWindow.getInstance(project).execute(connection, ee.getQuery());
            return true;
        }
        return false;
    }

    private void refreshInstance() {
        if (connection == null) {
            return;
        }

        try {
            final KdbResult result = connection.query(new KdbQuery(SCAN_QUERY));
            structureViewModel.updateModel(new NamespaceElement((Object[]) result.getObject()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void instanceSelectionChanged(InstanceConnection activated) {
        connection = activated;
    }

    @Override
    public void dispose() {
        connectionManager.removeConnectionListener(connectionListener);
    }

    private class TheKdbConnectionListener implements KdbConnectionListener {
        @Override
        public void connectionActivated(InstanceConnection deactivated, InstanceConnection activated) {
            instanceSelectionChanged(activated);
        }

        @Override
        public void connectionStateChanged(InstanceConnection connection, InstanceState oldState, InstanceState newState) {
            if (newState != InstanceState.CONNECTED) {
                // make inactive
            }
        }
    }
}
