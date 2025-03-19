package org.kdb.inside.brains.ide.runner.instance;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.console.LanguageConsoleBuilder;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.execution.console.ProcessBackedConsoleExecuteActionHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnAction;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.ide.runner.KdbRunningStateBase;
import org.kdb.inside.brains.view.LineNumberGutterProvider;

public class KdbInstanceRunningState extends KdbRunningStateBase<KdbInstanceRunConfiguration> {
    protected KdbInstanceRunningState(KdbInstanceRunConfiguration cfg, ExecutionEnvironment environment) {
        super(cfg, environment);
    }

    @Override
    protected GeneralCommandLine createCommandLine() throws ExecutionException {
        final GeneralCommandLine commandLine = super.createCommandLine();

        final String scriptName = cfg.getScriptName();
        if (Strings.isNotEmpty(scriptName)) {
            commandLine.addParameter(scriptName);

            final String params = cfg.getScriptArguments();
            if (Strings.isNotEmpty(params)) {
                commandLine.addParameters(params.split(" "));
            }
        }

        return commandLine;
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        final KdbProcessHandler processHandler = startProcess();

        final ProcessBackedConsoleExecuteActionHandler executeActionHandler = new ProcessBackedConsoleExecuteActionHandler(processHandler, true);

        final LanguageConsoleBuilder b = new LanguageConsoleBuilder();
        b.processHandler(processHandler);
        b.initActions(executeActionHandler, "KdbConsoleHistory");
        b.gutterContentProvider(new LineNumberGutterProvider());

        final LanguageConsoleView console = b.build(getEnvironment().getProject(), QLanguage.INSTANCE);
        console.attachToProcess(processHandler);

        return new DefaultExecutionResult(console, processHandler, AnAction.EMPTY_ARRAY);
    }
}
