package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImport;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;

public class QReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        QBaseReferenceProvider.register(registrar, QImport.class, new QImportReferenceProvider());
        QBaseReferenceProvider.register(registrar, QSymbol.class, new QSymbolReferenceProvider());
        QBaseReferenceProvider.register(registrar, QVariable.class, new QVariableReferenceProvider());
        QBaseReferenceProvider.register(registrar, QVariable.class, new QSpecReferenceProvider());
    }
}