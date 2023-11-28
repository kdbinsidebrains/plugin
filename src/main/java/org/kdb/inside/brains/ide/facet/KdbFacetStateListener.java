package org.kdb.inside.brains.ide.facet;

import com.intellij.facet.ProjectFacetListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.KdbToolWindowManager;

public class KdbFacetStateListener implements ProjectFacetListener<KdbFacet> {
    @Override
    public void firstFacetAdded(@NotNull Project project) {
        KdbToolWindowManager.enable(project);
    }

    @Override
    public void allFacetsRemoved(@NotNull Project project) {
        KdbToolWindowManager.disable(project);
    }
}