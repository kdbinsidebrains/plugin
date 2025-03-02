package org.kdb.inside.brains.lang.exclusions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;

import javax.swing.*;
import java.util.function.Predicate;

public class UndefinedExclusionDialog extends DialogWrapper {
    private final Predicate<String> existPredicate;
    private JPanel myComponent;
    private JCheckBox regexCheckBox;
    private JTextField variableNameField;

    public UndefinedExclusionDialog(@Nullable Project project, Predicate<String> existPredicate) {
        super(project, false);
        this.existPredicate = existPredicate;
        setTitle("Undefined Variable Exclusion");
        init();

        final ComponentValidator validator = new ComponentValidator(getDisposable())
                .withValidator(this::doValidate)
                .andRegisterOnDocumentListener(variableNameField)
                .installOn(variableNameField);

        regexCheckBox.addActionListener(e -> validator.revalidate());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return myComponent;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return variableNameField;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        final String text = variableNameField.getText().trim();
        if (text.isEmpty()) {
            return new ValidationInfo("Variable name can't be empty", variableNameField);
        }
        if (!regexCheckBox.isSelected() && !QLanguage.isIdentifier(text)) {
            return new ValidationInfo("Variable is not an identifier", variableNameField);
        }
        if (existPredicate.test(text)) {
            return new ValidationInfo("Exclusion already defined", variableNameField);
        }
        return super.doValidate();
    }

    public UndefinedExclusion show(UndefinedExclusion oldValue) {
        if (oldValue == null) {
            setOKButtonText("Add");
        } else {
            setOKButtonText("Modify");
            regexCheckBox.setSelected(oldValue.regex());
            variableNameField.setText(oldValue.name());
        }

        if (showAndGet()) {
            return new UndefinedExclusion(variableNameField.getText().trim(), regexCheckBox.isSelected());
        }
        return null;
    }
}