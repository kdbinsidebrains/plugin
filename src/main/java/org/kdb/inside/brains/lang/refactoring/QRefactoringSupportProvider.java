package org.kdb.inside.brains.lang.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;

public final class QRefactoringSupportProvider extends RefactoringSupportProvider {
    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
        return element instanceof QVariable || element instanceof QSymbol;
    }

    @Override
    public @NotNull RefactoringActionHandler getIntroduceVariableHandler() {
        return new QIntroduceVariableHandler(QIntroduceVariableHandler.VarType.VARIABLE);
    }

    @Override
    public @NotNull RefactoringActionHandler getIntroduceFieldHandler() {
        return new QIntroduceVariableHandler(QIntroduceVariableHandler.VarType.FIELD);
    }
/*

    @Override
    public @NotNull RefactoringActionHandler getIntroduceConstantHandler() {
        return new QIntroduceVariableHandler(QIntroduceVariableHandler.VarType.CONSTANT);
    }
*/

    @Override
    public boolean isInplaceRenameAvailable(@NotNull PsiElement element, PsiElement context) {
        return super.isInplaceRenameAvailable(element, context);
    }
}
