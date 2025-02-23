package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;

import javax.swing.*;

public class CustomScriptPanel {
    private JPanel myComponent;

    private JScrollPane scriptPanel;
    private LanguageTextField scriptTextArea;

    public CustomScriptPanel() {
    }

    void init(@Nullable Project project) {
        scriptTextArea = new LanguageTextField(QLanguage.INSTANCE, project, "", false);
        scriptPanel.setViewportView(scriptTextArea);
    }

    public JComponent getComponent() {
        return myComponent;
    }

    public String getText() {
        return scriptTextArea.getText();
    }

    public void setText(String text) {
        scriptTextArea.setText(text);
    }

    public void setEnabled(boolean enabled) {
        scriptTextArea.setEnabled(enabled);
    }
}