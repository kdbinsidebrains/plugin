package org.kdb.inside.brains.view.console.watch;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Consumer;
import com.intellij.util.ui.TextTransferable;
import com.intellij.util.ui.UIUtil;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.util.List;
import java.util.stream.Stream;

// See XDebuggerTree for original Java implementation
class WatchesTree extends DnDAwareTree implements Disposable {
    private static final TransferHandler DEFAULT_TRANSFER_HANDLER = new TransferHandler() {
        @Override
        protected Transferable createTransferable(JComponent c) {
            if (!(c instanceof WatchesTree tree)) {
                return null;
            }
            TreePath[] selectedPaths = tree.getSelectionPaths();
            if (selectedPaths == null || selectedPaths.length == 0) {
                return null;
            }

            StringBuilder plainBuf = new StringBuilder();
            StringBuilder htmlBuf = new StringBuilder();
            htmlBuf.append("<html>\n<body>\n<ul>\n");
            for (TreePath path : selectedPaths) {
                htmlBuf.append("  <li>");
                final Object node = path.getLastPathComponent();
                if (node instanceof VariableNode n) {
                    final String text = n.getExpression();
                    plainBuf.append(text);
                    htmlBuf.append(text);
                }
                plainBuf.append('\n');
                htmlBuf.append("</li>\n");
            }
            // remove the last newline
            plainBuf.setLength(plainBuf.length() - 1);
            htmlBuf.append("</ul>\n</body>\n</html>");
            return new TextTransferable(htmlBuf, plainBuf);
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }
    };
    private final Project project;
    private final WatchesTreeRenderer renderer;

    private final DefaultTreeModel treeModel;
    private final WatchesTreeRootNode rootNode = new WatchesTreeRootNode();
    private boolean showTypes = true;

    public WatchesTree(Project project, KdbOutputFormatter outputFormatter) {
        this.project = project;
        setOpaque(true);
        setRootVisible(false);
        setShowsRootHandles(true);

        renderer = new WatchesTreeRenderer(showTypes, outputFormatter);
        setCellRenderer(renderer);
        getEmptyText().setText("No watches");
        setModel(treeModel = new DefaultTreeModel(rootNode));

        new TreeSpeedSearch(this, false, path -> {
            String text = null;
            if (path != null) {
                final Object node = path.getLastPathComponent();
                if (node instanceof VariableNode n) {
                    text = n.getExpression();
                }
            }
            return StringUtil.notNullize(text);
        });
        PopupHandler.installPopupMenu(this, "Kdb.WatchesView", "WatchesTreePopup");

        setTransferHandler(DEFAULT_TRANSFER_HANDLER);
    }

    public List<VariableNode> getAllVariables() {
        return rootNode.getVariables();
    }

    public List<VariableNode> getSelectedNodes() {
        final TreePath[] selectionPaths = getSelectionPaths();
        if (selectionPaths == null || selectionPaths.length == 0) {
            return List.of();
        }
        return Stream.of(selectionPaths)
                .map(TreePath::getLastPathComponent)
                .filter(o -> o instanceof VariableNode)
                .map(o -> (VariableNode) o)
                .toList();
    }


    public VariableNode addVariable(String expression) {
        final VariableNode variableNode = new VariableNode(rootNode, expression, this::nodeUpdate);
        final int[] indexes = rootNode.add(variableNode);
        if (indexes != null) {
            treeModel.nodesWereInserted(rootNode, indexes);
            return variableNode;
        }
        return null;
    }

    public void editVariable(VariableNode node, Consumer<VariableNode> consumer) {
        VariableNode messageNode;
        int index = node == null ? -1 : rootNode.getIndex(node);
        if (index == -1) {
            final List<VariableNode> selectedNodes = getSelectedNodes();
            int selectedIndex = selectedNodes.isEmpty() ? -1 : rootNode.getIndex(selectedNodes.get(0));
            int targetIndex = selectedIndex == -1 ? rootNode.getChildCount() : selectedIndex + 1;

            messageNode = new VariableNode(rootNode, "", this::nodeUpdate);
            final int[] add = rootNode.add(targetIndex, messageNode);
            treeModel.nodesWereInserted(rootNode, add);
            setSelectionRows(ArrayUtilRt.EMPTY_INT_ARRAY);
        } else {
            messageNode = node;
        }

        new WatchInplaceEditor(project, this, messageNode, s -> {
            if (s == null || s.isBlank()) {
                final int[] remove = rootNode.remove(messageNode);
                treeModel.nodesWereRemoved(rootNode, remove, new Object[]{messageNode});
            } else if (!s.equals(messageNode.getExpression())) {
                final VariableNode variableNode = new VariableNode(rootNode, s, this::nodeUpdate);
                final int[] replace = rootNode.replace(messageNode, variableNode);
                treeModel.nodesChanged(rootNode, replace);
                consumer.consume(variableNode);
                // That's required to update TreePath object in SelectionModel
                setSelectionRows(replace);
            }
        }).show();
    }

    public List<VariableNode> replaceVariables(List<String> variables) {
        rootNode.clear();
        for (String variable : variables) {
            if (variable != null && !variable.isBlank()) {
                rootNode.add(new VariableNode(rootNode, variable, this::nodeUpdate));
            }
        }
        treeModel.nodeStructureChanged(rootNode);
        return rootNode.getVariables();
    }

    public void removeVariables(List<VariableNode> nodes) {
        final int[] indexes = rootNode.remove(nodes);
        if (indexes != null) {
            treeModel.nodesWereRemoved(rootNode, indexes, nodes.toArray());
        }
    }

    public void removeAllVariables() {
        rootNode.clear();
        treeModel.reload();
    }

    public void moveWatchUp(List<VariableNode> nodes) {
        final int[] indexes = rootNode.moveUp(nodes);
        treeModel.nodeStructureChanged(rootNode);
        setSelectionRows(indexes);
    }

    public void moveWatchDown(List<VariableNode> nodes) {
        int[] indexes = rootNode.moveDown(nodes);
        treeModel.nodeStructureChanged(rootNode);
        setSelectionRows(indexes);
    }

    @Override
    public void dispose() {
        setModel(null);
        treeModel.setRoot(null);
        setCellRenderer(null);
        UIUtil.dispose(this);
        setLeadSelectionPath(null);
        setAnchorSelectionPath(null);
        accessibleContext = null;
    }

    private void nodeUpdate(VariableNode node) {
        final int index = rootNode.getIndex(node);
        if (index != -1) {
            treeModel.nodesChanged(rootNode, new int[]{index});
        }
    }

    public boolean isShowTypes() {
        return showTypes;
    }

    public void setShowTypes(boolean show) {
        showTypes = show;
        renderer.setShowTypes(show);
    }
}