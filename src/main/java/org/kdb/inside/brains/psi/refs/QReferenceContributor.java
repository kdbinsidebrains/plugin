package org.kdb.inside.brains.psi.refs;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QImportFile;
import org.kdb.inside.brains.psi.QSymbol;
import org.kdb.inside.brains.psi.QVariable;

public class QReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(QSymbol.class), new QSymbolReferenceProvider());
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(QVariable.class), new QVariableReferenceProvider());
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(QImportFile.class), new QImportReferenceProvider());
    }
}