package org.kdb.inside.brains.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public class ElementContext {
    private final ElementScope scope;
    private final PsiElement element;

    private ElementContext(ElementScope scope, PsiElement element) {
        this.scope = scope;
        this.element = element;
    }

    public ElementScope getScope() {
        return scope;
    }

    public PsiElement getElement() {
        return element;
    }

    public QFile file() {
        return scope == ElementScope.FILE ? (QFile) element : null;
    }

    public boolean is(ElementScope scope) {
        return this.scope == scope;
    }

    public boolean any(ElementScope scope) {
        return this.scope == scope;
    }

    public boolean any(ElementScope s1, ElementScope s2) {
        return this.scope == s1 || this.scope == s2;
    }

    public QQueryExpr query() {
        return scope == ElementScope.QUERY ? (QQueryExpr) element : null;
    }

    public QTableExpr table() {
        return scope == ElementScope.TABLE ? (QTableExpr) element : null;
    }

    public QLambdaExpr lambda() {
        return scope == ElementScope.LAMBDA ? (QLambdaExpr) element : null;
    }

    public QParameters parameters() {
        return scope == ElementScope.PARAMETERS ? (QParameters) element : null;
    }

    public static boolean isRoot(ASTNode node) {
        if (node == null) {
            return false;
        }
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) {
            return true;
        }
        return treeParent.getTreeParent() == null;
    }

    public static IElementType of(ASTNode node) {
        ASTNode parent = node.getTreeParent();
        while (parent != null) {
            final IElementType elementType = parent.getElementType();

            if (elementType == QTypes.PARAMETERS) {
                return QTypes.PARAMETERS;
            }
            if (elementType == QTypes.LAMBDA_EXPR) {
                return QTypes.LAMBDA_EXPR;
            }
            if (elementType == QTypes.QUERY_EXPR) {
                return QTypes.QUERY_EXPR;
            }
            if (elementType == QTypes.TABLE_EXPR) {
                return QTypes.TABLE_EXPR;
            }
            parent = parent.getTreeParent();
        }
        return null;
    }

    public static ElementContext of(QPsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof QParameters) {
                return new ElementContext(ElementScope.PARAMETERS, parent);
            }
            if (parent instanceof QLambdaExpr) {
                return new ElementContext(ElementScope.LAMBDA, parent);
            }
            if (parent instanceof QQueryExpr) {
                return new ElementContext(ElementScope.QUERY, parent);
            }
            if (parent instanceof QTableExpr) {
                return new ElementContext(ElementScope.TABLE, parent);
            }
            parent = parent.getParent();
        }
        return new ElementContext(ElementScope.FILE, element.getContainingFile());
    }
}