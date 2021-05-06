package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QTypeCast;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QTypeCastAnnotator extends QElementAnnotator<QTypeCast> {
    private static final List<String> TYPES = Stream.of(KdbType.values()).map(KdbType::getName).collect(Collectors.toList());

    public QTypeCastAnnotator() {
        super(QTypeCast.class);
    }

    @Override
    public void annotate(@NotNull QTypeCast cast, @NotNull AnnotationHolder holder) {
        final PsiElement castType = cast.getFirstChild();

        final String text = castType.getText();
        final String name = getTypeName(text);
        if (name.isEmpty()) {
            return;
        }

        if (name.length() == 1) {
            final char ch = name.charAt(0);
            if (Character.isUpperCase(ch) && KdbType.byCode(Character.toLowerCase(ch)) != null) {
                return;
            }
        }

        final KdbType type = KdbType.byName(name);
        if (type != null) {
            return;
        }

        final String message = "Unknown cast type: " + name;
        final TextRange r = castType.getTextRange();
        final TextRange range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 1);

        holder.newAnnotation(HighlightSeverity.ERROR, message)
                .range(range)
                .withFix(new IntentionAction() {
                    @Override
                    public @IntentionName @NotNull String getText() {
                        return "Change cast type to...";
                    }

                    @Override
                    public @NotNull @IntentionFamilyName String getFamilyName() {
                        return "Cast: wrong type";
                    }

                    @Override
                    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                        return true;
                    }

                    @Override
                    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                        JBPopupFactory.getInstance()
                                .createPopupChooserBuilder(TYPES)
                                .setItemChosenCallback(t -> {
                                    WriteCommandAction.runWriteCommandAction(project, () -> editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), t));
                                })
                                .createPopup()
                                .showInBestPositionFor(editor);
                    }

                    @Override
                    public boolean startInWriteAction() {
                        return false;
                    }
                }).create();
    }

    private String getTypeName(String text) {
        return text.charAt(0) == '`' ? text.substring(1, text.length() - 1) : text.substring(1, text.length() - 2);
    }
}
