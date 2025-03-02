package org.kdb.inside.brains.lang.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class QSpecLibrary extends SyntheticLibrary implements ItemPresentation {
    private final Path location;
//    private final List<VirtualFile> sourceRoots;

    public static final String LIBRARY_NAME = "Nugend QSpec";

    public QSpecLibrary(Path location) {
        this.location = location;
//        this.sourceRoots = List.of(rootFile);
    }

    public static void validatePath(Path path) throws RuntimeConfigurationException {
        if (path == null) {
            throw new RuntimeConfigurationException("QSpec path is not provided");
        }
        if (!Files.exists(path)) {
            throw new RuntimeConfigurationException("QSpec path doesn't exist");
        }
        if (!Files.exists(path.resolve("lib/init.q"))) {
            throw new RuntimeConfigurationException("QSpec/lib/init.q file doesn't exist");
        }
    }

    @Override
    public String getPresentableText() {
        return LIBRARY_NAME;
    }

    @Override
    public @Nullable Icon getIcon(boolean unused) {
        return KdbIcons.Main.Library;
    }

    public Path getLocation() {
        return location;
    }

    @Override
    public @Nullable String getLocationString() {
        return location.toString();
    }

    @Override
    public @NotNull Collection<VirtualFile> getSourceRoots() {
        final VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(location);
        return file == null ? List.of() : List.of(file);
    }

    public QSpecLibrary validate() throws RuntimeConfigurationException {
        validatePath(location);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QSpecLibrary that)) return false;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }
}
