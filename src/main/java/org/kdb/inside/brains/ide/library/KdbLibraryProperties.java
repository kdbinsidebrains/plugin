package org.kdb.inside.brains.ide.library;

import com.intellij.openapi.roots.libraries.LibraryProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KdbLibraryProperties extends LibraryProperties<KdbLibraryProperties> {
    @Override
    public @Nullable KdbLibraryProperties getState() {
        return null;
    }

    @Override
    public void loadState(@NotNull KdbLibraryProperties state) {
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof KdbLibraryProperties;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
