package org.kdb.inside.brains.lang;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.index.QIndexService;

public final class QChooseByNameContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        final Project project = scope.getProject();
        if (project == null) {
            return;
        }
        QIndexService.getInstance(project).processAllKeys(processor, scope, filter);
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        final Project project = parameters.getProject();
        final GlobalSearchScope searchScope = parameters.getSearchScope();
        QIndexService.getInstance(project).processVariables(s -> s.equals(name), searchScope, (key, file, descriptor, variable) -> processor.process(variable));
    }
}
