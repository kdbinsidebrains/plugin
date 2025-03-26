package org.kdb.inside.brains.lang.annotation.impl;

import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.lang.annotation.BaseIntentionAction;
import org.kdb.inside.brains.lang.annotation.QElementAnnotator;
import org.kdb.inside.brains.lang.annotation.WriteIntentionAction;
import org.kdb.inside.brains.psi.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class QFlipAnnotator<T extends QFlip> extends QElementAnnotator<T> {
    private final String familyName;
    private final String typeName;

    private QFlipAnnotator(Class<T> clazz, @IntentionFamilyName @NotNull String familyName, String typeName) {
        super(clazz);
        this.familyName = familyName;
        this.typeName = typeName;
    }

    public static QFlipAnnotator<QDictExpr> newDictAnnotator() {
        return new QFlipAnnotator<>(QDictExpr.class, "Dict declaration", "field");
    }

    public static QFlipAnnotator<QTableExpr> newTableAnnotator() {
        return new QFlipAnnotator<>(QTableExpr.class, "Table declaration", "column");
    }

    @Override
    protected void annotate(@NotNull QFlip element, @NotNull AnnotationHolder holder) {
        validateTailSemicolon(element, holder);

        final List<QTableColumns> columnsList = PsiTreeUtil.getChildrenOfTypeAsList(element, QTableColumns.class);
        for (QTableColumns columns : columnsList) {
            validateTailSemicolon(columns, holder);
            for (QTableColumn column : columns.getColumns()) {
                validateMissedDeclarations(column, holder);
            }
        }
        validateDuplicateNames(columnsList, holder);
    }

    private void validateDuplicateNames(@NotNull List<QTableColumns> columnsList, @NotNull AnnotationHolder holder) {
        final Map<String, List<QVarDeclaration>> vars = columnsList.stream()
                .flatMap(l -> l.getColumns().stream())
                .map(QTableColumn::getVarDeclaration)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(QVariable::getQualifiedName));

        for (List<QVarDeclaration> value : vars.values()) {
            final int size = value.size();
            if (size > 1) {
                final QVarDeclaration first = value.get(0);
                final QVarDeclaration second = value.get(1);
                addDuplicateNameWarning(second, first, holder);
                for (int i = 1; i < size; i++) {
                    addDuplicateNameWarning(first, value.get(i), holder);
                }
            }
        }
    }

    protected void addDuplicateNameWarning(@NotNull QVarDeclaration master, @NotNull QVarDeclaration slave, @NotNull AnnotationHolder holder) {
        holder.newAnnotation(HighlightSeverity.WARNING, "Duplicate " + typeName + " name")
                .range(slave)
                .withFix(new BaseIntentionAction(familyName, "Navigate to duplicate", (p, e, f) -> QPsiUtil.selectInEditor(e, master)))
                .create();
    }

    protected void validateTailSemicolon(@NotNull PsiElement element, AnnotationHolder holder) {
        PsiElement semicolon = null;
        PsiElement child = QPsiUtil.getFirstNonWhitespaceAndCommentsChild(element);
        while (child != null) {
            final IElementType et = child.getNode().getElementType();
            if (et != QTypes.BRACKET_OPEN && et != QTypes.BRACKET_CLOSE) {
                if (et == QTypes.SEMICOLON) {
                    if (semicolon != null) {
                        createSemicolonError(child, holder);
                    } else {
                        semicolon = child;
                    }
                } else {
                    semicolon = null;
                }
            }
            child = PsiTreeUtil.skipWhitespacesAndCommentsForward(child);
        }

        if (semicolon != null) {
            createSemicolonError(semicolon, holder);
        }
    }

    private void validateMissedDeclarations(QTableColumn column, AnnotationHolder holder) {
        final PsiElement child = column.getFirstChild();
        if (child instanceof QVarDeclaration) {
            return;
        }
        if (child instanceof QVarReference && child == column.getLastChild()) {
            return;
        }

        AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Missing " + familyName + " name").range(child);
        final PsiElement missedDeclaration = getMissedColumnDeclaration(child);
        if (missedDeclaration != null) {
            builder = builder.withFix(new WriteIntentionAction(familyName, "Insert declaration colon", (project, editor, file) -> {
                        final TextRange range = missedDeclaration.getTextRange();
                        editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), ":");
                    })
            );
        }
        builder = builder.withFix(new WriteIntentionAction(familyName, "Insert " + typeName + " name", (project, editor, file) -> {
                    final QVarDeclaration var = QPsiUtil.createVarDeclaration(project, "renameMe");
                    final PsiElement el = column.addBefore(var, child);
                    column.addBefore(QPsiUtil.createColon(project), child);
                    QPsiUtil.selectInEditor(editor, el);
                })
        );
        builder.create();
    }

    private PsiElement getMissedColumnDeclaration(PsiElement firstChild) {
        if (!(firstChild instanceof QInvokeFunction invoke)) {
            return null;
        }

        final PsiElement first = QPsiUtil.getFirstNonWhitespaceAndCommentsChild(invoke);
        if (!(first instanceof QCustomFunction)) {
            return null;
        }

        final PsiElement second = QPsiUtil.getFirstNonWhitespaceAndCommentsChild(first);
        if (!(second instanceof QVarReference)) {
            return null;
        }
        return first.getNextSibling();
    }

    private void createSemicolonError(PsiElement el, AnnotationHolder holder) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Tailing semicolon is not allowed")
                .range(el)
                .withFix(new WriteIntentionAction(familyName, "Remove semicolon", (p, e, f) -> el.delete()))
                .create();
    }
}
