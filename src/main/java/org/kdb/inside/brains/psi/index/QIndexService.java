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
import org.kdb.inside.brains.psi.QVariable;

import java.util.function.Predicate;

public class QIndexService {
    private final Project project;
    private final FileBasedIndex index;

    public QIndexService(Project project) {
        this.project = project;
        index = FileBasedIndex.getInstance();
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
        final PsiManager psiManager = PsiManager.getInstance(project);
        processValues(keyPredicate, scope, (key, file, descriptor) -> {
            final PsiFile pf = psiManager.findFile(file);
            if (pf == null) {
                return;
            }

            final PsiElement el = pf.findElementAt(descriptor.getRange().getStartOffset());
            if (el == null) {
                return;
            }

            final PsiElement parent = el.getParent();
            if (parent instanceof QVariable) {
                processor.processVariables(key, file, descriptor, (QVariable) parent);
            }
        });
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

    @FunctionalInterface
    public interface ValuesProcessor {
        void processValues(String key, VirtualFile file, IdentifierDescriptor descriptor);
    }

    @FunctionalInterface
    public interface VariablesProcessor {
        void processVariables(String key, VirtualFile file, IdentifierDescriptor descriptor, QVariable variable);
    }
}