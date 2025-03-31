package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.lang.annotation.WriteIntentionAction;
import org.kdb.inside.brains.psi.*;

import java.util.List;

public class QAssignmentAnnotator extends QElementAnnotator<QAssignmentExpr> {
    private static final String FAMILY_NAME = "Variable Declaration";

    public QAssignmentAnnotator() {
        super(QAssignmentExpr.class);
    }

    @Override
    protected void annotate(@NotNull QAssignmentExpr assignment, @NotNull AnnotationHolder holder) {
        final QPatternDeclaration patternDeclaration = assignment.getPatternDeclaration();
        if (patternDeclaration != null) {
            validatePatternDeclaration(assignment, patternDeclaration, holder);
        }
    }

    private static void validatePatternDeclaration(@NotNull QAssignmentExpr assignment, QPatternDeclaration pattern, @NotNull AnnotationHolder holder) {
        final QExpression expression = assignment.getExpression();
        if (!(expression instanceof QParenthesesExpr parentheses)) {
            return;
        }

        final List<QExpression> expressions = parentheses.getExpressionList();
        final List<QTypedVariable> variables = pattern.getOrderedTypedVariables();

        final int variablesSize = variables.size();
        final int expressionsSize = expressions.size();
        final int size = Math.min(variablesSize, expressionsSize);

        for (int i = size; i < variablesSize; i++) {
            final QTypedVariable v = variables.get(i);
            holder.newAnnotation(HighlightSeverity.ERROR, "The variable has no assigned value")
                    .range(v)
                    .withFix(new RemoveExcessiveAction("Remove variable declaration", v))
                    .create();
        }

        for (int i = size; i < expressionsSize; i++) {
            final QExpression e = expressions.get(i);
            holder.newAnnotation(HighlightSeverity.ERROR, "The expression assignment is out of range")
                    .range(e)
                    .withFix(new RemoveExcessiveAction("Remove excessive declaration", e))
                    .withFix(new CrateVariableAction("Create new variable", pattern, i - variablesSize))
                    .create();
        }
    }

    private static class CrateVariableAction extends WriteIntentionAction {
        public CrateVariableAction(@NotNull String text, QPatternDeclaration variables, int count) {
            super(text, FAMILY_NAME, (project, editor, file) -> {
                PsiElement anchor = QPsiUtil.getPrevNonWhitespace(variables.getLastChild());
                for (int i = 0; i <= count; i++) {
                    anchor = anchor.getParent().addAfter(QPsiUtil.createSemicolon(project), anchor);
                }
                final PsiElement newVar = anchor.getParent().addAfter(QPsiUtil.createVarDeclaration(project, "newVariable"), anchor);
                QPsiUtil.selectInEditor(editor, newVar);
            });
        }
    }

    private static class RemoveExcessiveAction extends WriteIntentionAction {
        public RemoveExcessiveAction(@NotNull String text, @NotNull PsiElement e) {
            super(text, FAMILY_NAME, (project, editor, file) -> {
                        final PsiElement prev = QPsiUtil.getPrevNonWhitespace(e);
                        if (QPsiUtil.isSemicolon(prev)) {
                            prev.delete();
                        }
                        e.delete();
                    }
            );
        }
    }
}
