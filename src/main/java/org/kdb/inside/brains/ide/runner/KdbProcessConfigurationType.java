package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import icons.KdbIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

import javax.swing.*;

public class KdbProcessConfigurationType implements ConfigurationType {
    private final KdbProcessFactory configurationFactory;

    public KdbProcessConfigurationType() {
        this.configurationFactory = new KdbProcessFactory(this);
    }

    @NotNull
    @Override
    public String getId() {
        return "Kdb.RunLocalProcess";
    }

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.Application;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "KDB Instance";
    }

    @Nls
    @Override
    public String getConfigurationTypeDescription() {
        return "Run local KDB instance";
    }

    public KdbProcessFactory getConfigurationFactory() {
        return configurationFactory;
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{
                configurationFactory
        };
    }

    public static KdbProcessConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(KdbProcessConfigurationType.class);
    }

    private static class KdbProcessFactory extends ConfigurationFactory {
        @Override
        public @NotNull String getId() {
            return "Kdb.RunLocalProcess.Factory";
        }

        @Override
        public @NotNull String getName() {
            return super.getName();
        }

        protected KdbProcessFactory(@NotNull ConfigurationType type) {
            super(type);
        }

        @Override
        public boolean isApplicable(@NotNull Project project) {
            return FileTypeIndex.containsFileOfType(QFileType.INSTANCE, GlobalSearchScope.allScope(project));
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new KdbProcessRunConfiguration(project, this);
        }
    }
}
