package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
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

import java.util.Objects;

public abstract class KdbConfigurationProducerBase<C extends KdbRunConfigurationBase> extends LazyRunConfigurationProducer<C> {
    protected boolean setupConfigurationFromContext(@NotNull C configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        final Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final VirtualFile file = location.getVirtualFile();
        if (file == null || !(QFileType.is(file) || file.isDirectory())) {
            return false;
        }

        final Module module = context.getModule();
        if (module == null) {
            return false;
        }

        configuration.setModule(context.getModule());
        configuration.setScriptName(getScriptName(file));
        configuration.setWorkingDirectory(getWorkingDir(context, file));
        configuration.setGeneratedName();
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull C configuration, @NotNull ConfigurationContext context) {
        final Module module = context.getModule();
        if (module == null) {
            return false;
        }

        final Location<?> location = context.getLocation();
        if (location == null) {
            return false;
        }

        final VirtualFile file = location.getVirtualFile();
        if (file == null || !(QFileType.is(file) || file.isDirectory())) {
            return false;
        }

        final Sdk sdk = KdbSdkType.getModuleSdk(context.getModule());
        if (sdk == null) {
            return false;
        }

        return Objects.equals(module, configuration.getConfigurationModule().getModule()) &&
                StringUtil.equals(getScriptName(file), configuration.getScriptName()) &&
                StringUtil.equals(PathUtil.toSystemIndependentName(getWorkingDir(context, file)), PathUtil.toSystemIndependentName(configuration.getWorkingDirectory()));
    }

    protected @NotNull String getScriptName(VirtualFile file) {
        return file.getPath();
    }

    protected @Nullable String getWorkingDir(@NotNull ConfigurationContext context, VirtualFile file) {
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