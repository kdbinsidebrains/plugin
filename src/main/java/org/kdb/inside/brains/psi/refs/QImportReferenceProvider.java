package org.kdb.inside.brains.psi.refs;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class QImportReferenceProvider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        TextRange range = ElementManipulators.getValueTextRange(element);
        final int startOffset = range.getStartOffset();
        final String text = range.substring(element.getText()).trim();

        return new FileReferenceSet(text, element, startOffset, null, true, true) {
            @Override
            public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
                final PsiFile originalFile = element.getContainingFile().getOriginalFile();
                final Module module = ModuleUtilCore.findModuleForFile(originalFile);
                return getRoots(module);
            }
        }.getAllReferences();
    }

    @NotNull
    private static Collection<PsiFileSystemItem> getRoots(@Nullable final Module thisModule) {
        if (thisModule == null) {
            return List.of();
        }

        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(thisModule);
        final PsiManager psiManager = PsiManager.getInstance(thisModule.getProject());

        final VirtualFile[] sourceRoots = moduleRootManager.orderEntries().recursively()
                .withoutSdk().withoutLibraries()
                .sources().usingCache().getRoots();

        final Set<PsiFileSystemItem> result = new LinkedHashSet<>(sourceRoots.length);
        for (VirtualFile root : sourceRoots) {
            final PsiDirectory directory = psiManager.findDirectory(root);
            if (directory != null) {
                result.add(directory);
            }
        }
        return result;
    }
}
