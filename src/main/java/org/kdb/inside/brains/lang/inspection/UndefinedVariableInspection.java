package org.kdb.inside.brains.lang.inspection;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiEditorUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.ide.runner.qspec.QSpecTestLocator;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.QIndexService;
import org.kdb.inside.brains.view.inspector.InspectorToolWindow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

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

        if (QSpecTestLocator.isQSpecVariable(qualifiedName)) {
            return;
        }

        final ElementContext context = ElementContext.of(variable);
        final ElementScope scope = context.getScope();
        if (scope == ElementScope.TABLE) {
            return; // ignore every non-resolved variable as it may be referencing a column name
        }

        // ignore every non-resolved variable as it may be referencing a column name
        if (scope == ElementScope.QUERY) {
            return;
        }

        final PsiReference reference = variable.getReference();
        if (reference instanceof PsiPolyVariantReference) {
            final ResolveResult[] resolveResults = ((PsiPolyVariantReference) reference).multiResolve(false);
            if (resolveResults.length != 0) {
                return;
            }
        }

        final InspectorToolWindow inspector = InspectorToolWindow.getExist(holder.getProject());
        if (inspector != null && inspector.containsElement(qualifiedName)) {
            return;
        }

        LocalQuickFix[] localQuickFix = LocalQuickFix.EMPTY_ARRAY;
        final PsiElement parent = variable.getParent();
        if (parent instanceof QCustomFunction && parent.getParent() instanceof QInvokeFunction) {
            if (isItNamespace(variable, holder.getProject())) {
                return;
            }

            final QExpressions expressions = variable.getContext(QExpressions.class);
            if (expressions != null) {
                localQuickFix = new LocalQuickFix[]{createInvokeFix(variable, expressions), createInvokeFix(variable)};
            } else {
                localQuickFix = new LocalQuickFix[]{createInvokeFix(variable)};
            }
        }
        holder.registerProblem(variable, "`" + qualifiedName + "` might not have been defined", ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, localQuickFix);
    }

    private boolean isItNamespace(@NotNull QVarReference variable, Project project) {
        return QIndexService.getInstance(variable).firstMatch(s -> s.startsWith(variable.getQualifiedName()), GlobalSearchScope.allScope(project)) != null;
    }

    private LocalQuickFix createInvokeFix(QVarReference variable) {
        return createInvokeFix(variable, null);
    }

    private LocalQuickFix createInvokeFix(QVarReference variable, QPsiElement context) {
        final boolean global = context == null;
        final String name = variable.getName();
        final String label = "Create " + (global ? "global" : "local") + " function";

        return new LocalQuickFixOnPsiElement(variable, context) {
            @Override
            public @IntentionName @NotNull String getText() {
                return label + " " + name;
            }

            @Override
            public @IntentionFamilyName @NotNull String getFamilyName() {
                return label;
            }

            @Override
            public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
                final QVarReference var = (QVarReference) startElement;
                final QPsiElement ctx = (QPsiElement) endElement;

                final PsiElement expression = QPsiUtil.findRootExpression(var, ctx);
                final PsiElement body = expression.getParent();

                final String[] params = buildParameters((QInvokeFunction) var.getParent().getParent());

                final PsiElement lambdaDef = QPsiUtil.createLambdaDeclaration(project, global, var.getQualifiedName(), params);

                final PsiElement lambda;
                if (global && var.getContext(QLambdaExpr.class) != null) {
                    final PsiElement anchor = findNextElement(expression);
                    lambda = body.addAfter(lambdaDef, anchor);
                    body.addBefore(QPsiUtil.createWhitespace(project, "\n\n"), lambda);
                    body.addAfter(QPsiUtil.createSemicolon(project), lambda);
                } else {
                    lambda = body.addBefore(lambdaDef, expression);
                    body.addAfter(QPsiUtil.createSemicolon(project), lambda);
                    body.addBefore(QPsiUtil.createWhitespace(project, "\n\n"), expression);
                }

                final Editor editor = PsiEditorUtil.findEditor(lambda);
                if (editor != null) {
                    int offset = lambda.getTextOffset() + lambda.getTextLength() - 2;
                    editor.getCaretModel().moveToOffset(offset);
                    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                }
            }

            private String[] buildParameters(QInvokeFunction invoke) {
                final List<QArguments> arguments = invoke.getArgumentsList();
                final List<QExpression> expressions = invoke.getExpressionList();

                final AtomicInteger var = new AtomicInteger(0);
                return Stream.concat(arguments.stream().flatMap(a -> a.getExpressions().stream()), expressions.stream()).map(expression -> {
                    if (expression instanceof QVarReference) {
                        return ((QVarReference) expression).getSimpleName();
                    } else {
                        return "var" + var.incrementAndGet();
                    }
                }).toArray(String[]::new);
            }
        };
    }

    private PsiElement findNextElement(PsiElement element) {
        PsiElement e = element;
        while (e != null) {
            if (QPsiUtil.isSemicolon(e)) {
                return e;
            }
            if (QPsiUtil.isLeafText(e, t -> t.indexOf('\n') >= 0)) {
                return e.getPrevSibling();
            }
            e = e.getNextSibling();
        }
        return element;
    }
}
