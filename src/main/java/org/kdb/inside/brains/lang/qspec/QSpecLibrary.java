package org.kdb.inside.brains.lang.qspec;

import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.util.text.StringUtil;
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

    private List<VirtualFile> sourceRoots;

    public static final String LIBRARY_NAME = "Nugend QSpec";

    public QSpecLibrary(Path location) {
        this.location = location;
    }

    public static void validatePath(String path) throws RuntimeConfigurationException {
        validatePath(path == null ? null : Path.of(path));
    }

    public static void validatePath(Path path) throws RuntimeConfigurationException {
        if (path == null) {
            throw new RuntimeConfigurationException("QSpec path is not provided");
        }

        if (!Files.exists(path)) {
            throw new RuntimeConfigurationException("QSpec path directory doesn't exist");
        }
        if (!Files.isDirectory(path)) {
            throw new RuntimeConfigurationException("QSpec path must be a directory");
        }
        if (!Files.exists(path.resolve("lib/init.q"))) {
            throw new RuntimeConfigurationException("The lib/init.q file doesn't exist");
        }
    }

    public static @Nullable QSpecLibrary of(@Nullable String libraryPath) {
        return StringUtil.isEmpty(libraryPath) ? null : new QSpecLibrary(Path.of(libraryPath));
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
        if (sourceRoots == null) {
            if (!Files.isDirectory(location)) {
                sourceRoots = List.of();
            } else {
                final VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(location);
                sourceRoots = file == null ? List.of() : List.of(file);
            }
        }
        return sourceRoots;
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

    @Override
    public String toString() {
        return "QSpecLibrary{" +
                "path=" + location +
                "} " + super.toString();
    }
}
