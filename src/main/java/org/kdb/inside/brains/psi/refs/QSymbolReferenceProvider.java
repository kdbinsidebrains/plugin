package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QSymbol;

public class QSymbolReferenceProvider extends QBaseReferenceProvider<QSymbol> {
    protected QSymbolReferenceProvider() {
        super(QSymbol.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QSymbol element, @NotNull ProcessingContext context) {
        return new PsiReference[]{new QSymbolReference(element)};
    }

    public static class QSymbolReference extends QBaseReference<QSymbol> {
        public QSymbolReference(@NotNull QSymbol element) {
            super(element, new TextRange(1, element.getTextLength()));
        }

        @Override
        protected String getQualifiedName(QSymbol element) {
            return element.getQualifiedName();
        }

        @Override
        protected ResolveResult[] resolveGlobalDeclaration(QSymbol element) {
            final ResolveResult[] resolveResults = super.resolveGlobalDeclaration(element);
            // We resolve symbol on itself if nothing found.
            // It makes life for QSpec and in some other places
            // Downside: symbol is always resolvable and defined that by the fact is true.
            return resolveResults.length == 0 ? new ResolveResult[]{new PsiElementResolveResult(element)} : resolveResults;
        }
    }
}