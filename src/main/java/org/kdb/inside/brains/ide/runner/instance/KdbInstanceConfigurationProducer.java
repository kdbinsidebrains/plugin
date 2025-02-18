package org.kdb.inside.brains.ide.runner.instance;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.runner.KdbConfigurationProducerBase;
import org.kdb.inside.brains.psi.QFile;

public class KdbInstanceConfigurationProducer extends KdbConfigurationProducerBase<KdbInstanceRunConfiguration> {
    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return KdbInstanceConfigurationType.getInstance().getConfigurationFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull KdbInstanceRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        if (!super.setupConfigurationFromContext(configuration, context, sourceElement)) {
            return false;
        }
        final Location<PsiElement> location = context.getLocation();
        return location != null && location.getPsiElement() instanceof QFile;
    }
}