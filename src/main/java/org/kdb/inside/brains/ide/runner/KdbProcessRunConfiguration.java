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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
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
                throw new RuntimeConfigurationWarning("No KDB SDK for the project or module " + getConfigurationModule().getModuleName());
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
            b.gutterContentProvider(new KdbGutterProvider());

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
        final Module module = getConfigurationModule().getModule();
        if (module != null) {
            final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
            if (sdk != null && sdk.getSdkType().equals(KdbSdkType.getInstance())) {
                return sdk;
            }
        }
        return null;
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


/*
    public KdbProcessRunConfiguration(final Project project, final ConfigurationFactory factory, final String name) {
        super(project, factory, name);
        workDir = PathUtil.getLocalPath(project.getBasePath());
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return null;
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        scriptPath = ExternalizablePath.localPathValue(JDOMExternalizer.readString(element, "path"));
        scriptParams = JDOMExternalizer.readString(element, "params");
        final String wrk = JDOMExternalizer.readString(element, "workDir");
        if (!".".equals(wrk)) {
            workDir = ExternalizablePath.localPathValue(wrk);
        }
        envs.clear();
        JDOMExternalizer.readMap(element, envs, null, "env");
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        JDOMExternalizer.write(element, "path", ExternalizablePath.urlValue(scriptPath));
        JDOMExternalizer.write(element, "params", scriptParams);
        JDOMExternalizer.write(element, "workDir", ExternalizablePath.urlValue(workDir));
        JDOMExternalizer.writeMap(element, envs, null, "env");
    }

    @Override
    public void setProgramParameters(@Nullable String value) {
        scriptParams = value;
    }

    @Override
    public String getProgramParameters() {
        return scriptParams;
    }

    @Override
    public void setWorkingDirectory(@Nullable String value) {
        workDir = value;
    }

    @Override
    public String getWorkingDirectory() {
        return workDir;
    }

    @Override
    public void setEnvs(@NotNull Map<String, String> envs) {
        this.envs.clear();
        this.envs.putAll(envs);
    }

    @NotNull
    @Override
    public Map<String, String> getEnvs() {
        return envs;
    }

    @Override
    public void setPassParentEnvs(boolean passParentEnvs) {
        this.passParentEnv = passParentEnvs;
    }

    @Override
    public boolean isPassParentEnvs() {
        return passParentEnv;
    }


    @Nullable
    @Override
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        // TODO: not implemented
*/
/*
        if (scriptPath == null || !scriptPath.equals(getPathByElement(element))) {
            return null;
        }

        final PsiClass classToRun = GroovyRunnerPsiUtil.getRunningClass(element);

        if (element instanceof GroovyFile) {
            return new RefactoringElementAdapter() {
                @Override
                protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                    if (newElement instanceof GroovyFile) {
                        GroovyFile file = (GroovyFile) newElement;
                        setScriptPath(ScriptFileUtil.getScriptFilePath(file.getVirtualFile()));
                    }
                }

                @Override
                public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                    elementRenamedOrMoved(newElement);
                }
            };
        } else if (element instanceof PsiClass && element.getManager().areElementsEquivalent(element, classToRun)) {
            return new RefactoringElementAdapter() {
                @Override
                protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                    setName(((PsiClass) newElement).getName());
                }

                @Override
                public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                    elementRenamedOrMoved(newElement);
                }
            };
        }
*//*

        return null;
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return (executor1, runner) -> {
            final GeneralCommandLine commandLine = new GeneralCommandLine(getOptions().getScriptName());
//                commandLine.addParameter("-p 1000");

            final BasicGutterContentProvider gutterContentProvider = new KdbGutterProvider();

            final OSProcessHandler processHandler = new KdbProcessHandler(commandLine);
            ProcessTerminatedListener.attach(processHandler);

            final ProcessBackedConsoleExecuteActionHandler executeActionHandler = new ProcessBackedConsoleExecuteActionHandler(processHandler, true);

            final LanguageConsoleBuilder b = new LanguageConsoleBuilder();
            b.processHandler(processHandler);
            b.initActions(executeActionHandler, "KdbConsoleHistory");
            b.gutterContentProvider(gutterContentProvider);

            final LanguageConsoleView console = b.build(environment.getProject(), QLanguage.INSTANCE);
            console.attachToProcess(processHandler);

            return new DefaultExecutionResult(console, processHandler, AnAction.EMPTY_ARRAY);
        };


*/
/*        final VirtualFile scriptFile = ScriptFileUtil.findScriptFileByPath(getScriptPath());
        if (scriptFile == null) return null;

        final GroovyScriptRunner scriptRunner = getScriptRunner();
        if (scriptRunner == null) return null;*//*
 */
/*

        return new JavaCommandLineState(environment) {
            @NotNull
            @Override
            protected OSProcessHandler startProcess() throws ExecutionException {
                final OSProcessHandler handler = super.startProcess();
                handler.setShouldDestroyProcessRecursively(true);
                if (scriptRunner.shouldRefreshAfterFinish()) {
                    handler.addProcessListener(new ProcessAdapter() {
                        @Override
                        public void processTerminated(@NotNull ProcessEvent event) {
                            if (!ApplicationManager.getApplication().isDisposed()) {
                                VirtualFileManager.getInstance().asyncRefresh(null);
                            }
                        }
                    });
                }

                return handler;
            }

            @Override
            protected JavaParameters createJavaParameters() throws ExecutionException {
                final Module module = getModule();
                final boolean tests = ProjectRootManager.getInstance(getProject()).getFileIndex().isInTestSourceContent(scriptFile);
                String jrePath = isAlternativeJrePathEnabled() ? getAlternativeJrePath() : null;
                JavaParameters params = new JavaParameters();
                params.setUseClasspathJar(true);
                params.setDefaultCharset(getProject());
                params.setJdk(
                        module == null ? JavaParametersUtil.createProjectJdk(getProject(), jrePath)
                                : JavaParametersUtil.createModuleJdk(module, !tests, jrePath)
                );
                configureConfiguration(params, new CommonProgramRunConfigurationParametersDelegate(GroovyScriptRunConfiguration.this) {
                    @Nullable
                    @Override
                    public String getProgramParameters() {
                        return null;
                    }
                });
                scriptRunner.configureCommandLine(params, module, tests, scriptFile, GroovyScriptRunConfiguration.this);

                return params;
            }*//*

    }
}


*/
/*    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {

    }

    @Nullable
    private static String getPathByElement(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        if (file == null) return null;
        VirtualFile vfile = file.getVirtualFile();
        if (vfile == null) return null;
        return vfile.getPath();
    }

    public static JavaParameters createJavaParametersWithSdk(@Nullable Module module) {
        JavaParameters params = new JavaParameters();
        params.setCharset(null);

        if (module != null) {
            final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
            if (sdk != null && sdk.getSdkType() instanceof JavaSdkType) {
                params.setJdk(sdk);
            }
        }
        if (params.getJdk() == null) {
            params.setJdk(new SimpleJavaSdkType().createJdk("tmp", SystemProperties.getJavaHome()));
        }
        return params;
    }

    @Override
    @NotNull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new GroovyRunConfigurationEditor(getProject());
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        super.checkConfiguration();

        final String scriptPath = getScriptPath();

        final VirtualFile script = ScriptFileUtil.findScriptFileByPath(scriptPath);
        if (script == null) throw new RuntimeConfigurationException("Cannot find script " + scriptPath);

        final GroovyScriptRunner scriptRunner = getScriptRunner();
        if (scriptRunner == null) throw new RuntimeConfigurationException("Unknown script type " + scriptPath);

        scriptRunner.ensureRunnerConfigured(this);

        final PsiFile file = PsiManager.getInstance(getProject()).findFile(script);
        final PsiClass toRun = GroovyRunnerPsiUtil.getRunningClass(file);
        if (toRun == null) {
            throw new RuntimeConfigurationWarning(GroovyBundle.message("class.does.not.exist"));
        }
        if (toRun instanceof GrTypeDefinition) {
            if (!GroovyRunnerPsiUtil.canBeRunByGroovy(toRun)) {
                throw new RuntimeConfigurationWarning(GroovyBundle.message("class.cannot.be.executed"));
            }
        } else {
            throw new RuntimeConfigurationWarning(GroovyBundle.message("script.file.is.not.groovy.file"));
        }
        JavaParametersUtil.checkAlternativeJRE(this);
    }

    @Override
    public void setVMParameters(@Nullable String value) {
        vmParams = value;
    }

    @Override
    public String getVMParameters() {
        return vmParams;
    }

    @Override
    public boolean isAlternativeJrePathEnabled() {
        return myAlternativeJrePathEnabled;
    }

    @Override
    public void setAlternativeJrePathEnabled(boolean alternativeJrePathEnabled) {
        myAlternativeJrePathEnabled = alternativeJrePathEnabled;
    }

    @Nullable
    @Override
    public String getAlternativeJrePath() {
        return myAlternativeJrePath;
    }

    @Override
    public void setAlternativeJrePath(@Nullable String alternativeJrePath) {
        myAlternativeJrePath = alternativeJrePath;
    }

    @Override
    public String getRunClass() {
        return null;
    }

    @Override
    public String getPackage() {
        return null;
    }



    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        isDebugEnabled = debugEnabled;
    }

    public boolean isAddClasspathToTheRunner() {
        return isAddClasspathToTheRunner;
    }

    public void setAddClasspathToTheRunner(boolean addClasspathToTheRunner) {
        isAddClasspathToTheRunner = addClasspathToTheRunner;
    }

    @Nullable
    public String getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(@Nullable String scriptPath) {
        this.scriptPath = scriptPath;
    }

    @Override
    public GlobalSearchScope getSearchScope() {
        GlobalSearchScope superScope = super.getSearchScope();

        String path = getScriptPath();
        if (path == null) return superScope;

        VirtualFile scriptFile = LocalFileSystem.getInstance().findFileByPath(path);
        if (scriptFile == null) return superScope;

        GlobalSearchScope fileScope = GlobalSearchScope.fileScope(getProject(), scriptFile);
        if (superScope == null) return fileScope;

        return new DelegatingGlobalSearchScope(fileScope.union(superScope)) {
            @Override
            public int compare(@NotNull VirtualFile file1, @NotNull VirtualFile file2) {
                if (file1.equals(scriptFile)) return 1;
                if (file2.equals(scriptFile)) return -1;
                return super.compare(file1, file2);
            }
        };
    }
    */

