package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.Filter;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class QSpecTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {
    public QSpecTestConsoleProperties(@NotNull QSpecRunConfiguration config, String frameworkName, @NotNull Executor executor) {
        super(config, frameworkName, executor);
    }

    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties consoleProperties) {
        // We don't need that. Redefined for testing only.
        return new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties) {
            public void process(final String text, final Key outputType) {
                super.process(text, outputType);
            }
        };
    }

    @Override
    public @Nullable SMTestLocator getTestLocator() {
        return QSpecTestLocator.INSTANCE;
    }

    @Override
    public @Nullable AbstractRerunFailedTestsAction createRerunFailedTestsAction(ConsoleView consoleView) {
//        final AnAction action = ActionManager.getInstance().getAction("RerunFailedTests");
//        return action == null ? null :
        return new QSpecRerunFailedTestsAction(this, consoleView);
    }

    @SuppressWarnings("RawUseOfParameterized")
    private static class QSpecRerunFailedTestsAction extends AbstractRerunFailedTestsAction {
        protected QSpecRerunFailedTestsAction(QSpecTestConsoleProperties properties, @NotNull ComponentContainer view) {
            super(view);
            init(properties);
        }

        @Override
        protected @NotNull Filter getFilter(@NotNull Project project, @NotNull GlobalSearchScope searchScope) {
            return super.getFilter(project, searchScope).and(new Filter() {
                @Override
                public boolean shouldAccept(AbstractTestProxy test) {
                    return test.isLeaf();
                }
            });
        }

        @Nullable
        @Override
        protected MyRunProfile getRunProfile(@NotNull ExecutionEnvironment environment) {
            final RunProfile configuration = myConsoleProperties.getConfiguration();
            return new MyRunProfile((RunConfigurationBase<?>) configuration) {
                @Nullable
                @Override
                public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
                    final RunConfigurationBase<?> peer = getPeer();
                    if (!(peer instanceof QSpecRunConfiguration conf)) {
                        return null;
                    }

                    final List<AbstractTestProxy> failedTests = getFailedTests(conf.getProject());
                    if (failedTests.isEmpty()) {
                        return null;
                    }

                    return conf.createRunningState(environment).withFailedTests(failedTests);
                }
            };
        }
    }
}
