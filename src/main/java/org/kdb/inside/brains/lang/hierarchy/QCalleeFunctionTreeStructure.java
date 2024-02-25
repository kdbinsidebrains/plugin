package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;
import java.util.Map;

public class QCalleeFunctionTreeStructure extends QCallHierarchyTreeStructureBase {
    protected QCalleeFunctionTreeStructure(@NotNull Project project, PsiElement element, String currentScopeType) {
        super(project, element, currentScopeType);
    }

    @Override
    protected @NotNull Map<PsiElement, Collection<PsiElement>> getChildren(@NotNull QVarDeclaration declaration) {
        final PsiElement parent = declaration.getParent();
        if (!(parent instanceof QAssignmentExpr exp)) {
            return Map.of();
        }

        if (!(exp.getExpression() instanceof QLambdaExpr)) {
            return Map.of();
        }

        final Collection<QPsiElement> children = PsiTreeUtil.findChildrenOfAnyType(exp, QVarReference.class, QSymbol.class);

        final MultiMap<PsiElement, PsiElement> res = MultiMap.createOrderedSet();
        for (QPsiElement child : children) {
            final PsiReference reference = child.getReference();
            if (reference == null) {
                continue;
            }
            final PsiElement el = reference.resolve();
            if (el instanceof QVarDeclaration d && QPsiUtil.isGlobalDeclaration(d)) {
                res.putValue(el, child);
            }
        }
        return res.freezeValues();
    }
}
