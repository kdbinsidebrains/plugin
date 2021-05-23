package org.kdb.inside.brains.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarReference;
import org.kdb.inside.brains.psi.QVariable;

public class QVariableElementManipulator extends AbstractElementManipulator<QVariable> {
    @Override
    public @Nullable QVariable handleContentChange(@NotNull QVariable element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
        final String replace = range.replace(element.getText(), newContent);

        final QVariable var;
        if (element instanceof QVarReference) {
            var = QPsiUtil.createVarReference(element.getProject(), replace);
        } else {
            var = QPsiUtil.createVarDeclaration(element.getProject(), replace);
        }
        return (QVariable) element.replace(var);
    }
}
