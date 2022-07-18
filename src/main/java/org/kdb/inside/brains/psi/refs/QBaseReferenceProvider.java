package org.kdb.inside.brains.psi.refs;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public abstract class QBaseReferenceProvider<E extends PsiElement> extends PsiReferenceProvider {
    protected final Class<E> aClass;

    protected QBaseReferenceProvider(Class<E> aClass) {
        this.aClass = aClass;
    }

    protected abstract PsiReference @NotNull [] getElementReferences(@NotNull E element, @NotNull ProcessingContext context);

    @Override
    public final PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!aClass.isAssignableFrom(element.getClass())) {
            return PsiReference.EMPTY_ARRAY;
        }
        return getElementReferences(aClass.cast(element), context);
    }

    public static <T extends PsiElement> void register(PsiReferenceRegistrar registrar, Class<T> type, QBaseReferenceProvider<T> provider) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(type), provider);
    }
}