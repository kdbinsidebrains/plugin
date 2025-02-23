package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.WriteExternalException;
import org.apache.commons.io.FilenameUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.runner.KdbRunConfigurationBase;

import java.nio.file.Files;
import java.nio.file.Path;

public class QSpecRunConfiguration extends KdbRunConfigurationBase {
    private static final String SCRIPT = "script";
    private String expectationPattern;
    private String specificationPattern;
    private static final boolean DEFAULT_KEEP_FAILED_INSTANCE = false;
    private String customScript;
    private static final String EXPECTATION_PATTERN = "expectation";
    private static final String SPECIFICATION_PATTERN = "specification";
    private static final String KEEP_FAILED_INSTANCE = "keep_failed_instance";
    private boolean keepFailedInstance = DEFAULT_KEEP_FAILED_INSTANCE;

    public QSpecRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super("KDB QSpec Test Run Configuration", project, factory);
    }

    @NotNull
    @Override
    public SettingsEditor<QSpecRunConfiguration> getConfigurationEditor() {
        return new QSpecRunSettingsEditor(getProject());
    }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        expectationPattern = JDOMExternalizerUtil.readCustomField(element, EXPECTATION_PATTERN);
        specificationPattern = JDOMExternalizerUtil.readCustomField(element, SPECIFICATION_PATTERN);

        final String kfi = JDOMExternalizerUtil.readCustomField(element, KEEP_FAILED_INSTANCE);
        keepFailedInstance = kfi == null ? DEFAULT_KEEP_FAILED_INSTANCE : Boolean.parseBoolean(kfi);

        final Element child = element.getChild(SCRIPT);
        if (child != null) {
            customScript = child.getText();
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        addNonEmptyElement(element, EXPECTATION_PATTERN, expectationPattern);
        addNonEmptyElement(element, SPECIFICATION_PATTERN, specificationPattern);
        addNonEmptyElement(element, KEEP_FAILED_INSTANCE, String.valueOf(keepFailedInstance));
        addNonEmptyElement(element, KEEP_FAILED_INSTANCE, String.valueOf(keepFailedInstance));
        if (customScript != null) {
            element.addContent(new Element(SCRIPT).setText(customScript));
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();

        // Result is not important
        QSpecLibraryService.getInstance().getValidLibrary();
    }

    @Override
    public @Nullable @NlsActions.ActionText String suggestedName() {
        final String scriptName = getScriptName();
        if (scriptName == null || scriptName.isEmpty()) {
            return null;
        }
        String name = FilenameUtils.getBaseName(scriptName);
        if (specificationPattern != null && !specificationPattern.isEmpty()) {
            name += "." + specificationPattern;
        }
        if (expectationPattern != null && !expectationPattern.isEmpty()) {
            name += "/" + expectationPattern;
        }
        return name;
    }

    @Override
    public @Nullable @NlsActions.ActionText String getActionName() {
        final String scriptName = getScriptName();
        if (Files.isDirectory(Path.of(scriptName))) {
            return ProgramRunnerUtil.shortenName("Tests in '" + FilenameUtils.getBaseName(scriptName) + "'", 0);
        }

        if (expectationPattern != null && !expectationPattern.isEmpty()) {
            return ProgramRunnerUtil.shortenName(expectationPattern, 0);
        }
        if (specificationPattern != null && !specificationPattern.isEmpty()) {
            return ProgramRunnerUtil.shortenName(specificationPattern, 0);
        }
        return ProgramRunnerUtil.shortenName(getName(), 0);
    }

    public String getExpectationPattern() {
        return expectationPattern;
    }

    public void setExpectationPattern(String expectationPattern) {
        this.expectationPattern = expectationPattern;
    }

    public String getSpecificationPattern() {
        return specificationPattern;
    }

    public void setSpecificationPattern(String specificationPattern) {
        this.specificationPattern = specificationPattern;
    }

    public boolean isKeepFailedInstance() {
        return keepFailedInstance;
    }

    public void setKeepFailedInstance(boolean keepFailedInstance) {
        this.keepFailedInstance = keepFailedInstance;
    }

    public String getCustomScript() {
        return customScript;
    }

    public void setCustomScript(String customScript) {
        this.customScript = customScript;
    }

    /**
     * Returns the current library of the settings library if the configuration has no own.
     *
     * @return the current library of the settings library if the configuration has no own.
     */
    public String getActiveCustomScript() {
        return customScript != null ? customScript : QSpecLibraryService.getInstance().getCustomScript();
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        return createRunningState(env);
    }

    public @NotNull QSpecRunningState createRunningState(@NotNull ExecutionEnvironment env) throws ExecutionException {
        return new QSpecRunningState(this, getExecutionModule(), env);
    }
}