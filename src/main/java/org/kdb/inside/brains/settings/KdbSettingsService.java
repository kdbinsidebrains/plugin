package org.kdb.inside.brains.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.ExecutionOptions;
import org.kdb.inside.brains.core.InstanceOptions;
import org.kdb.inside.brains.view.chart.ChartOptions;
import org.kdb.inside.brains.view.console.ConsoleOptions;
import org.kdb.inside.brains.view.console.NumericalOptions;
import org.kdb.inside.brains.view.console.TableOptions;
import org.kdb.inside.brains.view.inspector.InspectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@State(name = "KdbSettings", storages = {@Storage(value = "kdb-settings.xml")})
public class KdbSettingsService implements PersistentStateComponent<KdbSettingsService.State> {
    private final State myState = new State();

    private final List<KdbSettingsListener> listeners = new CopyOnWriteArrayList<>();

    private static KdbSettingsService instance;

    public KdbSettingsService() {
    }

    public void addSettingsListener(KdbSettingsListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeSettingsListener(KdbSettingsListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public ConsoleOptions getConsoleOptions() {
        return myState.consoleOptions;
    }

    public void setConsoleOptions(ConsoleOptions options) {
        if (!myState.consoleOptions.equals(options)) {
            myState.consoleOptions.copyFrom(options);
            notifySettingsChanged(myState.consoleOptions);
        }
    }

    public TableOptions getTableOptions() {
        return myState.tableOptions;
    }

    public void setTableOptions(TableOptions options) {
        if (!myState.tableOptions.equals(options)) {
            myState.tableOptions.copyFrom(options);
            notifySettingsChanged(myState.tableOptions);
        }
    }

    public InstanceOptions getInstanceOptions() {
        return myState.instanceOptions;
    }

    public void setInstanceOptions(InstanceOptions options) {
        if (!myState.instanceOptions.equals(options)) {
            myState.instanceOptions.copyFrom(options);
            notifySettingsChanged(myState.instanceOptions);
        }
    }

    public ExecutionOptions getExecutionOptions() {
        return myState.executionOptions;
    }

    public void setExecutionOptions(ExecutionOptions options) {
        if (!myState.executionOptions.equals(options)) {
            myState.executionOptions.copyFrom(options);
            notifySettingsChanged(myState.executionOptions);
        }
    }

    public InspectorOptions getInspectorOptions() {
        return myState.inspectorOptions;
    }

    public void setInspectorOptions(InspectorOptions options) {
        if (!myState.inspectorOptions.equals(options)) {
            myState.inspectorOptions.copyFrom(options);
            notifySettingsChanged(myState.inspectorOptions);
        }
    }

    public ChartOptions getChartOptions() {
        return myState.chartOptions;
    }

    public void setChartOptions(ChartOptions options) {
        if (!myState.chartOptions.equals(options)) {
            myState.chartOptions.copyFrom(options);
            notifySettingsChanged(myState.chartOptions);
        }
    }

    public NumericalOptions getNumericalOptions() {
        return myState.numericalOptions;
    }

    public void setNumericalOptions(NumericalOptions options) {
        if (!myState.numericalOptions.equals(options)) {
            myState.numericalOptions.copyFrom(options);
            notifySettingsChanged(myState.numericalOptions);
        }
    }

    private void notifySettingsChanged(SettingsBean<?> bean) {
        listeners.forEach(l -> l.settingsChanged(this, bean));
    }

    @Nullable
    @Override
    public KdbSettingsService.State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState.setChartOptions(state.chartOptions);
        myState.setTableOptions(state.tableOptions);
        myState.setConsoleOptions(state.consoleOptions);
        myState.setEditorOptions(state.executionOptions);
        myState.setInstanceOptions(state.instanceOptions);
        myState.setCredentialPlugins(state.credentialPlugins);
        myState.setInspectorOptions(state.inspectorOptions);
        myState.setNumericalOptions(state.numericalOptions);
    }

    public static KdbSettingsService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(KdbSettingsService.class);
        }
        return instance;
    }

    public static class State {
        private final List<String> credentialPlugins = new ArrayList<>();
        private final ChartOptions chartOptions = new ChartOptions();
        private final TableOptions tableOptions = new TableOptions();
        private final ConsoleOptions consoleOptions = new ConsoleOptions();
        private final InstanceOptions instanceOptions = InstanceOptions.defaultOptions();
        private final ExecutionOptions executionOptions = new ExecutionOptions();
        private final InspectorOptions inspectorOptions = new InspectorOptions();
        private final NumericalOptions numericalOptions = new NumericalOptions();

        public List<String> getCredentialPlugins() {
            return credentialPlugins;
        }

        public void setCredentialPlugins(List<String> credentialPlugins) {
            this.credentialPlugins.clear();
            if (credentialPlugins != null) {
                this.credentialPlugins.addAll(credentialPlugins);
            }
        }

        public TableOptions getTableOptions() {
            return tableOptions;
        }

        public void setTableOptions(TableOptions tableOptions) {
            this.tableOptions.copyFrom(tableOptions);
        }

        public ConsoleOptions getConsoleOptions() {
            return consoleOptions;
        }

        public void setConsoleOptions(ConsoleOptions consoleOptions) {
            this.consoleOptions.copyFrom(consoleOptions);
        }

        public InstanceOptions getInstanceOptions() {
            return instanceOptions;
        }

        public void setInstanceOptions(InstanceOptions instanceOptions) {
            this.instanceOptions.copyFrom(instanceOptions);
        }

        public ExecutionOptions getEditorOptions() {
            return executionOptions;
        }

        public void setEditorOptions(ExecutionOptions executionOptions) {
            this.executionOptions.copyFrom(executionOptions);
        }

        public InspectorOptions getInspectorOptions() {
            return inspectorOptions;
        }

        public void setInspectorOptions(InspectorOptions inspectorOptions) {
            this.inspectorOptions.copyFrom(inspectorOptions);
        }

        public ChartOptions getChartOptions() {
            return chartOptions;
        }

        public void setChartOptions(ChartOptions options) {
            chartOptions.copyFrom(options);
        }

        public NumericalOptions getNumericalOptions() {
            return numericalOptions;
        }

        public void setNumericalOptions(NumericalOptions options) {
            numericalOptions.copyFrom(options);
        }
    }
}
