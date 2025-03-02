package org.kdb.inside.brains.lang.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.facet.KdbFacetType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class QSpecLibraryRootsProvider extends AdditionalLibraryRootsProvider {
    public static final List<SyntheticLibrary> NO_LIBRARIES = Collections.emptyList();

    @Override
    public @NotNull Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
        if (!KdbFacetType.isEnabled(project)) {
            return NO_LIBRARIES;
        }

        try {
            return List.of(QSpecLibraryService.getInstance().getValidLibrary());
        } catch (RuntimeConfigurationException e) {
            return NO_LIBRARIES;
        }
    }
}
