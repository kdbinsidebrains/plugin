package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.StreamUtil;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QFileType;
import org.kdb.inside.brains.ide.qspec.QSpecLibrary;
import org.kdb.inside.brains.ide.runner.KdbConsoleFilter;
import org.kdb.inside.brains.ide.runner.KdbRunningStateBase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QSpecRunningState extends KdbRunningStateBase<QSpecRunConfiguration> {
    public static final String ROOT_SCRIPT_PATH = "/org/kdb/inside/brains/qspec.q";
    private static final String FRAMEWORK_NAME = "KDB QSpec Tests";

    protected QSpecRunningState(QSpecRunConfiguration cfg, Module module, ExecutionEnvironment environment) {
        super(cfg, module, environment);
    }

    private static @NotNull String createPattern(String include) {
        if (include == null || include.trim().isEmpty()) {
            return "()";
        }
        final String[] split = include.split(",");
        if (split.length == 1) {
            return "enlist " + quote(split[0].trim());
        } else {
            return "(" + Stream.of(split).map(String::trim).map(QSpecRunningState::quote).collect(Collectors.joining(";")) + ")";
        }
    }

    private static String quote(String txt) {
        return '"' + txt.replace("\"", "\\\"") + '"';
    }

    private static String kdbPathArgument(Path path) {
        return quote(FilenameUtils.normalize(path.toString(), true));
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

    @Override
    public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        final QSpecLibrary lib = cfg.getActiveLibrary();
        if (lib == null) {
            throw new ExecutionException("QSpec library is not defined");
        }

        try {
            lib.validate();
        } catch (Exception ex) {
            throw new ExecutionException("QSpec library is not valid: " + ex.getMessage(), ex);
        }

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
    }

    private void processIsReady(ConsoleView consoleView, KdbProcessHandler processHandler, QSpecLibrary lib) {
        try {
            initializeTests(consoleView, processHandler, lib);

            executeTests(consoleView, processHandler, lib.specFolder());
        } catch (Exception ex) {
            consoleView.print(ExceptionUtils.getStackTrace(ex), ConsoleViewContentType.ERROR_OUTPUT);
            processHandler.killProcess();
        }
    }

    private void initializeTests(ConsoleView consoleView, KdbProcessHandler processHandler, QSpecLibrary lib) throws ExecutionException {
        consoleView.print("Initializing QSpec testing framework...\n", ConsoleViewContentType.LOG_INFO_OUTPUT);

        final String userScript = prepareScript(lib.script());
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

    private void executeTests(ConsoleView consoleView, KdbProcessHandler processHandler, String qSpecDir) throws IOException, ExecutionException {
        final Path root = Path.of(cfg.getScriptName());
        final Path workingDir = Path.of(cfg.getWorkingDirectory());

        final String scripts = collectScripts(workingDir, root);

        final String expectations = createPattern(cfg.getExpectationPattern());
        final String specifications = createPattern(cfg.getSpecificationPattern());

        final String rootScript = kdbPathArgument(root.toAbsolutePath());
//        final String rootScript = kdbPathArgument(workingDir.relativize(root));

        String args = String.join(";",
                rootScript,
                kdbPathArgument(Path.of(qSpecDir)),
                scripts,
                specifications,
                expectations
        );
        final String command = ".tst.app.runScript[" + args + "]";
        logAndExecuteCommand(consoleView, processHandler, command);
    }

    private String collectScripts(Path workingDir, Path script) throws IOException, ExecutionException {
        if (Files.isDirectory(script)) {
            try (Stream<Path> paths = Files.walk(script)) {
                final List<String> files = paths.filter(Files::isRegularFile).filter(QFileType::is).map(Path::toAbsolutePath).map(QSpecRunningState::kdbPathArgument).toList();
//                final List<String> files = paths.filter(Files::isRegularFile).filter(QFileType::is).map(workingDir::relativize).map(QSpecRunningState::kdbPathArgument).toList();
                if (files.isEmpty()) {
                    throw new ExecutionException("Not Q files found in " + script);
                }
                if (files.size() == 1) {
                    return "enlist " + files.get(0);
                }
                return "(" + String.join(";", files) + ")";
            }
        } else {
//            return "enlist " + kdbPathArgument(workingDir.relativize(script));
            return "enlist " + kdbPathArgument(script.toAbsolutePath());
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

    private String getAppScript() throws ExecutionException {
        try {
//            try (final InputStream in = QSpecRunningState.class.getResourceAsStream(ROOT_SCRIPT_PATH)) {
            try (final InputStream in = new FileInputStream(new File("C:\\Users\\smkli\\IdeaProjects\\kdbinsidebrains\\plugin\\src\\main\\resources\\org\\kdb\\inside\\brains\\qspec.q"))) {
                if (in == null) {
                    throw new ExecutionException("Resource can't be loaded: " + ROOT_SCRIPT_PATH);
                }
                return StreamUtil.readText(new InputStreamReader(in));
            }
        } catch (IOException ex) {
            throw new ExecutionException("Resource can't be loaded: " + ROOT_SCRIPT_PATH, ex);
        }
    }
}
