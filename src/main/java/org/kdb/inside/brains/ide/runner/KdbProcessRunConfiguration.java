package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.*;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.execution.util.ScriptFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.view.LineNumberGutterProvider;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class KdbProcessRunConfiguration extends ApplicationConfiguration implements RefactoringListenerProvider {
    protected KdbProcessRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory) {
        super("Kdb Script Run Configuration", project, factory);
    }

    @Nullable
    @Override
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        final String scriptName = getOptions().getMainClassName();
        final String pathByElement = getPathByElement(element);
        if (scriptName == null || pathByElement == null) {
            return null;
        }

        final String independentName = FileUtil.toSystemIndependentName(scriptName);
        final String independentPathByElement = FileUtil.toSystemIndependentName(pathByElement);
        if (!independentName.equals(independentPathByElement)) {
            return null;
        }

        if (!(element instanceof QFile)) {
            return null;
        }

        return new RefactoringElementAdapter() {
            @Override
            protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                if (newElement instanceof QFile) {
                    final QFile file = (QFile) newElement;
                    final String scriptFilePath = ScriptFileUtil.getScriptFilePath(file.getVirtualFile());
                    getOptions().setMainClassName(FileUtil.toSystemDependentName(scriptFilePath));
                }
            }

            @Override
            public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                elementRenamedOrMoved(newElement);
            }
        };
    }

    @Nullable
    private static String getPathByElement(@NotNull PsiElement element) {
        final PsiFile file = element.getContainingFile();
        if (file == null) {
            return null;
        }
        final VirtualFile vfile = file.getVirtualFile();
        if (vfile == null) {
            return null;
        }
        return ScriptFileUtil.getScriptFilePath(vfile);
    }

    @NotNull
    @Override
    public KdbProcessRunSettingsEditor getConfigurationEditor() {
        return new KdbProcessRunSettingsEditor(getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (getConfigurationModule().getModule() == null) {
            getConfigurationModule().checkForWarning();
        } else {
            final Sdk sdk = getSdk();
            if (sdk == null) {
                throw new RuntimeConfigurationWarning("No " + KdbSdkType.getInstance().getName() + " for the project or module " + getConfigurationModule().getModuleName());
            }
        }
        ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), getConfigurationModule().getModule());
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return (executor1, runner) -> {
            final GeneralCommandLine commandLine = createCommandLine();

            final OSProcessHandler processHandler = new KdbProcessHandler(commandLine);
            ProcessTerminatedListener.attach(processHandler);

            final ProcessBackedConsoleExecuteActionHandler executeActionHandler = new ProcessBackedConsoleExecuteActionHandler(processHandler, true);

            final LanguageConsoleBuilder b = new LanguageConsoleBuilder();
            b.processHandler(processHandler);
            b.initActions(executeActionHandler, "KdbConsoleHistory");
            b.gutterContentProvider(new LineNumberGutterProvider());

            final LanguageConsoleView console = b.build(environment.getProject(), QLanguage.INSTANCE);
            console.attachToProcess(processHandler);

            return new DefaultExecutionResult(console, processHandler, AnAction.EMPTY_ARRAY);
        };
    }

    private GeneralCommandLine createCommandLine() {
        final Sdk sdk = getSdk();
        if (sdk == null) {
            throw new IllegalStateException("KDB SDK is not specified");
        }

        final File executableFile = KdbSdkType.getInstance().getExecutableFile(sdk);

        final GeneralCommandLine commandLine = new GeneralCommandLine(executableFile.getAbsolutePath());
        commandLine.setWorkDirectory(getWorkingDirectory());

        if (getMainClassName() != null && !getMainClassName().isBlank()) {
            commandLine.addParameter(getMainClassName());
        }

        if (getProgramParameters() != null) {
            // If we don't split, parameters are quoted that is not correct for port, for example
            for (String s : getProgramParameters().split(" ")) {
                commandLine.addParameter(s);
            }
        }

        final Map<String, String> envs = new LinkedHashMap<>();
        envs.put("QHOME", sdk.getHomePath());
        envs.putAll(getEnvs());
        commandLine.withEnvironment(envs);
        return commandLine;
    }

    private Sdk getSdk() {
        return KdbSdkType.getModuleSdk(getConfigurationModule().getModule());
    }

    private static class KdbProcessHandler extends ColoredProcessHandler {
        public KdbProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
            super(commandLine);
        }

        @NotNull
        @Override
        protected BaseOutputReader.Options readerOptions() {
            return new BaseOutputReader.Options() {
                @Override
                public BaseDataReader.SleepingPolicy policy() {
                    return BaseOutputReader.Options.forMostlySilentProcess().policy();
                }

                @Override
                public boolean splitToLines() {
                    return false;
                }

                @Override
                public boolean withSeparators() {
                    return true;
                }
            };
        }
    }
}