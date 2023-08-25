package org.kdb.inside.brains.view.console.watch;

import com.intellij.util.containers.ContainerUtil;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

class WatchesTreeRootNode implements TreeNode {
    private final List<VariableNode> variables = new ArrayList<>();
    private TreePath myPath;

    WatchesTreeRootNode() {
    }

    int[] add(VariableNode variable) {
        if (variables.stream().map(VariableNode::getExpression).anyMatch(s -> s.equals(variable.getExpression()))) {
            return null;
        }
        variables.add(variable);
        return new int[]{variables.size() - 1};
    }

    int[] add(int index, VariableNode variable) {
        variables.add(index, variable);
        return new int[]{index};
    }

    int[] remove(VariableNode node) {
        return remove(List.of(node));
    }

    int[] remove(List<VariableNode> vars) {
        final int[] res = vars.stream().mapToInt(this.variables::indexOf).toArray();
        variables.removeAll(vars);
        return res;
    }

    int[] replace(VariableNode oldNode, VariableNode newNode) {
        final int i = variables.indexOf(oldNode);
        if (i != -1) {
            variables.set(i, newNode);
            return new int[]{i};
        }
        return null;
    }

    void clear() {
        variables.clear();
    }

    List<VariableNode> getVariables() {
        return variables;
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return variables.get(childIndex);
    }

    @Override
    public int getChildCount() {
        return variables.size();
    }

    @Override
    public TreeNode getParent() {
        return null;
    }

    @Override
    public int getIndex(TreeNode node) {
        return variables.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.enumeration(variables);
    }

    public TreePath getPath() {
        if (myPath == null) {
            myPath = new TreePath(this);
        }
        return myPath;
    }

    public int[] moveUp(List<VariableNode> nodes) {
        int i = 0;
        final int[] res = new int[nodes.size()];
        for (VariableNode node : nodes) {
            final int oldIndex = getIndex(node);
            final int newIndex = oldIndex - 1;
            res[i++] = newIndex;
            if (oldIndex > 0) {
                ContainerUtil.swapElements(variables, oldIndex, newIndex);
            }
        }
        return res;
    }

    public int[] moveDown(List<VariableNode> nodes) {
        int i = 0;
        final int[] res = new int[nodes.size()];
        final List<VariableNode> list = new ArrayList<>(nodes);
        Collections.reverse(list);
        for (VariableNode node : list) {
            int oldIndex = getIndex(node);
            final int newIndex = oldIndex + 1;
            if (oldIndex < variables.size() - 1) {
                ContainerUtil.swapElements(variables, oldIndex, newIndex);
            }
            res[i++] = newIndex;
        }
        return res;
    }
}