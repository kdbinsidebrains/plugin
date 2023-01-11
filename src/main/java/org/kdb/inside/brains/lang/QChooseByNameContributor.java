package org.kdb.inside.brains.lang;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.indexing.FindSymbolParameters;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.index.QIndexService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class QChooseByNameContributor implements ChooseByNameContributor {
    @Override
    public String @NotNull [] getNames(Project project, boolean includeNonProjectItems) {
        if (project == null) {
            return new String[0];
        }
        final List<String> result = new ArrayList<>();
        QIndexService.getInstance(project).processAllKeys(result::add, FindSymbolParameters.searchScopeFor(project, includeNonProjectItems));
        return ArrayUtilRt.toStringArray(result);
    }

    @Override
    public NavigationItem @NotNull [] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
        final FindSymbolParameters simple = FindSymbolParameters.simple(project, includeNonProjectItems);
        final Collection<? extends NavigationItem> result = QIndexService.getInstance(project).getDeclarations(name, simple.getSearchScope());
        return result.isEmpty() ? NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY : result.toArray(NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY);
    }
}
