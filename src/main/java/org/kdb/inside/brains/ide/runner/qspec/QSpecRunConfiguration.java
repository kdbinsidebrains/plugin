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
import com.intellij.openapi.util.text.StringUtil;
import org.apache.commons.io.FilenameUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.runner.KdbRunConfigurationBase;
import org.kdb.inside.brains.lang.qspec.QSpecLibraryService;

import java.nio.file.Files;
import java.nio.file.Path;

public class QSpecRunConfiguration extends KdbRunConfigurationBase {
    private static final String SCRIPT = "script";
    private static final String SUITE_PATTERN = "suite";
    private static final String TEST_PATTERN = "test";
    private String customScript;
    private static final String KEEP_FAILED_INSTANCE = "keep_failed";
    private static final boolean DEFAULT_KEEP_FAILED = false;
    private String suitePattern;
    private String testPattern;
    private boolean keepFailed = DEFAULT_KEEP_FAILED;

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
        suitePattern = JDOMExternalizerUtil.readCustomField(element, SUITE_PATTERN);
        testPattern = JDOMExternalizerUtil.readCustomField(element, TEST_PATTERN);

        final String kfi = JDOMExternalizerUtil.readCustomField(element, KEEP_FAILED_INSTANCE);
        keepFailed = kfi == null ? DEFAULT_KEEP_FAILED : Boolean.parseBoolean(kfi);

        final Element child = element.getChild(SCRIPT);
        if (child != null) {
            customScript = child.getText();
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        addNonEmptyElement(element, TEST_PATTERN, testPattern);
        addNonEmptyElement(element, SUITE_PATTERN, suitePattern);
        addNonEmptyElement(element, KEEP_FAILED_INSTANCE, String.valueOf(keepFailed));
        if (customScript != null) {
            element.addContent(new Element(SCRIPT).setText(customScript));
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();

        // The result is not important
        QSpecLibraryService.getInstance().getValidLibrary();
    }

    @Override
    public @Nullable @NlsActions.ActionText String suggestedName() {
        final String scriptName = getScriptName();
        if (StringUtil.isEmpty(scriptName)) {
            return null;
        }

        final boolean hasTest = StringUtil.isNotEmpty(testPattern);
        final boolean hasSuite = StringUtil.isNotEmpty(suitePattern);

        if (!hasSuite && !hasTest) {
            return "Tests in '" + FilenameUtils.getName(scriptName) + "'";
        }

        String name = FilenameUtils.getBaseName(scriptName);
        if (hasSuite) {
            name += "." + suitePattern;
        }
        if (hasTest) {
            name += "/" + testPattern;
        }
        return name;
    }

    @Override
    public @Nullable @NlsActions.ActionText String getActionName() {
        final String scriptName = getScriptName();
        if (Files.isDirectory(Path.of(scriptName))) {
            return ProgramRunnerUtil.shortenName("Tests in '" + FilenameUtils.getBaseName(scriptName) + "'", 0);
        }

        if (StringUtil.isNotEmpty(testPattern)) {
            return ProgramRunnerUtil.shortenName(testPattern, 0);
        }
        if (StringUtil.isNotEmpty(suitePattern)) {
            return ProgramRunnerUtil.shortenName(suitePattern, 0);
        }
        return ProgramRunnerUtil.shortenName(getName(), 0);
    }

    public String getTestPattern() {
        return testPattern;
    }

    public void setTestPattern(String testPattern) {
        this.testPattern = testPattern;
    }

    public String getSuitePattern() {
        return suitePattern;
    }

    public void setSuitePattern(String suitePattern) {
        this.suitePattern = suitePattern;
    }

    public boolean isKeepFailed() {
        return keepFailed;
    }

    public void setKeepFailed(boolean keepFailed) {
        this.keepFailed = keepFailed;
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

    public @NotNull QSpecRunningState createRunningState(@NotNull ExecutionEnvironment env) {
        return new QSpecRunningState(this, env);
    }
}