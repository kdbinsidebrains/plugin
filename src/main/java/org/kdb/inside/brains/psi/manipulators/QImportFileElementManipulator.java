package org.kdb.inside.brains.psi.manipulators;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QImportFile;

import static org.kdb.inside.brains.QFileType.createFactoryFile;

public class QImportFileElementManipulator extends AbstractElementManipulator<QImportFile> {
    @Override
    public @Nullable QImportFile handleContentChange(@NotNull QImportFile element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
        final String replace = range.replace(element.getText(), newContent);
        return (QImportFile) element.replace(createImportPath(element.getProject(), replace));
    }

    private static QImportFile createImportPath(Project project, String path) {
        final QFile file = createFactoryFile(project, path.startsWith("\\l ") ? path : "\\l " + path);
        return PsiTreeUtil.findChildOfType(file, QImportFile.class);
    }
}
