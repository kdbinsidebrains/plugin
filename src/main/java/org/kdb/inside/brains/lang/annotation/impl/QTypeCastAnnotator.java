package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWithRightAlignedComponent;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.CastType;
import org.kdb.inside.brains.lang.annotation.BaseIntentionAction;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QTypeCastExpr;

import java.util.List;
import java.util.Set;

public class QTypeCastAnnotator extends QElementAnnotator<QTypeCastExpr> {
    private static final String FAMILY_NAME = "Type cast";

    private static final Set<String> EXTRACTORS = Set.of("hh", "mm", "ss");

    public QTypeCastAnnotator() {
        super(QTypeCastExpr.class);
    }

    @Override
    public void annotate(@NotNull QTypeCastExpr cast, @NotNull AnnotationHolder holder) {
        final PsiElement castType = cast.getFirstChild();

        final String name = QPsiUtil.getTypeCast(cast);
        if (name.isEmpty()) {
            return;
        }

        final TextRange r = castType.getTextRange();

        final CastSource type;
        final TextRange range;

        if (cast.getText().charAt(0) == '`') {
            if (EXTRACTORS.contains(name) || CastType.byName(name) != null) {
                return;
            }
            type = CastSource.SYMBOL;
            range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 1);
        } else {
            if (name.length() != 1) {
                return;
            }

            final char ch = name.charAt(0);
            if (CastType.byCode(Character.toLowerCase(ch)) != null) {
                return;
            }
            type = Character.isUpperCase(ch) ? CastSource.UPPER : CastSource.LOWER;
            range = new TextRange(r.getStartOffset() + 1, r.getEndOffset() - 2);
        }

        holder.newAnnotation(HighlightSeverity.ERROR, "Unknown cast type: " + name)
                .range(range)
                .withFix(new BaseIntentionAction("Change cast type to...", FAMILY_NAME, (p, e, f) -> {
                    JBPopupFactory.getInstance()
                            .createPopupChooserBuilder(List.of(CastType.values()))
                            .setRenderer(new ListCellRendererWithRightAlignedComponent<>() {
                                @Override
                                protected void customize(CastType typeCast) {
                                    this.setLeftText(typeCast.name);
                                    this.setRightForeground(JBColor.GRAY);
                                    if (type == CastSource.UPPER) {
                                        this.setRightText(typeCast.upperCode);
                                    } else if (type == CastSource.LOWER) {
                                        this.setRightText(typeCast.lowerCode);
                                    }
                                }
                            })
                            .setItemChosenCallback(t -> {
                                final String newTypeName = type == CastSource.SYMBOL ? t.name : type == CastSource.UPPER ? t.upperCode : t.lowerCode;
                                WriteCommandAction.runWriteCommandAction(p, () -> e.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), newTypeName));
                            })
                            .createPopup()
                            .showInBestPositionFor(e);
                }))
                .create();
    }

    private enum CastSource {
        SYMBOL,
        LOWER,
        UPPER
    }
}
