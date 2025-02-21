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
import org.kdb.inside.brains.ide.qspec.QSpecLibrary;
import org.kdb.inside.brains.ide.qspec.QSpecLibraryService;
import org.kdb.inside.brains.ide.runner.KdbRunConfigurationBase;

import java.nio.file.Files;
import java.nio.file.Path;

public class QSpecRunConfiguration extends KdbRunConfigurationBase {
    private String expectationPattern;
    private String specificationPattern;
    private QSpecLibrary library;
    private static final boolean DEFAULT_INHERIT_LIBRARY = true;
    private static final boolean DEFAULT_KEEP_FAILED_INSTANCE = false;
    private static final String EXPECTATION_PATTERN = "expectation";
    private static final String SPECIFICATION_PATTERN = "specification";
    private static final String INHERIT_LIBRARY = "inherit_library";
    private static final String KEEP_FAILED_INSTANCE = "keep_failed_instance";
    private boolean inheritLibrary = DEFAULT_INHERIT_LIBRARY;
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

        final String il = JDOMExternalizerUtil.readCustomField(element, INHERIT_LIBRARY);
        inheritLibrary = il == null ? DEFAULT_INHERIT_LIBRARY : Boolean.parseBoolean(il);

        final String kfi = JDOMExternalizerUtil.readCustomField(element, KEEP_FAILED_INSTANCE);
        keepFailedInstance = kfi == null ? DEFAULT_KEEP_FAILED_INSTANCE : Boolean.parseBoolean(kfi);
        library = QSpecLibrary.read(element);
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        addNonEmptyElement(element, EXPECTATION_PATTERN, expectationPattern);
        addNonEmptyElement(element, SPECIFICATION_PATTERN, specificationPattern);
        addNonEmptyElement(element, INHERIT_LIBRARY, String.valueOf(inheritLibrary));
        addNonEmptyElement(element, KEEP_FAILED_INSTANCE, String.valueOf(keepFailedInstance));
        if (library != null) {
            library.write(element);
        }
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();

        final QSpecLibrary activeLibrary = getActiveLibrary();
        if (activeLibrary == null) {
            throw new RuntimeConfigurationException("QSpec library is not specified");
        } else {
            try {
                activeLibrary.validate();
            } catch (Exception ex) {
                throw new RuntimeConfigurationException(ex.getMessage(), ex);
            }
        }
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

    public QSpecLibrary getLibrary() {
        return library;
    }

    public void setLibrary(QSpecLibrary library) {
        this.library = library;
    }

    public boolean isInheritLibrary() {
        return inheritLibrary;
    }

    public void setInheritLibrary(boolean inheritLibrary) {
        this.inheritLibrary = inheritLibrary;
    }

    public boolean isKeepFailedInstance() {
        return keepFailedInstance;
    }

    public void setKeepFailedInstance(boolean keepFailedInstance) {
        this.keepFailedInstance = keepFailedInstance;
    }

    /**
     * Returns the current library of the settings library if the configuration has no own.
     *
     * @return the current library of the settings library if the configuration has no own.
     */
    public QSpecLibrary getActiveLibrary() {
        return inheritLibrary ? QSpecLibraryService.getInstance().getLibrary() : library;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        return createRunningState(env);
    }

    public @NotNull QSpecRunningState createRunningState(@NotNull ExecutionEnvironment env) throws ExecutionException {
        return new QSpecRunningState(this, getExecutionModule(), env);
    }
}