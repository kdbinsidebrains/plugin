package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
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
import com.intellij.util.xmlb.annotations.Transient;
import com.jgoodies.common.base.Strings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class KdbRunConfigurationBase extends ModuleBasedConfiguration<KdbRunConfigurationModule, Element> implements RunConfigurationWithSuppressedDefaultDebugAction {
    private static final String SCRIPT_NAME = "script_name";
    private String myScriptName;
    private String myWorkingDirectory;
    private static final String KDB_OPTIONS = "kdb_options";
    private static final String WORKING_DIRECTORY_NAME = "working_directory";
    private static final String ELEMENT_ENVS = "envs";
    private static final String ELEMENT_ENV = "env";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_PASS_PARENT = "passParent";
    private static final boolean DEFAULT_PASS_PARENT = true;
    private final Map<String, String> myEnvs = new LinkedHashMap<>();
    private String kdbOptions;
    private boolean myPassParentEnvs = DEFAULT_PASS_PARENT;

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

    @Transient
    public @Nullable Module getExecutionModule() {
        return getConfigurationModule().getModule();
    }

    @Transient
    public void setExecutionModule(@Nullable Module module) {
        getConfigurationModule().setModule(module);
    }

    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);

        addNonEmptyElement(element, SCRIPT_NAME, myScriptName);
        addNonEmptyElement(element, KDB_OPTIONS, kdbOptions);
        addNonEmptyElement(element, WORKING_DIRECTORY_NAME, myWorkingDirectory);

        final Element e = new Element(ELEMENT_ENVS);
        e.setAttribute(ATTRIBUTE_PASS_PARENT, String.valueOf(myPassParentEnvs));
        for (Map.Entry<String, String> entry : myEnvs.entrySet()) {
            e.addContent(new Element(ELEMENT_ENV).setAttribute(ATTRIBUTE_NAME, entry.getKey()).setText(entry.getValue()));
        }
        element.addContent(e);
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        myScriptName = JDOMExternalizerUtil.readCustomField(element, SCRIPT_NAME);
        kdbOptions = JDOMExternalizerUtil.readCustomField(element, KDB_OPTIONS);
        myWorkingDirectory = JDOMExternalizerUtil.readCustomField(element, WORKING_DIRECTORY_NAME);

        myEnvs.clear();
        myPassParentEnvs = DEFAULT_PASS_PARENT;

        final Element es = element.getChild(ELEMENT_ENVS);
        if (es != null) {
            myPassParentEnvs = Boolean.parseBoolean(es.getAttributeValue(ATTRIBUTE_PASS_PARENT, "true"));
            for (Element e : es.getChildren(ELEMENT_ENV)) {
                final String name = e.getAttributeValue(ATTRIBUTE_NAME);
                if (Strings.isNotEmpty(name)) {
                    myEnvs.put(name, e.getText());
                }
            }
        }
    }

    protected void addNonEmptyElement(@NotNull Element element, @NotNull String attributeName, @Nullable String value) {
        if (StringUtil.isNotEmpty(value)) {
            JDOMExternalizerUtil.writeCustomField(element, attributeName, value);
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        final Module module = getExecutionModule();
        if (module == null) {
            throw new RuntimeConfigurationWarning("KDB SDK is not specified for the module");
        }

        if (!KdbSdkType.hasValidSdk(module)) {
            throw new RuntimeConfigurationWarning("KDB SDK is not correct");
        }

        if (StringUtil.isEmpty(myScriptName)) {
            throw new RuntimeConfigurationError("Script name is not specified");
        }

        if (StringUtil.isEmpty(myWorkingDirectory)) {
            throw new RuntimeConfigurationError("Working directory is not specified");
        }
    }

    @Override
    public Collection<Module> getValidModules() {
        final Project project = getProject();
        if (project.isDefault()) {
            return List.of();
        }
        return ContainerUtil.filter(ModuleManager.getInstance(project).getModules(), KdbSdkType::hasValidSdk);
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
        return DEFAULT_PASS_PARENT;
    }

    @NotNull
    public Map<String, String> getEnvs() {
        return myEnvs;
    }

    public void setEnvs(@NotNull final Map<String, String> envs) {
        myEnvs.clear();
        myEnvs.putAll(envs);
    }

    public boolean isPassParentEnvs() {
        return myPassParentEnvs;
    }

    public void setPassParentEnvs(final boolean passParentEnvs) {
        myPassParentEnvs = passParentEnvs;
    }

    public abstract RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException;
}
