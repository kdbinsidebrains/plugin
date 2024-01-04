package org.kdb.inside.brains.psi.index;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QPsiUtil;
import org.kdb.inside.brains.psi.QVarDeclaration;

import java.util.*;
import java.util.function.Predicate;

public class QIndexService {
    private final Project project;
    private final FileBasedIndex index;

    public QIndexService(Project project) {
        this.project = project;
        index = FileBasedIndex.getInstance();
    }

    public String firstMatch(@NotNull Predicate<String> keyPredicate, @NotNull GlobalSearchScope scope) {
        final Result<String> result = new Result<>();
        processAllKeys(s -> {
            if (keyPredicate.test(s)) {
                result.value = s;
                return false;
            }
            return true;
        }, scope);
        return result.value;
    }

    public QVarDeclaration getFirstInFile(@NotNull String qualifiedName, @NotNull PsiFile file) {
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

    public @Nullable QVarDeclaration getFirstGlobalDeclarations(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        final Result<QVarDeclaration> r = new Result<>();
        processValues(s -> s.equals(qualifiedName), scope, (key, file, descriptor) -> {
            final QVarDeclaration var = resolveGlobalDeclaration(descriptor, file);
            if (var != null) {
                r.value = var;
                return false;
            }
            return true;
        });
        return r.value;
    }

    public @NotNull Collection<QVarDeclaration> getDeclarations(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        final Set<QVarDeclaration> declarations = new HashSet<>();
        processValues(s -> s.equals(qualifiedName), scope, (key, file, descriptor) -> {
            final QVarDeclaration var = resolveDeclaration(descriptor, file);
            if (var != null) {
                declarations.add(var);
            }
            return true;
        });
        return declarations;
    }

    @Nullable
    private QVarDeclaration resolveGlobalDeclaration(IdentifierDescriptor descriptor, VirtualFile file) {
        final QVarDeclaration var = resolveDeclaration(descriptor, file);
        if (var != null && QPsiUtil.isGlobalDeclaration(var)) {
            return var;
        }
        return null;
    }

    public void processValues(@NotNull Predicate<String> keyPredicate, @NotNull GlobalSearchScope scope, @NotNull QIndexService.ValuesProcessor processor) {
        // We can't process all in the same thread so we collect value values firstly and then process each
        // See https://github.com/kdbinsidebrains/plugin/issues/76
        final List<String> keys = new ArrayList<>();
        processAllKeys(key -> {
            if (keyPredicate.test(key)) {
                keys.add(key);
            }
            return true;
        }, scope);

        for (String key : keys) {
            final boolean result = index.processValues(QIdentifiersIndex.INDEX_ID, key, null, (file, value) -> {
                for (IdentifierDescriptor d : value) {
                    if (!processor.processValues(key, file, d)) {
                        return false;
                    }
                }
                return true;
            }, scope);

            if (!result) {
                break;
            }
        }
    }

    @Nullable
    private QVarDeclaration resolveDeclaration(IdentifierDescriptor descriptor, VirtualFile file) {
        if (descriptor.isSymbol()) {
            return null;
        }

        final PsiManager psiManager = PsiManager.getInstance(project);
        final PsiFile pf = psiManager.findFile(file);
        if (pf == null) {
            return null;
        }

        final PsiElement el = pf.findElementAt(descriptor.range().getStartOffset());
        if (el == null) {
            return null;
        }

        if (el instanceof QVarDeclaration) {
            return (QVarDeclaration) el;
        }

        final PsiElement parent = el.getParent();
        if (parent instanceof QVarDeclaration) {
            return (QVarDeclaration) parent;
        }
        return null;
    }

    public void processAllKeys(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope) {
        index.processAllKeys(QIdentifiersIndex.INDEX_ID, processor, scope.getProject());
    }

    public static QIndexService getInstance(Project project) {
        return project.getService(QIndexService.class);
    }

    public static QIndexService getInstance(PsiElement element) {
        return getInstance(element.getProject());
    }

    @FunctionalInterface
    public interface ValuesProcessor {
        boolean processValues(String key, VirtualFile file, IdentifierDescriptor descriptor);
    }

    private static class Result<T> {
        private T value;
    }
}