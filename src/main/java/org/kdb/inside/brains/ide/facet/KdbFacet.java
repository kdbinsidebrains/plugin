package org.kdb.inside.brains.ide.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KdbFacet extends Facet<KdbFacetConfiguration> {
    public KdbFacet(@NotNull FacetType facetType, @NotNull Module module, @NotNull String name, @NotNull KdbFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }

    @Nullable
    public static KdbFacet getInstance(Module module) {
        return FacetManager.getInstance(module).getFacetByType(KdbFacetType.TYPE_ID);
    }
}
