package org.kdb.inside.brains.lang.inspection.fix;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.kdb.inside.brains.psi.QPsiUtil.moveCaret;

public class CreateFunctionFix extends LocalQuickFixOnPsiElement {
    private final boolean global;
    private final String name;
    private final String label;

    public CreateFunctionFix(QVarReference variable, PsiElement context) {
        super(variable, context);
        global = variable == context;
        name = variable.getName();
        label = "Create " + (global ? "global" : "local") + " function";
    }

    @Override
    public @IntentionFamilyName @NotNull String getFamilyName() {
        return label;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return label + " '" + name + "'";
    }

    @Override
    public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        final QVarReference var = (QVarReference) startElement;
        final QPsiElement ctx = (QPsiElement) endElement;

        final PsiElement expression = QPsiUtil.findRootExpression(var, ctx);
        final String[] params = buildParameters((QInvokeFunction) var.getParent().getParent());
        final PsiElement lambdaDef = QPsiUtil.createLambda(project, var.getQualifiedName() + ":", !global, params);

        final PsiElement lambda;
        if (global && var.getContext(QLambdaExpr.class) != null) {
            final PsiElement anchor = findNextElement(expression);
            lambda = QPsiUtil.insertAfter(project, lambdaDef, anchor, true);
        } else {
            lambda = QPsiUtil.insertBefore(project, lambdaDef, expression, true);
        }
        moveCaret(lambda, -2);
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
}