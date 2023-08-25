package org.kdb.inside.brains.view.console.watch;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import com.intellij.xdebugger.impl.ui.tree.TreeInplaceEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.tree.TreePath;

class WatchInplaceEditor extends TreeInplaceEditor {
    private final Project project;
    private final WatchesTree tree;
    private final VariableNode node;
    private final Consumer<String> consumer;
    private final VariableEditorComboBox comboBox;
    private final String defaultValue;

    public WatchInplaceEditor(@NotNull Project project, @NotNull WatchesTree tree, @NotNull VariableNode node, Consumer<String> consumer) {
        this.project = project;
        this.tree = tree;
        this.node = node;

        this.consumer = consumer;
        this.comboBox = new VariableEditorComboBox(project);

        this.defaultValue = node.getExpression();
        this.comboBox.getComboBoxEditor().setItem(defaultValue);
    }

    @Override
    protected void onHidden() {
        final ComboPopup popup = comboBox.getPopup();
        if (popup != null && popup.isVisible()) {
            popup.hide();
        }
    }

    @Override
    public void cancelEditing() {
        super.cancelEditing();
        consumer.consume(defaultValue);
    }

    @Override
    public void doOKAction() {
        final String item = (String) comboBox.getComboBoxEditor().getItem();
        super.doOKAction();
        consumer.consume(item);
    }

    @Override
    protected void doPopupOKAction() {
        ComboPopup popup = comboBox.getPopup();
        if (popup != null && popup.isVisible()) {
            Object value = popup.getList().getSelectedValue();
            if (value instanceof String s) {
                comboBox.setItem(s);
            }
        }
        doOKAction();
    }

    @Override
    protected JTree getTree() {
        return tree;
    }

    @Override
    protected Project getProject() {
        return project;
    }

    @Override
    protected TreePath getNodePath() {
        return node.getPath();
    }

    @Override
    public Editor getEditor() {
        return comboBox.getComboBoxEditor().getEditor();
    }

    @Override
    public JComponent getEditorComponent() {
        return comboBox.getComboBoxEditor().getEditorComponent();
    }


    @Override
    protected JComponent createInplaceEditorComponent() {
        return comboBox;
    }

    @Override
    protected JComponent getPreferredFocusedComponent() {
        return comboBox;
    }
}