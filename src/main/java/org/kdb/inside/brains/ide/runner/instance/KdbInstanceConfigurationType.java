package org.kdb.inside.brains.ide.runner.instance;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QFileType;

public class KdbInstanceConfigurationType extends ConfigurationTypeBase {
    private static final String TYPE_ID = "Kdb.RunInstanceConfiguration";
    private static final String FACTORY_ID = TYPE_ID + "Factory";

    public KdbInstanceConfigurationType() {
        super(TYPE_ID, "KDB Instance", "Run local KDB instance", KdbIcons.Main.RunFile);
        addFactory(new KdbProcessFactory(this));
    }

    public static KdbInstanceConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(KdbInstanceConfigurationType.class);
    }

    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return getConfigurationFactories()[0];
    }

    public static class KdbProcessFactory extends ConfigurationFactory {
        protected KdbProcessFactory(@NotNull ConfigurationType type) {
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
            return new KdbInstanceRunConfiguration(project, this);
        }
    }
}
