package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.QIndexService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        return resolveElement(myElement);
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

    protected ResolveResult[] resolveElement(T element) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return ResolveResult.EMPTY_ARRAY;
        }

        final String name = getQualifiedName(element);
        if (name == null) {
            return ResolveResult.EMPTY_ARRAY;
        }

        final QIndexService index = QIndexService.getInstance(element);
        final QVarDeclaration initial = index.findFirstInFile(name, file);
        if (initial == null) {
            return multi(findGlobalVariables(name, element, GlobalSearchScope.allScope(element.getProject())));
        }
        return single(initial);
    }

    protected List<QVarDeclaration> findGlobalVariables(String qualifiedName, T element, @NotNull GlobalSearchScope scope) {
        final List<QVarDeclaration> elements = new ArrayList<>();
        final QIndexService index = QIndexService.getInstance(element);
        index.processVariables(s -> s.equals(qualifiedName), scope, (key, file, descriptor, variable) -> {
            if (QPsiUtil.isGlobalDeclaration(variable)) {
                elements.add(variable);
            }
        });
        return elements;
    }

    @NotNull
    protected ResolveResult[] single(QVarDeclaration el) {
        return new ResolveResult[]{new PsiElementResolveResult(el)};
    }

    protected ResolveResult[] multi(List<QVarDeclaration> allGlobal) {
        return allGlobal.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    protected abstract String getQualifiedName(T element);
}
