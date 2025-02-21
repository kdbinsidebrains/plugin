package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

public class QSpecConfigurationType extends ConfigurationTypeBase {
    private static final String TYPE_ID = "Kdb.RunQSpecConfiguration";
    private static final String FACTORY_ID = TYPE_ID + "Factory";

    public QSpecConfigurationType() {
        super(TYPE_ID, "KDB QSpec Test", "KDB QSpec test run configuration", KdbIcons.Main.RunQSpec);
        addFactory(new QSpecConfigurationFactory(this));
    }

    @NotNull
    public static QSpecConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(QSpecConfigurationType.class);
    }

    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return getConfigurationFactories()[0];
    }

    public static class QSpecConfigurationFactory extends ConfigurationFactory {
        protected QSpecConfigurationFactory(@NotNull ConfigurationType type) {
            super(type);
        }

        @Override
        public @NotNull String getId() {
            return FACTORY_ID;
        }

        @Override
        public boolean isApplicable(@NotNull Project project) {
            return FileTypeIndex.containsFileOfType(QFileType.INSTANCE, GlobalSearchScope.allScope(project));
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new QSpecRunConfiguration(project, this);
        }
    }
}