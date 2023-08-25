package org.kdb.inside.brains.view.console.watch;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorComboBoxEditor;
import com.intellij.xdebugger.impl.ui.DebuggerCopyPastePreprocessor;
import com.intellij.xdebugger.impl.ui.XDebuggerEmbeddedComboBox;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

// See XDebuggerComboBoxEditor
public class VariableEditorComboBox extends XDebuggerEmbeddedComboBox<String> {
    private final Project project;
    private final TheEditorComboBoxEditor comboBoxEditor;

    public VariableEditorComboBox(Project project) {
        this.project = project;
        comboBoxEditor = new TheEditorComboBoxEditor();

        setEditable(true);
        setEditor(comboBoxEditor);

        setMaximumRowCount(10);
    }

    public TheEditorComboBoxEditor getComboBoxEditor() {
        return comboBoxEditor;
    }

    public class TheEditorComboBoxEditor extends EditorComboBoxEditor {
        public TheEditorComboBoxEditor() {
            super(project, QFileType.INSTANCE);
        }

        @Override
        protected void onEditorCreate(EditorEx editor) {
            super.onEditorCreate(editor);
            editor.putUserData(DebuggerCopyPastePreprocessor.REMOVE_NEWLINES_ON_PASTE, true);
            editor.setPlaceholder("Type variable or expression and press Enter to add to the watch list");
        }

        @NotNull
        @Override
        public Object getItem() {
            return ((Document) super.getItem()).getText();
        }

        @Override
        public void setItem(Object anObject) {
            if (anObject == null) {
                anObject = "";
            }
            if (anObject.equals(getItem())) {
                return;
            }
            final String s = (String) anObject;
            WriteCommandAction.writeCommandAction(project).run(() -> getDocument().setText(s));

            final Editor editor = getEditor();
            if (editor != null) {
                editor.getCaretModel().moveToOffset(s.length());
            }
        }
    }
}

