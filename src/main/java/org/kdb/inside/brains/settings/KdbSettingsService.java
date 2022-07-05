package org.kdb.inside.brains.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.ExecutionOptions;
import org.kdb.inside.brains.core.InstanceOptions;
import org.kdb.inside.brains.view.console.ConsoleOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@com.intellij.openapi.components.State(name = "KdbSettings", storages = {@Storage("kdb-settings.xml")})
public class KdbSettingsService implements PersistentStateComponent<KdbSettingsService.State> {
    private final State myState = new State();

    private final List<KdbSettingsListener> listeners = new CopyOnWriteArrayList<>();

    private static KdbSettingsService instance;
    private static final String DEFAULT_CREDENTIALS = "${user.name}";
    private static final String CREDENTIAL_ATTRIBUTE = "KdbInsideBrainsGlobalCredentials";

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

    public String getDefaultCredentials() {
        final String res = PasswordSafe.getInstance().getPassword(new CredentialAttributes(CREDENTIAL_ATTRIBUTE));
        return res == null ? DEFAULT_CREDENTIALS : res;
    }

    public void setDefaultCredentials(String credentials) {
        Objects.requireNonNull(credentials, "Default credentials can't be null");

        if (!credentials.equals(getDefaultCredentials())) {
            PasswordSafe.getInstance().setPassword(new CredentialAttributes(CREDENTIAL_ATTRIBUTE), credentials);
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

    public InstanceOptions getInstanceOptions() {
        return myState.instanceOptions;
    }

    public void setInstanceOptions(InstanceOptions options) {
        if (!myState.instanceOptions.equals(options)) {
            myState.instanceOptions.copyFrom(options);
            notifySettingsChanged(myState.instanceOptions);
        }
    }

    public ExecutionOptions getConnectionOptions() {
        return myState.executionOptions;
    }

    public void setConnectionOptions(ExecutionOptions options) {
        if (!myState.executionOptions.equals(options)) {
            myState.executionOptions.copyFrom(options);
            notifySettingsChanged(myState.executionOptions);
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
        myState.setConsoleOptions(state.consoleOptions);
        myState.setEditorOptions(state.executionOptions);
        myState.setInstanceOptions(state.instanceOptions);
        myState.setCredentialPlugins(state.credentialPlugins);
    }

    public static KdbSettingsService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(KdbSettingsService.class);
        }
        return instance;
    }

    static class State {
        private final List<String> credentialPlugins = new ArrayList<>();
        private final ConsoleOptions consoleOptions = new ConsoleOptions();
        private final InstanceOptions instanceOptions = new InstanceOptions();
        private final ExecutionOptions executionOptions = new ExecutionOptions();

        public List<String> getCredentialPlugins() {
            return credentialPlugins;
        }

        public void setCredentialPlugins(List<String> credentialPlugins) {
            this.credentialPlugins.clear();
            if (credentialPlugins != null) {
                this.credentialPlugins.addAll(credentialPlugins);
            }
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
    }
}
