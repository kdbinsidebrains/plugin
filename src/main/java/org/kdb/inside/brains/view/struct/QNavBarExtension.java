package org.kdb.inside.brains.view.struct;

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.*;

/**
 * See JavaNavBarExtension as an example
 */
public class QNavBarExtension extends StructureAwareNavBarModelExtension {
    @NotNull
    @Override
    protected Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @Nullable PsiElement getLeafElement(@NotNull DataContext dataContext) {
        final PsiElement leafElement = super.getLeafElement(dataContext);
        // resolve Assignment to something meaningful
        if (leafElement instanceof QAssignmentExpr) {
            return ElementContext.of(leafElement).getElement();
        }
        return leafElement;
    }

    @Override
    public @Nullable String getPresentableText(Object object) {
        if (!(object instanceof QPsiElement e)) {
            return null;
        }

        if (object instanceof QSymbol s) {
            return s.getName();
        }
        if (object instanceof QVarDeclaration d) {
            return d.getQualifiedName();
        }
        if (object instanceof QCommand c) {
            return c.getCommand().getText();
        }
        if (object instanceof QImport i) {
            return "<import>";
        }
        if (object instanceof QContext ctx) {
            return getVarName(ctx.getVariable());
        }
        if (object instanceof QLambdaExpr || object instanceof QTableExpr || object instanceof QDictExpr) {
            return getDeclarationName((QPsiElement) object);
        }
        return null;
    }

    private String getDeclarationName(QPsiElement element) {
        final VarAssignment assignment = QPsiUtil.getVarAssignment(element);
        return assignment == null ? "<anonymous>" : assignment.declaration().getQualifiedName();
    }

    private String getVarName(QVarDeclaration var) {
        return var == null ? null : var.getQualifiedName();
    }
}