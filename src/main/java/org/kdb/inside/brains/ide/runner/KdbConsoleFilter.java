package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KdbConsoleFilter implements Filter {
    public KdbConsoleFilter(@NotNull Project project, Module module, String workingDirectory) {
    }

    @Override
    public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
        return null;
    }
}
