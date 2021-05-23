package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QVarDeclaration;

import java.util.List;
import java.util.function.Predicate;

public class QIndexService {
    private final Project project;
    private final FileBasedIndex index;

    public QIndexService(Project project) {
        this.project = project;
        index = FileBasedIndex.getInstance();
    }

    public QVarDeclaration findFirstInFile(@NotNull String qualifiedName, @NotNull PsiFile file) {
        final List<List<IdentifierDescriptor>> values = index.getValues(QIdentifiersIndex.INDEX_ID, qualifiedName, GlobalSearchScope.fileScope(file));
        for (List<IdentifierDescriptor> value : values) {
            for (IdentifierDescriptor descriptor : value) {
                final QVarDeclaration declaration = resolveDeclaration(descriptor, file.getVirtualFile());
                if (declaration != null) {
                    return declaration;
                }
            }
        }
        return null;
    }

    public void processValues(@NotNull Predicate<String> keyPredicate, @NotNull GlobalSearchScope scope, @NotNull QIndexService.ValuesProcessor processor) {
        processAllKeys(key -> {
            if (keyPredicate.test(key)) {
                index.processValues(QIdentifiersIndex.INDEX_ID, key, null, (file, value) -> {
                    for (IdentifierDescriptor d : value) {
                        processor.processValues(key, file, d);
                    }
                    return true;
                }, scope);
            }
            return true;
        }, scope);
    }

    public void processVariables(@NotNull Predicate<String> keyPredicate, @NotNull GlobalSearchScope scope, @NotNull QIndexService.VariablesProcessor processor) {
        processValues(keyPredicate, scope, (key, file, descriptor) -> {
            final QVarDeclaration var = resolveDeclaration(descriptor, file);
            if (var != null) {
                processor.processVariables(key, file, descriptor, var);
            }
        });
    }

    @Nullable
    private QVarDeclaration resolveDeclaration(IdentifierDescriptor descriptor, VirtualFile file) {
        final PsiManager psiManager = PsiManager.getInstance(project);
        final PsiFile pf = psiManager.findFile(file);
        if (pf == null) {
            return null;
        }

        final PsiElement el = pf.findElementAt(descriptor.getRange().getStartOffset());
        if (el == null) {
            return null;
        }

        final PsiElement parent = el.getParent();
        if (parent instanceof QVarDeclaration) {
            return (QVarDeclaration) parent;
        }
        return null;
    }

    public void processAllKeys(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope) {
        processAllKeys(processor, scope, null);
    }

    public void processAllKeys(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        index.processAllKeys(QIdentifiersIndex.INDEX_ID, processor, scope, filter);
    }

    public static QIndexService getInstance(Project project) {
        return project.getService(QIndexService.class);
    }

    public static QIndexService getInstance(PsiElement element) {
        return getInstance(element.getProject());
    }


    @FunctionalInterface
    public interface ValuesProcessor {
        void processValues(String key, VirtualFile file, IdentifierDescriptor descriptor);
    }

    @FunctionalInterface
    public interface VariablesProcessor {
        void processVariables(String key, VirtualFile file, IdentifierDescriptor descriptor, QVarDeclaration variable);
    }
}