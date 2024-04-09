package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CreateTemplateDialog extends DialogWrapper {
    private final ChartTemplate template;
    private final ChartTemplatesService templatesService;

    private final JBTextField nameField = new JBTextField();
    private final JBTextField descriptionField = new JBTextField();
    private final JBCheckBox quickAction = new JBCheckBox("Add to quick popup actions");

    public CreateTemplateDialog(@NotNull Project project, @NotNull ChartTemplate template) {
        super(project, false, IdeModalityType.IDE);
        setTitle("Creating Chart Template");

        this.template = template;
        templatesService = ChartTemplatesService.getService(project);

        nameField.setText(template.getName());
        descriptionField.setText(template.getDescription());
        quickAction.setSelected(template.isQuickAction());

        setOKActionEnabled(false);
        setOKButtonText("Create");

        init();
        initValidation();
    }

    @Override
    protected void doOKAction() {
        if (getOKAction().isEnabled()) {
            storeValues();
            close(OK_EXIT_CODE);
        }
    }

    protected void storeValues() {
        template.setName(nameField.getText().trim());
        template.setDescription(descriptionField.getText().trim());
        template.setQuickAction(quickAction.isSelected());
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        final String text = nameField.getText().trim();
        if (text.isEmpty()) {
            return new ValidationInfo("Name can't be null", nameField);
        }
        if (templatesService.containsName(text)) {
            return new ValidationInfo("Template with the same name already exist", nameField);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Name:", nameField)
                .addLabeledComponent("Description:", descriptionField)
                .addComponentToRightColumn(quickAction)
                .getPanel();
    }
}
