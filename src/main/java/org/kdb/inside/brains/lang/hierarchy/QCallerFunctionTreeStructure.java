package org.kdb.inside.brains.lang.hierarchy;

import com.intellij.find.findUsages.FindUsagesHandlerBase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;
import java.util.Map;

public class QCallerFunctionTreeStructure extends QCallHierarchyTreeStructureBase {
    protected QCallerFunctionTreeStructure(@NotNull Project project, PsiElement element, String currentScopeType) {
        super(project, element, currentScopeType);
    }

    @Override
    protected @NotNull Map<PsiElement, Collection<PsiElement>> getChildren(@NotNull QVarDeclaration declaration) {
        final Collection<UsageInfo> usages = findUsages(declaration);

        final MultiMap<PsiElement, PsiElement> res = MultiMap.createOrderedSet();
        for (UsageInfo usage : usages) {
            PsiElement element = usage.getElement();
            if (element == declaration || !(element instanceof QPsiElement qEl)) {
                continue;
            }

            if (!(element instanceof QVarReference) && !(element instanceof QSymbol)) {
                continue;
            }

            final QAssignmentExpr assign = getGlobalLambdaAssignment(element);
            if (assign != null) {
                res.putValue(assign.getVarDeclaration(), element);
            } else {
                res.putValue(element.getContainingFile(), element);
            }
        }
        return res.freezeValues();
    }

    private QAssignmentExpr getGlobalLambdaAssignment(PsiElement el) {
        PsiElement element = el;
        while (element != null) {
            final QAssignmentExpr assign = PsiTreeUtil.getParentOfType(element, QAssignmentExpr.class);
            if (assign != null && QPsiUtil.isGlobalDeclaration(assign) && assign.getExpression() instanceof QLambdaExpr) {
                return assign;
            }
            element = assign;
        }
        return null;
    }


    private @NotNull Collection<UsageInfo> findUsages(@NotNull QVarDeclaration declaration) {
        final FindUsagesHandlerBase handler = new FindUsagesHandlerBase(declaration);
        final CommonProcessors.CollectProcessor<UsageInfo> processor = new CommonProcessors.CollectProcessor<>();
        handler.processElementUsages(declaration, processor, handler.getFindUsagesOptions(null));
        return processor.getResults();
    }
}
