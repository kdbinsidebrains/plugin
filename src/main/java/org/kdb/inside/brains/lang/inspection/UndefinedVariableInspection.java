package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.lang.exclusions.UndefinedExclusion;
import org.kdb.inside.brains.lang.inspection.fix.CreateFunctionFix;
import org.kdb.inside.brains.lang.inspection.fix.ExcludeVariableFix;
import org.kdb.inside.brains.lang.inspection.fix.QSpecLibraryFix;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.QIndexService;
import org.kdb.inside.brains.view.inspector.InspectorToolWindow;

import java.util.ArrayList;
import java.util.List;

public class UndefinedVariableInspection extends ElementInspection<QVarReference> {
    public UndefinedVariableInspection() {
        super(QVarReference.class);
    }

    @Override
    protected void validate(@NotNull QVarReference variable, @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final String qualifiedName = variable.getQualifiedName();
        if (QLanguage.isKeyword(qualifiedName) || QLanguage.isSystemFunction(qualifiedName)) {
            return;
        }

        if (QPsiUtil.isImplicitVariable(variable)) {
            return;
        }

        if (UndefinedExclusion.isExcluded(qualifiedName)) {
            return;
        }

        final ElementContext context = ElementContext.of(variable);
        final ElementScope scope = context.getScope();
        // ignore every non-resolved variable as it may be referencing a column name
        if (scope == ElementScope.TABLE) {
            return;
        }

        // ignore every non-resolved variable as it may be referencing a column name
        if (scope == ElementScope.QUERY) {
            return;
        }

        for (PsiReference reference : variable.getReferences()) {
            if (reference.resolve() != null) {
                return;
            }
        }

        final InspectorToolWindow inspector = InspectorToolWindow.getExist(holder.getProject());
        if (inspector != null && inspector.containsElement(qualifiedName)) {
            return;
        }

        final List<LocalQuickFix> localQuickFix = new ArrayList<>();
        final PsiElement parent = variable.getParent();
        if (parent instanceof QCustomFunction customFunction && parent.getParent() instanceof QInvokeFunction invokeFunction) {
            if (processInvokeFunction(holder.getProject(), invokeFunction, customFunction, variable, localQuickFix)) {
                return;
            }
        }
        localQuickFix.add(new ExcludeVariableFix(variable));
        holder.registerProblem(variable, "`" + qualifiedName + "` might not have been defined", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix.toArray(LocalQuickFix[]::new));
    }

    private boolean processInvokeFunction(Project project, QInvokeFunction invoke, QCustomFunction function, @NotNull QVarReference variable, List<LocalQuickFix> localQuickFix) {
        if (isItNamespace(variable, project)) {
            return true;
        }

        if (variable.getQualifiedName().equals(TestDescriptor.SUITE)) {
            localQuickFix.add(new QSpecLibraryFix(variable));
        } else {
            final QExpressions expressions = variable.getContext(QExpressions.class);
            if (expressions != null) {
                localQuickFix.add(new CreateFunctionFix(variable, expressions));
            }
            localQuickFix.add(new CreateFunctionFix(variable, variable));
        }
        return false;
    }

    /**
     * The case like:
     * <pre>
     * .asd.qwe.zcx:12;
     * .asd.qwe[`zcx]
     * </pre>
     * <p>
     * Here on the second line .asd.qwe is part of the namespace and we ignore such cases even can't resolve for now.
     */
    private boolean isItNamespace(@NotNull QVarReference variable, Project project) {
        final String qualifiedName = variable.getQualifiedName() + ".";
        return QIndexService.getInstance(variable).firstMatch(s -> s.startsWith(qualifiedName), GlobalSearchScope.allScope(project)) != null;
    }
}
