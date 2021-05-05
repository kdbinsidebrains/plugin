package org.kdb.inside.brains.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QVariable;
import org.kdb.inside.brains.psi.QVariableElement;

public class QVariableElementManipulator extends AbstractElementManipulator<QVariable> {
    @Override
    public @Nullable QVariable handleContentChange(@NotNull QVariable element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
        final String replace = range.replace(element.getText(), newContent);
        return (QVariable) element.replace(QVariableElement.createVariable(element.getProject(), replace));
    }
}
