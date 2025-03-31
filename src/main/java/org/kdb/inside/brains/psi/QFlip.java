package org.kdb.inside.brains.psi;

import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * Marker interface for QTableExpr or QDictExpr structures. Can be used when both should be treated the same way.
 */
public interface QFlip extends QPsiElement {
    default List<QTableColumn> getColumns() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, QTableColumns.class).stream().flatMap(t -> t.getColumns().stream()).toList();
    }
}