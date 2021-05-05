package org.kdb.inside.brains.ide.module.facet;

import com.intellij.facet.ProjectFacetListener;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class KdbFacetStateManager implements ProjectFacetListener<KdbFacet> {
    private static final AtomicBoolean FACET_ENABLED = new AtomicBoolean();

    @Override
    public void facetAdded(@NotNull KdbFacet facet) {
        FACET_ENABLED.set(true);
    }

    @Override
    public void facetRemoved(@NotNull KdbFacet facet, @NotNull Project project) {
        FACET_ENABLED.set(false);
    }

    public static boolean isFacetEnabled() {
        return FACET_ENABLED.get();
    }
}
