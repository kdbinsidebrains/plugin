package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Predicate;

public class TemplateConfigurable extends NamedConfigurable<ChartTemplate> {
    private String name;

    private final ChartTemplate template;
    private final Predicate<String> nameChecker;
    private final JTextPane configDescription = new JTextPane();
    private final JBTextField descriptionField = new JBTextField();
    private final JBCheckBox quickAction = new JBCheckBox("Add to quick popup actions");

    public TemplateConfigurable(@NotNull ChartTemplate template, @NotNull Predicate<String> nameChecker, @Nullable Runnable updateTree) {
        super(true, updateTree);
        this.template = template;
        this.nameChecker = nameChecker;

        name = template.getName();
        descriptionField.setText(template.getDescription());
        quickAction.setSelected(template.isQuickAction());

        configDescription.setEditable(false);
        configDescription.setContentType("text/html");
        configDescription.setText(template.getConfig().toHumanString());

        updateName();
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public void setDisplayName(String name) {
        this.name = name;
    }

    @Override
    public ChartTemplate getEditableObject() {
        return template;
    }

    @Override
    public String getBannerSlogan() {
        return "Chart Template " + getDisplayName();
    }

    @Override
    public JComponent createOptionsPanel() {
        final JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Description:", descriptionField)
                .addComponentToRightColumn(quickAction)
                .addLabeledComponentFillVertically("Configuration:", new JBScrollPane(configDescription))
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(0, 10, 0, 10));
        return panel;
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(template.getName(), name) || !Objects.equals(template.getDescription(), descriptionField.getText()) || template.isQuickAction() != quickAction.isSelected();
    }

    @Override
    public void apply() {
        template.setName(getDisplayName().trim());
        template.setDescription(descriptionField.getText().trim());
        template.setQuickAction(quickAction.isSelected());
        updateName();
    }

    @Override
    protected void checkName(@NonNls @NotNull String name) throws ConfigurationException {
        super.checkName(name);
        if (!template.getName().equals(name) && nameChecker.test(name)) {
            throw new ConfigurationException("Template with the same already exist");
        }
    }
}
