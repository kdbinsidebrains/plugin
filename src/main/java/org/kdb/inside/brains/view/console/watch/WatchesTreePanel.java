package org.kdb.inside.brains.view.console.watch;

import com.intellij.ide.dnd.DnDAction;
import com.intellij.ide.dnd.DnDDragStartBean;
import com.intellij.ide.dnd.DnDSource;
import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.JBUI;
import com.intellij.xdebugger.XDebuggerBundle;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.awt.*;

class WatchesTreePanel implements DnDSource {
    private final JPanel myMainPanel;
    private final WatchesTree myTree;

    public WatchesTreePanel(@NotNull Project project, @NotNull KdbOutputFormatter outputFormatter, @NotNull Disposable parentDisposable) {
        myTree = new WatchesTree(project, outputFormatter);
        myTree.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
        myMainPanel = new JPanel(new BorderLayout());

        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
        scrollPane.setBorder(JBUI.Borders.empty());
        myMainPanel.add(scrollPane, BorderLayout.CENTER);

        Disposer.register(parentDisposable, myTree);
        Disposer.register(parentDisposable, myMainPanel::removeAll);
    }

    public JPanel getMainPanel() {
        return myMainPanel;
    }

    public WatchesTree getTree() {
        return myTree;
    }

    @Override
    public boolean canStartDragging(DnDAction action, @NotNull Point dragOrigin) {
        return getNodesToDrag().length > 0;
    }

    @Override
    public DnDDragStartBean startDragging(DnDAction action, @NotNull Point dragOrigin) {
        return new DnDDragStartBean(getNodesToDrag());
    }

    private VariableNode[] getNodesToDrag() {
        return myTree.getSelectedNodes(VariableNode.class, null);
    }

    @Override
    public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, @NotNull DnDDragStartBean bean) {
        VariableNode[] nodes = getNodesToDrag();
        if (nodes.length == 1) {
            return DnDAwareTree.getDragImage(myTree, nodes[0].getPath(), dragOrigin);
        }
        return DnDAwareTree.getDragImage(myTree, XDebuggerBundle.message("xdebugger.drag.text.0.elements", nodes.length), dragOrigin);
    }
}