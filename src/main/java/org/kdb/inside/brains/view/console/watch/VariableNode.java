package org.kdb.inside.brains.view.console.watch;

import com.intellij.util.Consumer;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collections;
import java.util.Enumeration;

public class VariableNode implements TreeNode {
    private final String expression;
    private final WatchesTreeRootNode myParent;
    private final Consumer<VariableNode> updateConsumer;
    private TreePath myPath;
    private VariableValue value;
    private boolean changed = false;
    private boolean updating = false;

    VariableNode(WatchesTreeRootNode parent, String expression, Consumer<VariableNode> updateConsumer) {
        this.myParent = parent;
        this.expression = expression;
        this.updateConsumer = updateConsumer;
    }

    public String getExpression() {
        return expression;
    }

    public VariableValue getValue() {
        return value;
    }

    public TreePath getPath() {
        if (myPath == null) {
            myPath = myParent.getPath().pathByAddingChild(this);
        }
        return myPath;
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean isUpdating() {
        return updating;
    }


    void updating() {
        updating = true;
        updateConsumer.consume(this);
    }

    void updated(VariableValue value) {
        changed = VariableValue.changed(this.value, value);
        this.value = value;
        this.updating = false;
        updateConsumer.consume(this);
    }

    // System methods
    @Override
    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public TreeNode getParent() {
        return myParent;
    }

    @Override
    public int getIndex(TreeNode node) {
        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Enumeration<? extends TreeNode> children() {
        return Collections.emptyEnumeration();
    }
}