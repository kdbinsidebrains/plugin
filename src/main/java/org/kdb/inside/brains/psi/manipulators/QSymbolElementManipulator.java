package org.kdb.inside.brains.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QSymbol;

public class QSymbolElementManipulator extends AbstractElementManipulator<QSymbol> {
    @Override
    public @Nullable QSymbol handleContentChange(@NotNull QSymbol element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
        final String replace = range.replace(element.getText(), newContent);
        final QSymbol sym = QPsiUtil.createSymbol(element.getProject(), replace);
        return (QSymbol) element.replace(sym);
    }
}
