package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.intention.AbstractIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.intellij.util.IncorrectOperationException;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.lang.CastType;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class QParametersAnnotator extends QElementAnnotator<QParameters> {

    public static final String FAMILY_NAME = "QParameters";

    public QParametersAnnotator() {
        super(QParameters.class);
    }

    @Override
    public void annotate(@NotNull QParameters params, @NotNull AnnotationHolder holder) {
        final List<QVarDeclaration> vars = params.getVariables();

        checkDuplicates(holder, vars);
        checkParamsTypes(holder, params);
        checkParamsCount(holder, params, vars);
    }

    private void checkParamsTypes(@NotNull AnnotationHolder holder, @NotNull QParameters params) {
        params.getParameters().forEach(p -> checkParameterType(holder, p));
    }

    private void checkParameterType(@NotNull AnnotationHolder holder, @NotNull QParameter param) {
        if (param instanceof @NotNull QPatternParameter pp) {
            final List<QParameter> parameters = pp.getParameters();
            if (parameters.size() < 2) {
                holder.newAnnotation(HighlightSeverity.WARNING, "Redundant patter match").range(pp).withFix(new RedundantParameterAction(pp)).create();
            }
            parameters.forEach(p -> checkParameterType(holder, p));
        } else if (param instanceof QTypedParameter tp) {
            final QExpression expression = tp.getExpression();
            if (expression == null && QPsiUtil.isColon(tp.getLastChild())) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Parameter type is not declared")
                        .range(tp)
                        .withFix(new InsertTypeAction(tp))
                        .withFix(new RemoveTypeAction(tp))
                        .create();
            }

            if (!(expression instanceof QLiteralExpr li)) {
                return;
            }

            final QSymbol symbol = li.getSymbol();
            if (symbol == null) {
                return;
            }

            final String typeCode = symbol.getText().substring(1);
            if (Strings.isEmpty(typeCode) || typeCode.length() > 1 || KdbType.typeOf(typeCode.charAt(0)) == null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Type is not correct")
                        .range(symbol)
                        .withFix(new ChangeTypeAction(symbol))
                        .withFix(new RemoveTypeAction(tp))
                        .create();
            }
        }
    }


    private static void checkDuplicates(@NotNull AnnotationHolder holder, List<QVarDeclaration> vars) {
        final Map<String, QVariable> names = new HashMap<>();
        for (QVariable param : vars) {
            final QVariable previous = names.put(param.getName(), param);
            if (previous != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate parameter name.").range(param).withFix(new RenameIntentionAction(param)).create();
                holder.newAnnotation(HighlightSeverity.ERROR, "Duplicate parameter name.").range(previous).withFix(new RenameIntentionAction(previous)).create();
            }
        }
    }

    private static void checkParamsCount(@NotNull AnnotationHolder holder, @NotNull QParameters params, List<QVarDeclaration> vars) {
        if (vars.size() <= 8) {
            return;
        }

        for (int i = 8; i < vars.size(); i++) {
            final QVariable var = vars.get(i);
            holder.newAnnotation(HighlightSeverity.ERROR, "Only 8 parameters are allowed.").range(var).withFix(new RemoveParameterAction(params, var)).create();
        }
    }

    private static class RemoveParameterAction extends AbstractIntentionAction {
        private final QParameters params;
        private final QVariable var;

        public RemoveParameterAction(QParameters params, QVariable var) {
            this.params = params;
            this.var = var;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return "Remove parameter";
        }

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return FAMILY_NAME;
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final Document document = editor.getDocument();

            final String text = params.getText();
            final TextRange pr = var.getTextRangeInParent();

            final int start = text.lastIndexOf(';', pr.getStartOffset());

            int end = text.indexOf(';', pr.getEndOffset());
            if (end == -1) {
                end = text.indexOf(']', pr.getEndOffset());
            }

            final int off = params.getTextOffset();
            final int gs = off + start;
            final int ge = off + end;

            document.deleteString(gs, ge);
        }
    }

    private static abstract class BaseTypeAction extends AbstractIntentionAction {
        private final String text;
        private final boolean replace;
        @NotNull
        private final QPsiElement element;

        public BaseTypeAction(String text, QPsiElement element, boolean replace) {
            this.text = text;
            this.element = element;
            this.replace = replace;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return text;
        }

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return FAMILY_NAME;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(CastType.CASTING_TYPES)
                    .setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
                        @Override
                        protected void customize(KdbType type) {
                            this.setLeftText("`" + type.getCode());
                            this.setRightForeground(JBColor.GRAY);
                            this.setRightText(type.getTypeName());
                        }
                    })
                    .setItemChosenCallback(t ->
                            WriteCommandAction.runWriteCommandAction(project, () -> {
                                        final String text = "`" + t.getCode();
                                        final Document document = editor.getDocument();
                                        if (replace) {
                                            final TextRange range = element.getTextRange();
                                            document.replaceString(range.getStartOffset(), range.getEndOffset(), text);
                                        } else {
                                            document.insertString(element.getTextOffset() + element.getTextLength(), text);
                                        }
                                    }
                            )
                    )
                    .createPopup()
                    .showInBestPositionFor(editor);
        }
    }

    private static class ChangeTypeAction extends BaseTypeAction {
        public ChangeTypeAction(@NotNull QSymbol symbol) {
            super("Change parameter type", symbol, true);
        }
    }

    private static class InsertTypeAction extends BaseTypeAction {
        public InsertTypeAction(@NotNull QTypedParameter parameter) {
            super("Insert parameter type", parameter, false);
        }
    }

    private static class RemoveTypeAction extends AbstractIntentionAction {
        @NotNull
        private final QTypedParameter param;

        public RemoveTypeAction(@NotNull QTypedParameter param) {
            this.param = param;
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return "Remove parameter type";
        }

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return FAMILY_NAME;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final Document document = editor.getDocument();
            final TextRange allRange = param.getTextRange();
            final TextRange nameRange = param.getVarDeclaration().getTextRange();
            document.deleteString(nameRange.getEndOffset(), allRange.getEndOffset());
        }
    }

    private static class RedundantParameterAction extends AbstractIntentionAction {
        @NotNull
        private final QPatternParameter param;

        public RedundantParameterAction(@NotNull QPatternParameter param) {
            this.param = param;
        }

        @Override
        public boolean startInWriteAction() {
            return true;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return "Remove pattern match";
        }

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return FAMILY_NAME;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final Document document = editor.getDocument();
            final TextRange range = param.getTextRange();

            document.deleteString(range.getEndOffset(), range.getEndOffset() + 1);
            document.deleteString(range.getStartOffset(), range.getStartOffset() + 1);
        }
    }

    private static class RenameIntentionAction extends AbstractIntentionAction {
        private final QVariable variable;

        private RenameIntentionAction(QVariable variable) {
            this.variable = variable;
        }

        @Override
        public @IntentionName @NotNull String getText() {
            return "Rename parameter";
        }

        @Override
        public @IntentionFamilyName @NotNull String getFamilyName() {
            return FAMILY_NAME;
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
            final TextRange r = variable.getTextRange();
            editor.getSelectionModel().setSelection(r.getStartOffset(), r.getEndOffset());
        }
    }
}
