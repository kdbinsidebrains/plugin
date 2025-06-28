package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QPsiElement;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;
import org.kdb.inside.brains.psi.index.DeclarationRef;
import org.kdb.inside.brains.psi.index.QIndexService;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class QBaseReference<T extends QPsiElement> extends PsiPolyVariantReferenceBase<T> {
    public QBaseReference(@NotNull T psiElement) {
        super(psiElement);
    }

    public QBaseReference(@NotNull T element, TextRange range) {
        super(element, range);
    }

    public QBaseReference(@NotNull T psiElement, boolean soft) {
        super(psiElement, soft);
    }

    public QBaseReference(@NotNull T element, TextRange range, boolean soft) {
        super(element, range, soft);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return resolveGlobalDefinition(myElement);
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        if (!element.isValid()) {
            return false;
        }

        if (!(element instanceof QVariable) && !(element instanceof QSymbol)) {
            return false;
        }

        final ResolveResult[] resolveResults = multiResolve(false);
        for (ResolveResult resolveResult : resolveResults) {
            if (!resolveResult.isValidResult()) {
                continue;
            }
            final PsiElement el = resolveResult.getElement();
            if (el == null) {
                continue;
            }

            if (element.equals(el)) {
                return true;
            }
        }

        final PsiReference myRef = myElement.getReference();
        final PsiReference otherRef = element.getReference();
        if (myRef != null && otherRef != null) {
            final PsiElement myR = myRef.resolve();
            final PsiElement otR = otherRef.resolve();
            if (myElement == otR) {
                return true;
            }
            return otR != null && myR != null && Objects.equals(otR, myR);
        }
        return false;
    }

    protected ResolveResult[] resolveGlobalDefinition(T element) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return ResolveResult.EMPTY_ARRAY;
        }

        final String name = getQualifiedName(element);
        if (name == null) {
            return ResolveResult.EMPTY_ARRAY;
        }

        final QIndexService index = QIndexService.getInstance(element);
        final DeclarationRef initial = index.getFirstInFile(name, file);
        if (initial == null) {
            final GlobalSearchScope scope = GlobalSearchScope.allScope(element.getProject());
            final Collection<DeclarationRef> declarations = QIndexService.getInstance(element).getDeclarations(name, scope);
            return multi(declarations.stream().filter(DeclarationRef::isGlobalDeclaration));
        }
        return single(initial);
    }

    @NotNull
    protected ResolveResult[] single(DeclarationRef el) {
        return new ResolveResult[]{new PsiElementResolveResult(el.getElement())};
    }

    protected ResolveResult[] multi(Stream<DeclarationRef> variables) {
        return variables.map(DeclarationRef::getElement).map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    protected ResolveResult[] multi(Collection<DeclarationRef> variables) {
        return multi(variables.stream());
    }

    protected abstract String getQualifiedName(T element);
}
