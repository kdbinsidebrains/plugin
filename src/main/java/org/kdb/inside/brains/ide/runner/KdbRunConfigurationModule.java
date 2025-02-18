package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class KdbRunConfigurationModule extends RunConfigurationModule {
    public KdbRunConfigurationModule(@NotNull Project project) {
        super(project);
    }
}
