package org.kdb.inside.brains.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QLambda;
import org.kdb.inside.brains.psi.QParameters;
import org.kdb.inside.brains.psi.QTable;
import org.kdb.inside.brains.psi.QVariable;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    private static final boolean COLLAPSED_BY_DEFAULT = false;

    @NotNull
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        final Collection<QTable> tables = PsiTreeUtil.findChildrenOfType(root, QTable.class);
        final Collection<QLambda> lambdas = PsiTreeUtil.findChildrenOfType(root, QLambda.class);
        if (lambdas.isEmpty() && tables.isEmpty()) {
            return FoldingDescriptor.EMPTY;
        }
        return Stream.concat(tables.stream(), lambdas.stream()).map(l -> new FoldingDescriptor(l, l.getTextRange())).toArray(FoldingDescriptor[]::new);
    }

    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        final PsiElement psi = node.getPsi();
        if (psi instanceof QLambda) {
            final QLambda lambda = (QLambda) psi;
            return "{" + Optional.of(lambda).map(QLambda::getParameters).map(QParameters::getVariableList).map(Collection::stream).map(v -> "[" + v.map(QVariable::getName).collect(Collectors.joining(";")) + "]").orElse("") + "...}";
        } else if (psi instanceof QTable) {
            final QTable tbl = (QTable) psi;
            return "([" +
                    (tbl.getKeyColumns() != null ? " " + tbl.getKeyColumns().getAssignments().size() + " " : "") +
                    "]" +
                    (tbl.getValueColumns() != null ? " " + tbl.getValueColumns().getAssignments().size() + " " : "") +
                    ")";
        }
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return COLLAPSED_BY_DEFAULT;
    }
}
