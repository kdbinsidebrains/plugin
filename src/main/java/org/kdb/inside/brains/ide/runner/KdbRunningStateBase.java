package org.kdb.inside.brains.ide.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.util.io.BaseDataReader;
import com.intellij.util.io.BaseOutputReader;
import com.jgoodies.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import java.io.File;

public abstract class KdbRunningStateBase<C extends KdbRunConfigurationBase> extends CommandLineState {
    protected final C cfg;

    protected KdbRunningStateBase(C cfg, ExecutionEnvironment environment) {
        super(environment);
        this.cfg = cfg;
    }

    @Override
    protected @NotNull KdbProcessHandler startProcess() throws ExecutionException {
        final GeneralCommandLine commandLine = createCommandLine();
        final KdbProcessHandler handler = new KdbProcessHandler(commandLine);
        ProcessTerminatedListener.attach(handler);
        return handler;
    }

    protected GeneralCommandLine createCommandLine() throws ExecutionException {
        final Module module = cfg.getExecutionModule();
        if (module == null) {
            throw new ExecutionException("Execution module is not specified");
        }

        final Sdk sdk = KdbSdkType.getModuleSdk(module);
        if (sdk == null) {
            throw new ExecutionException("KDB SDK is not specified");
        }

        final File executableFile = KdbSdkType.getInstance().getExecutableFile(sdk);
        if (executableFile == null) {
            throw new ExecutionException("KDB SDK is not correct");
        }

        final GeneralCommandLine commandLine = new GeneralCommandLine(executableFile.getAbsolutePath());

        final String args = cfg.getKdbOptions();
        if (Strings.isNotEmpty(args)) {
            commandLine.withParameters(args.split(" "));
        }

        commandLine.setWorkDirectory(cfg.getWorkingDirectory());

        final String homePath = sdk.getHomePath();
        if (homePath != null) {
            commandLine.withEnvironment("QHOME", homePath);
        }
        commandLine.withEnvironment(cfg.getEnvs());

        return commandLine;
    }

    protected static class KdbProcessHandler extends ColoredProcessHandler {
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
