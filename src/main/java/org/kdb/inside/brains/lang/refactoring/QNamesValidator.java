package org.kdb.inside.brains.lang.refactoring;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;

public class QNamesValidator implements NamesValidator {
    @Override
    public boolean isKeyword(@NotNull String name, Project project) {
        return QLanguage.isKeyword(name);
    }

    @Override
    public boolean isIdentifier(@NotNull String name, Project project) {
        return QLanguage.isIdentifier(name);
    }
}
