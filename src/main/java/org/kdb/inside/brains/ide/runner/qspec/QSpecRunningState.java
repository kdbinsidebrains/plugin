package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.*;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.runner.KdbConsoleFilter;
import org.kdb.inside.brains.ide.runner.KdbRunningStateBase;
import org.kdb.inside.brains.lang.qspec.QSpecLibrary;
import org.kdb.inside.brains.lang.qspec.QSpecLibraryService;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.psi.QFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QSpecRunningState extends KdbRunningStateBase<QSpecRunConfiguration> {
    public static final String ROOT_SCRIPT_PATH = "/org/kdb/inside/brains/qspec.q";

    private static final String FRAMEWORK_NAME = "KDB QSpec Tests";
    private List<String> failedScripts = null;

    public QSpecRunningState(QSpecRunConfiguration cfg, Module module, ExecutionEnvironment environment) {
        super(cfg, module, environment);
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        try {
            final QSpecLibrary lib = QSpecLibraryService.getInstance().getValidLibrary();

            final KdbProcessHandler processHandler = startProcess();

            final TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(cfg.getProject());
            setConsoleBuilder(consoleBuilder);

            QSpecTestConsoleProperties consoleProperties = new QSpecTestConsoleProperties(cfg, FRAMEWORK_NAME, executor);

            ConsoleView consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole(FRAMEWORK_NAME, processHandler, consoleProperties);
            consoleView.addMessageFilter(new KdbConsoleFilter(cfg.getProject(), module, cfg.getWorkingDirectory()));

            DefaultExecutionResult executionResult = new DefaultExecutionResult(consoleView, processHandler);
            final AbstractRerunFailedTestsAction rerunFailedTestsAction = consoleProperties.createRerunFailedTestsAction(consoleView);
            if (rerunFailedTestsAction != null) {
                rerunFailedTestsAction.setModelProvider(((SMTRunnerConsoleView) consoleView)::getResultsViewer);
                executionResult.setRestartActions(rerunFailedTestsAction, new ToggleAutoTestAction());
            } else {
                executionResult.setRestartActions(new ToggleAutoTestAction());
            }

            processHandler.addProcessListener(new ProcessListener() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                    processIsReady(consoleView, processHandler, lib);
                }
            });

            return executionResult;
        } catch (RuntimeConfigurationException e) {
            throw new ExecutionException(e.getLocalizedMessage());
        }
    }

    private static String toKdbPath(Path path) {
        return quote(TestDescriptor.testPath(path));
    }

    private void initializeTests(ConsoleView consoleView, KdbProcessHandler processHandler) throws ExecutionException {
        consoleView.print("Initializing QSpec testing framework...\n", ConsoleViewContentType.LOG_INFO_OUTPUT);

        final String userScript = prepareScript(cfg.getActiveCustomScript());
        if (userScript != null) {
            consoleView.print("Loading custom script...\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            executeScript(processHandler, userScript);
        }

        final String appScript = prepareScript(getAppScript());
        if (appScript != null) {
            consoleView.print("Loading execution script...\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
            executeScript(processHandler, appScript);
        }
        consoleView.print("QSpec framework is ready.\n", ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    private static @NotNull String createPattern(String include) {
        return include == null || include.trim().isEmpty() ? "()" : quote(include);
    }

    private static String quote(String txt) {
        return '"' + txt.replace("\"", "\\\"") + '"';
    }

    private void processIsReady(ConsoleView consoleView, KdbProcessHandler processHandler, QSpecLibrary lib) {
        try {
            initializeTests(consoleView, processHandler);
            executeTests(consoleView, processHandler, lib);
        } catch (CantRunException ex) {
            consoleView.print(ex.getMessage(), ConsoleViewContentType.ERROR_OUTPUT);
            processHandler.killProcess();
        } catch (Exception ex) {
            consoleView.print(ExceptionUtils.getStackTrace(ex), ConsoleViewContentType.ERROR_OUTPUT);
            processHandler.killProcess();
        }
    }

    private void executeScript(KdbProcessHandler processHandler, String script) {
        final String oneLine = prepareScript(script);
        if (oneLine != null) {
            executeCommand(processHandler, oneLine);
        }
    }

    private void logAndExecuteCommand(ConsoleView consoleView, KdbProcessHandler processHandler, String command) {
        consoleView.print(command + "\n", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        executeCommand(processHandler, command);
    }

    private void executeCommand(KdbProcessHandler processHandler, String script) {
        final PrintStream out = new PrintStream(processHandler.getProcessInput());
        out.println(script);
        out.flush();
    }

    @Override
    protected GeneralCommandLine createCommandLine() {
        final GeneralCommandLine commandLine = super.createCommandLine();
        commandLine.addParameter("-q");
        return commandLine;
    }

    private static String toKdbBool(boolean val) {
        return val ? "1b" : "0b";
    }

    private static String toKdbDict(List<String> strings) {
        if (strings == null || strings.isEmpty()) {
            return "()";
        }
        if (strings.size() == 1) {
            return "enlist " + strings.get(0);
        }
        return "(" + String.join(";", strings) + ")";
    }

    private static @Nullable String prepareScript(String script) {
        if (script == null || script.isEmpty()) {
            return null;
        }

        // remove windows new line
        final String oneLine = Stream.of(
                        script.replace("\r", "").split("\n"))
                .map(String::trim)
                .filter(s -> !s.startsWith("/"))
                .collect(Collectors.joining(" ")
                ).trim();
        if (oneLine.isEmpty()) {
            return null;
        }
        return oneLine;
    }

    private void executeTests(ConsoleView consoleView, KdbProcessHandler processHandler, QSpecLibrary lib) throws IOException, ExecutionException {
        final Path root = Path.of(cfg.getScriptName());
        final Path spec = lib.getLocation();
        final List<String> params = failedScripts != null ? failedScripts : collectExecutionScripts(root);

        String args = String.join(";",
                toKdbPath(spec),
                toKdbPath(root),
                toKdbBool(cfg.isKeepFailed()),
                toKdbDict(params)
        );
        final String command = ".tst.app.runScript[" + args + "];";
        logAndExecuteCommand(consoleView, processHandler, command);
    }

    protected @NotNull List<String> collectExecutionScripts(Path root) throws IOException, ExecutionException {
        final List<String> scripts = collectScripts(root);
        final String expectation = createPattern(cfg.getTestPattern());
        final String specification = createPattern(cfg.getSuitePattern());
        final String filter = toKdbDict(List.of(specification, expectation));
        return scripts.stream().map(s -> "(" + s + "; enlist " + filter + ")").toList();
    }

    private List<String> collectScripts(Path script) throws IOException, ExecutionException {
        List<String> scripts;
        if (Files.isDirectory(script)) {
            final PsiManager instance = PsiManager.getInstance(cfg.getProject());
            final VirtualFileManager fileManager = VirtualFileManager.getInstance();

            try (Stream<Path> paths = Files.walk(script)) {
                scripts = paths.filter(Files::isRegularFile).filter(QFileType::is).filter(p -> {
                    try {
                        final VirtualFile vf = fileManager.findFileByNioPath(p);
                        if (vf != null) {
                            final PsiFile file = instance.findFile(vf);
                            if (file instanceof QFile qf) {
                                return TestDescriptor.hasTestCases(qf);
                            }
                        }
                    } catch (Exception ignore) {
                    }
                    return false;
                }).map(QSpecRunningState::toKdbPath).toList();
            }
        } else {
            scripts = List.of(toKdbPath(script));
        }

        if (scripts.isEmpty()) {
            throw new CantRunException("No QSpec files found in '" + script + "'");
        }
        return scripts;
    }

    private String getAppScript() throws ExecutionException {
        try {
            final URL resource = QSpecRunningState.class.getResource(ROOT_SCRIPT_PATH);
            if (resource == null) {
                throw new ExecutionException("QSpec App Script not found: " + ROOT_SCRIPT_PATH);
            }

            try (final InputStream in = resource.openStream()) {
                if (in == null) {
                    throw new ExecutionException("Resource can't be loaded: " + ROOT_SCRIPT_PATH);
                }
                return StreamUtil.readText(new InputStreamReader(in));
            }
        } catch (IOException ex) {
            throw new ExecutionException("Resource can't be loaded: " + ROOT_SCRIPT_PATH, ex);
        }
    }

    public @Nullable QSpecRunningState withFailedTests(List<AbstractTestProxy> failedTests) {
        final List<String> scripts = new ArrayList<>();
        final Map<String, List<String>> filters = new HashMap<>();

        for (AbstractTestProxy test : failedTests) {
            final String locationUrl = test.getLocationUrl();
            if (locationUrl == null) {
                continue;
            }
            if (!locationUrl.startsWith(TestDescriptor.TEST_URI_SPEC)) {
                continue;
            }

            int i = locationUrl.indexOf("?[");
            if (i < 0) {
                continue;
            }
            int j = locationUrl.indexOf("]/[", i + 2);
            if (j < 0) {
                continue;
            }
            final int length = locationUrl.length() - 1;
            if (locationUrl.charAt(length) != ']') {
                continue;
            }
            final String script = quote(locationUrl.substring(TestDescriptor.TEST_URI_SPEC.length(), i));
            final String spec = quote(locationUrl.substring(i + 2, j));
            final String expect = quote(locationUrl.substring(j + 3, length));

            final List<String> strings = filters.computeIfAbsent(script, s -> {
                scripts.add(s);
                return new ArrayList<>();
            });

            final String kdbDict = toKdbDict(List.of(spec, expect));
            if (!strings.contains(kdbDict)) {
                strings.add(kdbDict);
            }
        }

        failedScripts = scripts.stream().map(s -> "(" + s + ";" + toKdbDict(filters.get(s)) + ")").toList();
        return this;
    }
}