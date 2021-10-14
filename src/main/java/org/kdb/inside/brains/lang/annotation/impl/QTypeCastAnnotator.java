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
import org.kdb.inside.brains.psi.QTypeCastExpr;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QTypeCastAnnotator extends QElementAnnotator<QTypeCastExpr> {
    private final List<TypeCast> typeCasts;
    private final Set<String> extractors = Set.of("hh", "mm", "ss");

    public QTypeCastAnnotator() {
        super(QTypeCastExpr.class);
        typeCasts = Stream.of(KdbType.values()).map(TypeCast::new).collect(Collectors.toList());
    }

    @Override
    public void annotate(@NotNull QTypeCastExpr cast, @NotNull AnnotationHolder holder) {
        final PsiElement castType = cast.getFirstChild();

        final String name = QPsiUtil.getTypeCast(cast);
        if (name.isEmpty()) {
            return;
        }

        final TextRange r = castType.getTextRange();

        final int code;
        final TextRange range;
        if (name.length() == 1) {
            final char ch = name.charAt(0);
            if (KdbType.byCode(Character.toLowerCase(ch)) != null) {
                return;
            }
            code = Character.isUpperCase(ch) ? 1 : 2;
            range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 2);
        } else {
            if (extractors.contains(name)) {
                return;
            }

            final KdbType type = KdbType.byName(name);
            if (type != null) {
                return;
            }
            code = 0;
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
                                .createPopupChooserBuilder(typeCasts)
                                .setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
                                    @Override
                                    protected void customize(TypeCast typeCast) {
                                        this.setLeftText(typeCast.name);
                                        this.setRightForeground(JBColor.GRAY);
                                        if (code == 1) {
                                            this.setRightText(typeCast.upperCode);
                                        } else if (code == 2) {
                                            this.setRightText(typeCast.lowerCode);
                                        }
                                    }
                                })
                                .setItemChosenCallback(t -> {
                                    final String name = code == 0 ? t.name : code == 1 ? t.upperCode : t.lowerCode;
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

    private static class TypeCast {
        private final String name;
        private final String lowerCode;
        private final String upperCode;

        public TypeCast(KdbType type) {
            this(type.getName(), type.getCode());
        }

        public TypeCast(String name, char code) {
            this.name = name;
            this.lowerCode = String.valueOf(Character.toLowerCase(code));
            this.upperCode = String.valueOf(Character.toUpperCase(code));
        }
    }
}
