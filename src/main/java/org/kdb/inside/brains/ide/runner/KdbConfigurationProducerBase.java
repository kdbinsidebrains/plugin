package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;

import java.util.Objects;

import static com.intellij.util.PathUtil.toSystemIndependentName;

public abstract class KdbConfigurationProducerBase<C extends KdbRunConfigurationBase> extends LazyRunConfigurationProducer<C> {
    protected boolean setupConfigurationFromContext(@NotNull C configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        final String contextScript = getContextScript(context);
        if (contextScript == null) {
            return false;
        }

        final Module module = context.getModule();
        if (module == null) {
            return false;
        }

        configuration.setModule(context.getModule());
        configuration.setScriptName(contextScript);
        configuration.setWorkingDirectory(getWorkingDir(context));
        configuration.setGeneratedName();
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull C configuration, @NotNull ConfigurationContext context) {
        final Module contextModule = context.getModule();
        final Module configModule = configuration.getConfigurationModule().getModule();
        if (!Objects.equals(contextModule, configModule)) {
            return false;
        }

        final String contextScript = toSystemIndependentName(getContextScript(context));
        final String configScript = toSystemIndependentName(configuration.getScriptName());
        if (!StringUtil.equals(contextScript, configScript)) {
            return false;
        }

        final String contextDir = toSystemIndependentName(getWorkingDir(context));
        final String configDir = toSystemIndependentName(configuration.getWorkingDirectory());
        return StringUtil.equals(contextDir, configDir);
    }

    private VirtualFile getContextFile(@NotNull ConfigurationContext context) {
        final Location<?> location = context.getLocation();
        if (location == null) {
            return null;
        }

        final VirtualFile file = location.getVirtualFile();
        if (file == null || !(QFileType.is(file) || file.isDirectory())) {
            return null;
        }
        return file;
    }

    private String getContextScript(@NotNull ConfigurationContext context) {
        final VirtualFile file = getContextFile(context);
        return file == null ? null : file.getPath();
    }

    protected @Nullable String getWorkingDir(@NotNull ConfigurationContext context) {
        final VirtualFile file = getContextFile(context);
        if (file == null) {
            return null;
        }

        final ProjectFileIndex instance = ProjectFileIndex.getInstance(context.getProject());
        VirtualFile folder = instance.getSourceRootForFile(file);
        if (folder == null) {
            folder = instance.getContentRootForFile(file);
        }
        if (folder != null) {
            return folder.getCanonicalPath();
        }
        return file.getParent().getCanonicalPath();
    }
}