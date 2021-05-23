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
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QTypeCast;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QTypeCastAnnotator extends QElementAnnotator<QTypeCast> {
    final List<KdbType> types;

    public QTypeCastAnnotator() {
        super(QTypeCast.class);
        types = Stream.of(KdbType.values()).collect(Collectors.toList());
    }

    @Override
    public void annotate(@NotNull QTypeCast cast, @NotNull AnnotationHolder holder) {
        final PsiElement castType = cast.getFirstChild();

        final String name = QPsiUtil.getTypeCast(cast);
        if (name.isEmpty()) {
            return;
        }

        final TextRange r = castType.getTextRange();

        final boolean code;
        final TextRange range;
        if (name.length() == 1) {
            final char ch = name.charAt(0);
            if (Character.isUpperCase(ch) && KdbType.byCode(Character.toLowerCase(ch)) != null) {
                return;
            }
            code = true;
            range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 2);
        } else {
            final KdbType type = KdbType.byName(name);
            if (type != null) {
                return;
            }
            code = false;
            range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 1);
        }

        holder.newAnnotation(HighlightSeverity.ERROR, "Unknown cast type: " + name)
                .range(range)
                .withFix(new IntentionAction() {
                    @Override
                    public @IntentionName @NotNull String getText() {
                        return "Change cast type to...";
                    }

                    @Override
                    public @NotNull @IntentionFamilyName String getFamilyName() {
                        return "Type cast";
                    }

                    @Override
                    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
                        return true;
                    }

                    @Override
                    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
                        JBPopupFactory.getInstance()
                                .createPopupChooserBuilder(types)
                                .setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
                                    @Override
                                    protected void customize(KdbType value) {
                                        this.setLeftText(value.getName());
                                        this.setRightForeground(JBColor.GRAY);
                                        this.setRightText(String.valueOf(value.getUpperCode()));
                                    }

                                })
                                .setItemChosenCallback(t -> {
                                    final String name = code ? String.valueOf(Character.toUpperCase(t.getCode())) : t.getName();
                                    WriteCommandAction.runWriteCommandAction(project, () -> editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), name));
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
}
