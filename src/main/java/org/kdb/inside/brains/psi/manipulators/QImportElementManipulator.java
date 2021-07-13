package org.kdb.inside.brains.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QImport;

import static org.kdb.inside.brains.QFileType.createFactoryFile;

public class QImportElementManipulator extends AbstractElementManipulator<QImport> {
    @Override
    public @Nullable QImport handleContentChange(@NotNull QImport element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
        final String replace = range.replace(element.getText(), newContent);

        final QFile file = createFactoryFile(element.getProject(), replace);
        final QImport childOfType = PsiTreeUtil.findChildOfType(file, element.getClass());
        if (childOfType == null) {
            return element;
        }
        return (QImport) element.replace(childOfType);
    }
}
