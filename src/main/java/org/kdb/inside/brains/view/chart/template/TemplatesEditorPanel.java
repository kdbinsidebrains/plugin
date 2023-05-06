package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.MasterDetailsStateService;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class TemplatesEditorPanel extends MasterDetailsComponent {
    private static final String STATE_KEY = "KdbChartTemplatesState";
    private final Project project;
    private final ChartTemplatesService templateService;

    public TemplatesEditorPanel(@NotNull Project project) {
        this.project = project;
        initTree();
        templateService = ChartTemplatesService.getService(project);
    }

    @Override
    public String getDisplayName() {
        return "Chart Templates Manager";
    }

    @Override
    protected String getEmptySelectionString() {
        return "Select a template to view or edit its details here";
    }

    @Override
    protected String getComponentStateKey() {
        return STATE_KEY;
    }

    @Override
    protected Dimension getPanelPreferredSize() {
        return JBUI.size(400, 200);
    }

    @Override
    protected MasterDetailsStateService getStateService() {
        return MasterDetailsStateService.getInstance(project);
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        return true;
    }

    protected List<AnAction> createActions(boolean fromPopup) {
        final ArrayList<AnAction> result = new ArrayList<>();
        result.add(new MyDeleteAction());
        return result;
    }

    @Override
    protected void initTree() {
        super.initTree();
        myTree.setShowsRootHandles(false);
        new TreeSpeedSearch(myTree, true, treePath -> ((MyNode) treePath.getLastPathComponent()).getDisplayName());

        final ColoredTreeCellRenderer cellRenderer = (ColoredTreeCellRenderer) myTree.getCellRenderer();

        myTree.setCellRenderer((tree, value, selected, expanded, leaf, row, hasFocus) -> {
            cellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (value instanceof MyNode && ((MyNode) value).getConfigurable() != null) {
                final MyNode node = (MyNode) value;
                final Object editableObject = node.getConfigurable();
                if (!(editableObject instanceof TemplateConfigurable)) {
                    return cellRenderer;
                }

                final ChartTemplate template = ((TemplateConfigurable) editableObject).getEditableObject();
                cellRenderer.setIcon(template.getIcon());
            }
            return cellRenderer;
        });
    }

    private java.util.List<ChartTemplate> collectTemplates() {
        final int childCount = myRoot.getChildCount();
        final List<ChartTemplate> res = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            final MyNode node = (MyNode) myRoot.getChildAt(i);
            res.add(((TemplateConfigurable) node.getConfigurable()).getEditableObject());
        }
        return res;
    }

    private boolean containsName(String name) {
        final Enumeration<TreeNode> children = myRoot.children();
        while (children.hasMoreElements()) {
            final NamedConfigurable<?> configurable = ((MyNode) children.nextElement()).getConfigurable();
            if (name.equals(configurable.getDisplayName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();

        templateService.setTemplates(collectTemplates());
    }

    @Override
    public void reset() {
        myRoot.removeAllChildren();

        templateService.getTemplates().forEach(t -> {
            myRoot.add(new MyNode(new TemplateConfigurable(t, TemplatesEditorPanel.this::containsName, TREE_UPDATER)));
        });
        ((DefaultTreeModel) myTree.getModel()).reload(myRoot);
    }
}