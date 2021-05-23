package org.kdb.inside.brains.psi;

import com.intellij.psi.PsiElement;

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

    public QQuery query() {
        return scope == ElementScope.QUERY ? (QQuery) element : null;
    }

    public QTable table() {
        return scope == ElementScope.TABLE ? (QTable) element : null;
    }

    public QLambda lambda() {
        return scope == ElementScope.LAMBDA ? (QLambda) element : null;
    }

    public QParameters parameters() {
        return scope == ElementScope.PARAMETERS ? (QParameters) element : null;
    }

    public static ElementContext of(QPsiElement element) {
        PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof QParameters) {
                return new ElementContext(ElementScope.PARAMETERS, parent);
            }
            if (parent instanceof QLambda) {
                return new ElementContext(ElementScope.LAMBDA, parent);
            }
            if (parent instanceof QQuery) {
                return new ElementContext(ElementScope.QUERY, parent);
            }
            if (parent instanceof QTable) {
                return new ElementContext(ElementScope.TABLE, parent);
            }
            parent = parent.getParent();
        }
        return new ElementContext(ElementScope.FILE, element.getContainingFile());
    }
}