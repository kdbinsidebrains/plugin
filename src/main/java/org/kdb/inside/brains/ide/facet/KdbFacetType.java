package org.kdb.inside.brains.ide.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KdbFacetType extends FacetType<KdbFacet, KdbFacetConfiguration> {
    public static final String ID = "KDB_FACET_TYPE";

    public final static FacetTypeId<KdbFacet> TYPE_ID = new FacetTypeId<>(ID);

    public KdbFacetType() {
        super(TYPE_ID, ID, "KDB+ Q Language");
    }

    @Override
    public KdbFacetConfiguration createDefaultConfiguration() {
        return new KdbFacetConfiguration();
    }

    @Override
    public KdbFacet createFacet(@NotNull Module module, String name, @NotNull KdbFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return new KdbFacet(this, module, name, configuration, underlyingFacet);
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        // Can be attached to any module.
        return true;
    }

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.Module;
    }

    public static KdbFacetType getInstance() {
        return findInstance(KdbFacetType.class);
    }

    public static boolean isEnabled(@NotNull Project project) {
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (FacetManager.getInstance(module).getFacetByType(TYPE_ID) != null) {
                return true;
            }
        }
        return false;
    }
}
