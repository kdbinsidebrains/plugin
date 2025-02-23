package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configuration.AbstractRunConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import java.util.Collection;
import java.util.List;

public abstract class KdbRunConfigurationBase extends AbstractRunConfiguration implements RunConfigurationWithSuppressedDefaultDebugAction {
    private static final String SCRIPT_NAME = "script_name";
    private static final String WORKING_DIRECTORY_NAME = "working_directory";
    private static final String KDB_OPTIONS = "kdb_options";
    private String myScriptName;
    private String myWorkingDirectory;
    private String kdbOptions;

    public KdbRunConfigurationBase(String name, @NotNull Project project, @NotNull ConfigurationFactory factory) {
        this(name, new KdbRunConfigurationModule(project), factory);
    }

    public KdbRunConfigurationBase(String name, @NotNull KdbRunConfigurationModule configurationModule, @NotNull ConfigurationFactory factory) {
        super(name, configurationModule, factory);

        Module module = configurationModule.getModule();
        if (module == null) {
            Collection<Module> modules = getValidModules();
            if (modules.size() == 1) {
                module = ContainerUtil.getFirstItem(modules);
                getConfigurationModule().setModule(module);
            }
        }

        myWorkingDirectory = configurationModule.getProject().getBasePath();
        if (module != null) {
            final VirtualFile virtualFile = ProjectUtil.guessModuleDir(module);
            if (virtualFile != null && virtualFile.getCanonicalPath() != null) {
                myWorkingDirectory = virtualFile.getCanonicalPath();
            }
        }
    }

    public Module getExecutionModule() throws ExecutionException {
        final Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new ExecutionException("Module is not specified");
        }
        return module;
    }

    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        addNonEmptyElement(element, SCRIPT_NAME, myScriptName);
        addNonEmptyElement(element, WORKING_DIRECTORY_NAME, myWorkingDirectory);
        addNonEmptyElement(element, KDB_OPTIONS, kdbOptions);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        myScriptName = JDOMExternalizerUtil.readCustomField(element, SCRIPT_NAME);
        myWorkingDirectory = JDOMExternalizerUtil.readCustomField(element, WORKING_DIRECTORY_NAME);
        kdbOptions = JDOMExternalizerUtil.readCustomField(element, KDB_OPTIONS);
    }

    protected void addNonEmptyElement(@NotNull Element element, @NotNull String attributeName, @Nullable String value) {
        if (StringUtil.isNotEmpty(value)) {
            JDOMExternalizerUtil.writeCustomField(element, attributeName, value);
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (KdbSdkType.getHomePath(getConfigurationModule()) == null) {
            throw new RuntimeConfigurationWarning("KDB SDK is not specified for the module");
        }

        if (myScriptName == null || myScriptName.trim().isEmpty()) {
            throw new RuntimeConfigurationError("Script name is not specified");
        }

        if (myWorkingDirectory == null || myWorkingDirectory.trim().isEmpty()) {
            throw new RuntimeConfigurationError("Working directory is not specified");
        }
    }

    @Override
    public Collection<Module> getValidModules() {
        final Project project = getProject();
        if (project.isDefault()) {
            return List.of();
        }
        return ContainerUtil.filter(ModuleManager.getInstance(project).getModules(), m -> KdbSdkType.getModuleSdk(m) != null);
    }

    public String getScriptName() {
        return myScriptName;
    }

    public void setScriptName(String scriptName) {
        this.myScriptName = scriptName;
    }

    public String getWorkingDirectory() {
        return myWorkingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.myWorkingDirectory = workingDirectory;
    }

    public String getKdbOptions() {
        return kdbOptions;
    }

    public void setKdbOptions(String kdbOptions) {
        this.kdbOptions = kdbOptions;
    }

    @Override
    public boolean isBuildBeforeLaunchAddedByDefault() {
        return false;
    }

    @Override
    public boolean isBuildProjectOnEmptyModuleList() {
        return false;
    }

    @Override
    public boolean isExcludeCompileBeforeLaunchOption() {
        return true;
    }

    public abstract RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException;
}
