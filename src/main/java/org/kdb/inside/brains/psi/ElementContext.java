package org.kdb.inside.brains.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public boolean isFile() {
        return scope == ElementScope.FILE;
    }

    public ElementContext getParent() {
        return ElementContext.of(element);
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

    public boolean any(ElementScope s1, ElementScope s2, ElementScope s3) {
        return this.scope == s1 || this.scope == s2 || this.scope == s3;
    }

    public QQueryExpr query() {
        return scope == ElementScope.QUERY ? (QQueryExpr) element : null;
    }

    public QDictExpr dict() {
        return scope == ElementScope.DICT ? (QDictExpr) element : null;
    }

    public QTableExpr table() {
        return scope == ElementScope.TABLE ? (QTableExpr) element : null;
    }

    public QLambdaExpr lambda() {
        return scope == ElementScope.LAMBDA ? (QLambdaExpr) element : null;
    }

    public QContext context() {
        return scope == ElementScope.CONTEXT ? (QContext) element : null;
    }

    public QParameters parameters() {
        return scope == ElementScope.PARAMETERS ? (QParameters) element : null;
    }

    public static boolean isFile(@Nullable ASTNode node) {
        if (node == null) {
            return false;
        }
        ASTNode treeParent = node.getTreeParent();
        if (treeParent == null) {
            return true;
        }
        return treeParent.getTreeParent() == null;
    }

    public static IElementType of(@NotNull ASTNode node) {
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
            if (elementType == QTypes.DICT_EXPR) {
                return QTypes.DICT_EXPR;
            }
            if (elementType == QTypes.TABLE_EXPR) {
                return QTypes.TABLE_EXPR;
            }
            parent = parent.getTreeParent();
        }
        return null;
    }

    public static @NotNull ElementContext of(@NotNull PsiElement element) {
        PsiElement el = element.getParent();
        while (el != null) {
            if (el instanceof QParameters) {
                return new ElementContext(ElementScope.PARAMETERS, el);
            }
            if (el instanceof QLambdaExpr) {
                return new ElementContext(ElementScope.LAMBDA, el);
            }
            if (el instanceof QQueryExpr) {
                return new ElementContext(ElementScope.QUERY, el);
            }
            if (el instanceof QDictExpr) {
                return new ElementContext(ElementScope.DICT, el);
            }
            if (el instanceof QContext) {
                return new ElementContext(ElementScope.CONTEXT, el);
            }
            if (el instanceof QTableExpr) {
                return new ElementContext(ElementScope.TABLE, el);
            }
            el = el.getParent();
        }
        return new ElementContext(ElementScope.FILE, element.getContainingFile());
    }
}