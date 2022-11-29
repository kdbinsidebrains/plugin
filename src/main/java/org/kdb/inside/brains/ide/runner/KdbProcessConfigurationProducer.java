package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

public class KdbProcessConfigurationProducer extends LazyRunConfigurationProducer<KdbProcessRunConfiguration> {
    private static @Nullable String getWorkingDir(@NotNull ConfigurationContext context, VirtualFile file) {
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

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return KdbProcessConfigurationType.getInstance().getConfigurationFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull KdbProcessRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        final Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final Module module = context.getModule();
        if (module == null) {
            return false;
        }

        final VirtualFile file = location.getVirtualFile();
        if (!QFileType.is(file)) {
            return false;
        }

        configuration.setName(file.getName());
        configuration.setModule(context.getModule());
        configuration.setMainClassName(file.getCanonicalPath());
        configuration.setWorkingDirectory(getWorkingDir(context, file));
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull KdbProcessRunConfiguration configuration, @NotNull ConfigurationContext context) {
        final Module module = context.getModule();
        if (module == null) {
            return false;
        }

        final Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final VirtualFile file = location.getVirtualFile();
        if (!QFileType.is(file)) {
            return false;
        }

        final Sdk sdk = KdbSdkType.getModuleSdk(context.getModule());
        if (sdk == null) {
            return false;
        }

        final String workingDir = getWorkingDir(context, file);
        return module.equals(configuration.getConfigurationModule().getModule())
                && StringUtil.equals(workingDir, PathUtil.toSystemIndependentName(configuration.getWorkingDirectory()))
                && StringUtil.equals(file.getCanonicalPath(), configuration.getMainClassName());
    }
}
