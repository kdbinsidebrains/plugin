package org.kdb.inside.brains.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class QFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    private static final boolean COLLAPSED_BY_DEFAULT = false;

    private static final FoldingDescriptor[] EMPTY_ARRAY = new FoldingDescriptor[0];

    @NotNull
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        final Collection<QLambdaExpr> lambdas = PsiTreeUtil.findChildrenOfType(root, QLambdaExpr.class);
        final Collection<QFlip> tables = PsiTreeUtil.findChildrenOfType(root, QFlip.class);
        if (lambdas.isEmpty() && tables.isEmpty()) {
            return EMPTY_ARRAY;
        }
        return Stream.concat(tables.stream(), lambdas.stream()).map(l -> new FoldingDescriptor(l, l.getTextRange())).toArray(FoldingDescriptor[]::new);
    }

    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        final PsiElement psi = node.getPsi();
        if (psi instanceof QLambdaExpr lambda) {
            return "{" + lambda.getParametersInfo() + "...}";
        } else if (psi instanceof QTableExpr tbl) {
            return "([" + getColumnsCount(tbl.getKeys()) + "]" + getColumnsCount(tbl.getValues()) + ")";
        } else if (psi instanceof QDictExpr dict) {
            return "([" + getColumnsCount(dict.getFields()) + "])";
        }
        return "...";
    }

    @NotNull
    private String getColumnsCount(@Nullable QTableColumns columns) {
        return columns == null ? "" : getColumnsCount(columns.getColumns());
    }

    @NotNull
    private String getColumnsCount(@Nullable List<QTableColumn> columns) {
        return columns == null ? "" : " " + columns.size() + " ";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return COLLAPSED_BY_DEFAULT;
    }
}
