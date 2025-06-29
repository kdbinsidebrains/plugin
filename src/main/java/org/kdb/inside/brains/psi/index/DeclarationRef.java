package org.kdb.inside.brains.psi.index;

import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import org.kdb.inside.brains.psi.*;

public abstract class DeclarationRef {
    private DeclarationRef() {
    }

    public abstract boolean isGlobal();


    public abstract PsiElement getElement();

    public abstract QExpression getExpression();

    public abstract NavigationItem getNavigationItem();


    public static DeclarationRef of(QSymbol symbol) {
        return new SymbolRef(symbol);
    }

    public static DeclarationRef of(QVarDeclaration declaration) {
        return new VariableRef(declaration);
    }


    public void navigate(boolean requestFocus) {
        final NavigationItem navigationItem = getNavigationItem();
        if (navigationItem != null) {
            navigationItem.navigate(requestFocus);
        }
    }

    private static abstract class BaseRef<Element extends PsiElement> extends DeclarationRef {
        protected final Element element;

        public BaseRef(Element element) {
            this.element = element;
        }

        @Override
        public final PsiElement getElement() {
            return element;
        }
    }

    private static class SymbolRef extends BaseRef<QSymbol> {
        private SymbolRef(QSymbol symbol) {
            super(symbol);
        }

        @Override
        public boolean isGlobal() {
            return true;
        }

        @Override
        public QExpression getExpression() {
            return null;
        }

        @Override
        public NavigationItem getNavigationItem() {
            return null;
        }
    }

    private static class VariableRef extends BaseRef<QVarDeclaration> {
        public VariableRef(QVarDeclaration declaration) {
            super(declaration);
        }

        @Override
        public NavigationItem getNavigationItem() {
            return element;
        }

        @Override
        public boolean isGlobal() {
            return QPsiUtil.isGlobalDeclaration(element);
        }

        @Override
        public QExpression getExpression() {
            final PsiElement parent = element.getParent();
            if (!(parent instanceof QAssignmentExpr assignment)) {
                return null;
            }
            return assignment.getExpression();
        }
    }
}