package org.kdb.inside.brains.core;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Progressive;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import kx.KxConnection;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.toolbar.InstancesComboAction;
import org.kdb.inside.brains.core.credentials.CredentialService;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.treeview.forms.InstanceEditorDialog;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.kdb.inside.brains.core.InstanceState.CONNECTED;
import static org.kdb.inside.brains.core.InstanceState.DISCONNECTED;

public class KdbConnectionManager implements Disposable, DumbAware {
    private InstanceConnection activeConnection;

    private final Project project;
    private final Map<KdbInstance, TheInstanceConnection> connections = new HashMap<>();

    private final KdbQueryLogger queryLogger;
    private final List<KdbQueryListener> queryListeners = new CopyOnWriteArrayList<>();
    private final List<KdbConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();

    private static final int PROGRESS_TICK_MILLIS = 100;

    private final ScheduledExecutorService connectionProgressExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "KdbConnectionManager-ProgressUpdater"));

    public KdbConnectionManager(Project project) {
        this.project = project;
        this.queryLogger = project.getService(KdbQueryLogger.class);
    }

    public void addQueryListener(KdbQueryListener listener) {
        if (listener != null) {
            queryListeners.add(listener);
        }
    }

    public void removeQueryListener(KdbQueryListener listener) {
        if (listener != null) {
            queryListeners.remove(listener);
        }
    }

    public void addConnectionListener(KdbConnectionListener listener) {
        if (listener != null) {
            connectionListeners.add(listener);
        }
    }

    public void removeConnectionListener(KdbConnectionListener listener) {
        if (listener != null) {
            connectionListeners.remove(listener);
        }
    }

    public void activate(KdbInstance instance) {
        InstanceConnection old = activeConnection;
        this.activeConnection = register(instance);
        processConnectionActivated(old, activeConnection);
    }

    @NotNull
    public InstanceState test(@NotNull KdbInstance instance) throws Exception {
        return new TheInstanceConnection(instance).test();
    }

    @NotNull
    public InstanceConnection register(@NotNull KdbInstance instance) {
        TheInstanceConnection c = connections.get(instance);
        if (c == null) {
            c = new TheInstanceConnection(instance);
            connections.put(instance, c);
            processConnectionCreated(c);
        }
        c.connect();
        return c;
    }

    @Nullable
    public InstanceConnection unregister(@NotNull KdbInstance instance) {
        final TheInstanceConnection conn = connections.remove(instance);
        if (conn != null) {
            conn.disconnect();
            processConnectionRemoved(conn);
        }
        return conn;
    }

    @Nullable
    public InstanceConnection getConnection(@NotNull KdbInstance instance) {
        return connections.get(instance);
    }

    public InstanceState getInstanceState(KdbInstance instance) {
        final TheInstanceConnection conn = connections.get(instance);
        return conn == null ? null : conn.getState();
    }

    @NotNull
    public List<InstanceConnection> getConnections() {
        return new ArrayList<>(connections.values());
    }

    @Nullable
    public InstanceConnection getActiveConnection() {
        return activeConnection;
    }

    public KdbInstance createTempInstance(KdbInstance instance, KdbScope scope) {
        return new TemporalKdbInstance(instance, scope);
    }

    public boolean isTempInstance(KdbInstance instance) {
        return instance instanceof TemporalKdbInstance;
    }

    @Override
    public void dispose() {
        connectionListeners.clear();

        connections.values().forEach(TheInstanceConnection::disconnect);
        connections.clear();
    }

    private void processConnectionCreated(TheInstanceConnection c) {
        connectionListeners.forEach(l -> l.connectionCreated(c));
    }

    protected void processConnectionRemoved(InstanceConnection c) {
        connectionListeners.forEach(l -> l.connectionRemoved(c));
    }

    private void processConnectionState(InstanceConnection conn, InstanceState oldState, InstanceState state) {
        UIUtil.invokeAndWaitIfNeeded(() -> {
                    connectionListeners.forEach(l -> l.connectionStateChanged(conn, oldState, state));

                    final String name = conn.getName();
                    if (state == CONNECTED) {
                        createNotification("Instance has been connected: " + name, NotificationType.INFORMATION).notify(project);
                    } else if (state == DISCONNECTED) {
                        final Exception error = conn.getDisconnectError();
                        if (error == null) {
                            createNotification("Instance has been disconnected: " + name, NotificationType.INFORMATION).notify(project);
                        } else {
                            createNotification("Instance can't be connected: " + name + " - " + error.getMessage(), NotificationType.WARNING)
                                    .addAction(new DumbAwareAction("Check Instance Details") {
                                        @Override
                                        public void actionPerformed(@NotNull AnActionEvent e) {
                                            final KdbInstance instance = conn.getInstance();
                                            final InstanceEditorDialog editor = new InstanceEditorDialog(InstanceEditorDialog.Mode.UPDATE, project, instance);
                                            if (editor.showAndGet()) {
                                                instance.updateFrom(editor.createInstance());
                                            }
                                        }
                                    })
                                    .addAction(new DumbAwareAction("Reconnect Instance") {
                                        @Override
                                        public void actionPerformed(@NotNull AnActionEvent e) {
                                            conn.connectAndWait();
                                        }
                                    })
                                    .notify(project);
                        }
                    }
                }
        );
    }

    private void processQueryStarted(InstanceConnection conn, KdbQuery query) {
        if (getOptions().isLogQueries()) {
            queryLogger.logQueryStarted(conn, query);
        }
        queryListeners.forEach(l -> l.queryStarted(conn, query));
    }

    private void processQueryFinished(InstanceConnection conn, KdbQuery query, KdbResult result) {
        if (getOptions().isLogQueries()) {
            queryLogger.processQueryFinished(conn, query, result);
        }
        queryListeners.forEach(l -> l.queryFinished(conn, query, result));
    }

    protected void processConnectionActivated(InstanceConnection old, InstanceConnection connection) {
        if (old == connection) {
            return;
        }
        connectionListeners.forEach(l -> l.connectionActivated(old, connection));

        if (connection != null) {
            final String content = "Active connection changed to: " + connection.getName();
            createNotification(content, NotificationType.INFORMATION).notify(project);

            final ExecutionOptions commonOptions = getOptions();
            if (commonOptions.isShowConnectionChange()) {
                final JComponent notificationComponent = InstancesComboAction.getInstance().getNotificationComponent();
                if (notificationComponent == null) {
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    final int timeout = commonOptions.getConnectionChangeTimeout();
                    final Dimension size = notificationComponent.getSize();
                    final Point aPointOnComponent = new Point(size.width / 2, size.height);
                    JBPopupFactory.getInstance().createBalloonBuilder(new JLabel(content))
                            .setDialogMode(false)
                            .setFadeoutTime(timeout)
                            .setCloseButtonEnabled(false)
                            .setRequestFocus(false)
                            .setSmallVariant(true)
                            .setBlockClicksThroughBalloon(false)
                            .createBalloon()
                            .show(new RelativePoint(notificationComponent, aPointOnComponent), Balloon.Position.below);
                });
            }
        }
    }

    public static KdbConnectionManager getManager(Project project) {
        return project == null ? null : project.getService(KdbConnectionManager.class);
    }

    private ExecutionOptions getOptions() {
        return KdbSettingsService.getInstance().getExecutionOptions();
    }

    private class QueryProgressive implements Progressive {
        private boolean canceled = false;
        private ProgressIndicator indicator;

        private final KdbQuery query;
        //        private final Consumer<KdbResult> handler;
        private final TheInstanceConnection myConnection;

        private final KdbResult result = new KdbResult();

        private static final int MB_SIZE = 1024 * 1024;

        private QueryProgressive(TheInstanceConnection myConnection, KdbQuery query) {
//        private QueryProgressive(TheInstanceConnection myConnection, KdbQuery query, Consumer<KdbResult> handler) {
            this.query = query;
//            this.handler = handler;
            this.myConnection = myConnection;
        }

        public KdbResult getResult() {
            return result;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            this.indicator = indicator;

            final Object query = this.query.toQueryObject(getOptions().isNormalizeQuery());

            safeQuery(query, indicator, getOptions().isAutoReconnect());
        }

        private boolean safeQuery(@NotNull Object query, @NotNull ProgressIndicator indicator, boolean retry) {
            try {
                validate(indicator);

                performQuery(query, indicator);

                indicator.checkCanceled();

                return true;
            } catch (IOException ex) {
                if (retry && myConnection.getState() != DISCONNECTED) {
                    myConnection.close(ex);
                    if (myConnection.connectAndWait() == CONNECTED) {
                        return safeQuery(query, indicator, false);
                    }
                }
                indicator.cancel();
                complete(ex);
                myConnection.close(ex);
            } catch (InterruptedException ignore) {
                indicator.cancel();
                // nothing to do here - the task has been done
            } catch (Throwable ex) {
                indicator.cancel();
                complete(ex);
            }
            return false;
        }

        private void performQuery(@NotNull Object query, @NotNull ProgressIndicator indicator) throws IOException, c.KException {
            final KxConnection c = myConnection.safeConnection();

            final Object object = c.query(query,
                    () -> checkCancelled(indicator),
                    this::validateMessageSize,
                    phase -> indicator.setText(phase.getDescription())
            );
            complete(object);
        }

        private void checkCancelled(ProgressIndicator indicator) throws CancellationException {
            if (canceled || indicator.isCanceled()) {
                throw new CancellationException("The query has been interrupted.");
            }
        }

        private void validateMessageSize(int size) throws CancellationException {
            if (size > getOptions().getWarningMessageMb() * MB_SIZE) {
                final int sizeMb = ((int) ((size / MB_SIZE) * 100d)) / 100;

                final CompletableFuture<Boolean> res = new CompletableFuture<>();
                ApplicationManager.getApplication().invokeAndWait(() -> {
                            final int i = Messages.showOkCancelDialog(project, "The response is " + sizeMb + "Mb and could take long processing time or cause out of memory error. Would you like to proceed the response?", "Big Result Warning", "Cancel The Query", "Proceed and Show Result", AllIcons.General.NotificationWarning);
                            res.complete(i == Messages.CANCEL);
                        }
                );
                try {
                    if (!res.get()) {
                        throw new CancellationException("The response is " + sizeMb + "Mb and was cancelled.");
                    }
                } catch (ExecutionException | InterruptedException ignore) {
                }
            }
        }

        private void validate(@NotNull ProgressIndicator indicator) throws InterruptedException {
            if (indicator.isCanceled()) {
                complete(new IllegalStateException("Query has been cancelled"));
                throw new InterruptedException();
            }
        }

        private void complete(Object res) {
            result.complete(res);
        }

        public void cancel() {
            canceled = true;
            if (indicator != null) {
                indicator.cancel();
            }
        }

        public boolean isCanceled() {
            return canceled || (indicator != null && indicator.isCanceled());
        }
    }

    private Notification createNotification(String content, NotificationType type) {
        return NotificationGroupManager.getInstance().getNotificationGroup("Kdb.ConnectionState").createNotification(content, type).setIcon(KdbIcons.Main.Notification);
    }

    /**
     * We redefine scope and parent here so instance can inherit its credentials.
     */
    private static class TemporalKdbInstance extends KdbInstance {
        private final KdbScope scope;

        public TemporalKdbInstance(KdbInstance instance, KdbScope scope) {
            super(instance.getName(), instance.getHost(), instance.getPort(), instance.getCredentials(), instance.getOptions());
            this.scope = scope;
        }

        @Override
        public KdbScope getScope() {
            // Has scope
            return scope;
        }

        @Override
        public StructuralItem getParent() {
            // But no parent
            return scope;
        }

        @Override
        protected void notifyItemUpdated() {
            // We don't notify anything. Scope here just for settings
        }
    }

    private class TheInstanceConnection implements InstanceConnection {
        private Exception error;
        private InstanceState state;
        private long stateChangeTime;

        private KxConnection myConnection;
        private QueryProgressive queryProgressive;

        private final KdbInstance instance;

        TheInstanceConnection(KdbInstance instance) {
            this.instance = instance;

            this.state = InstanceState.DISCONNECTED;
            this.stateChangeTime = System.currentTimeMillis();
        }

        @Override
        public KdbInstance getInstance() {
            return instance;
        }

        InstanceState test() throws Exception {
            try {
                connecting();

                final ConnectionProgressive progressive = new ConnectionProgressive(this);
                final Task task = new Task.Modal(project, "Testing Connection to Kdb Instance", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);

                        progressive.run(indicator);
                    }
                };
                task.queue();

                if (error != null) {
                    throw error;
                }
                return state;
            } finally {
                disconnect();
            }
        }

        @Override
        public void connect() {
            if (!state.isConnectable()) {
                return;
            }

            connecting();

            final ConnectionProgressive progressive = new ConnectionProgressive(this);
            final Task task = new Task.Backgroundable(project, "Connection to kdb instance", true, PerformInBackgroundOption.DEAF) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    progressive.run(indicator);
                }
            };
            task.queue();
        }

        @Override
        public InstanceState connectAndWait() {
            if (!state.isConnectable()) {
                return state;
            }

            connecting();

            final ConnectionProgressive progressive = new ConnectionProgressive(this);
            final Task task = new Task.Modal(project, "Connection to Kdb Instance", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    progressive.run(indicator);
                }
            };
            task.queue();
            return state;
        }


        @Override
        public void disconnect() {
            if (!state.isDisconnectable()) {
                return;
            }
            close(null);
        }

        @Override
        public boolean isTemporal() {
            return isTempInstance(instance);
        }

        @Override
        public long getStateChangeTime() {
            return stateChangeTime;
        }

        @Override
        public InstanceState getState() {
            return state;
        }

        @Override
        public Exception getDisconnectError() {
            return error;
        }

        void connecting() {
            updateState(InstanceState.CONNECTING);
        }

        void connected(KxConnection connection) {
            this.myConnection = connection;
            updateState(InstanceState.CONNECTED);
        }


        void close(Exception exception) {
            if (myConnection != null) {
                myConnection.close();
                myConnection = null;
            }

            if (state != InstanceState.DISCONNECTED) {
                updateState(InstanceState.DISCONNECTED, exception);
            }
        }

        private void updateState(InstanceState newState) {
            updateState(newState, null);
        }

        private void updateState(InstanceState newState, Exception exception) {
            InstanceState oldState = state;

            state = newState;
            error = exception;
            stateChangeTime = System.currentTimeMillis();

            // Notify only if contains - test is not in the scope, for example
            if (connections.containsKey(instance)) {
                processConnectionState(this, oldState, state);
            }
        }

        @Override
        public KdbQuery getQuery() {
            return queryProgressive != null ? queryProgressive.query : null;
        }

        @Override
        public KdbResult query(KdbQuery query) throws ConcurrentQueryException {
            try {
                final CompletableFuture<KdbResult> res = new CompletableFuture<>();
                doQuery(query, res::complete, true);
                return res.get();
            } catch (ConcurrentQueryException ex) {
                throw ex;
            } catch (Exception ex) {
                return new KdbResult().complete(ex);
            }
        }

        @Override
        public void query(KdbQuery query, Consumer<KdbResult> handler) throws ConcurrentQueryException {
            doQuery(query, handler, false);
        }

        @Override
        public void cancelQuery() {
            if (queryProgressive != null) {
                if (queryProgressive.isCanceled()) {
                    Messages.showInfoMessage("The query has been cancelled but not any phase can be terminated. If you'd like immediate result, try to reconnect the instance.", "Cancelling Is in Progress");
                } else {
                    queryProgressive.cancel();
                }
            }
        }

        @Override
        public boolean isQueryCancelled() {
            return queryProgressive != null && queryProgressive.isCanceled();
        }

        private void doQuery(KdbQuery query, Consumer<KdbResult> handler, boolean modal) throws ConcurrentQueryException {
            if (getQuery() != null) {
                throw new ConcurrentQueryException("Another query is already running");
            }

            queryProgressive = new QueryProgressive(this, query);
            processQueryStarted(TheInstanceConnection.this, query);
            createTask(modal, queryProgressive, () -> {
                final KdbResult result = queryProgressive.getResult();
                queryProgressive = null;
                handler.accept(result);
                processQueryFinished(TheInstanceConnection.this, query, result);
            }).queue();
        }

        private Task createTask(boolean modal, QueryProgressive progressive, Runnable finished) {
            final String title = "Executing query on instance " + instance;
            if (modal) {
                return new Task.Modal(project, title, true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        queryProgressive.run(indicator);
                    }

                    @Override
                    public void onFinished() {
                        finished.run();
                    }
                };
            } else {
                return new Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        queryProgressive.run(indicator);
                    }

                    @Override
                    public void onFinished() {
                        finished.run();
                    }
                };
            }
        }

        private KxConnection safeConnection() throws IOException {
            if (myConnection == null) {
                throw new IOException("Instance is not connected");
            }
            return myConnection;
        }

        @Override
        public String toString() {
            return "TheInstanceConnection{" +
                    "instance=" + instance +
                    ", state=" + state +
                    ", error=" + error +
                    ", stateChangeTime=" + stateChangeTime +
                    '}';
        }
    }

    private class ConnectionProgressive implements Progressive {
        private KxConnection connection = null;
        private ScheduledFuture<?> progressFeature = null;

        private final TheInstanceConnection myConnection;

        public ConnectionProgressive(TheInstanceConnection connection) {
            this.myConnection = connection;
        }

        private boolean isCancelled(@NotNull ProgressIndicator indicator) {
            return myConnection.state == InstanceState.DISCONNECTED || indicator.isCanceled();
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            final KdbInstance instance = myConnection.instance;

            final String instanceName = instance.getName();
            final InstanceOptions options = InstanceOptions.resolveOptions(instance);
            final int timeout = options.getSafeTimeout();

            try {
                final String credentials = CredentialService.resolveCredentials(instance);

                if (isCancelled(indicator)) {
                    return;
                }

                indicator.setText("Connecting to KDB Instance: " + instanceName);
                indicator.setText2("with timeout " + timeout + "ms");

                // 10ms is graduation
                final double ticks = timeout / (double) PROGRESS_TICK_MILLIS;

                // create connection - an error could be here
                connection = new KxConnection(instance.getHost(), instance.getPort(), options.isSafeAsync(), options.isSafeTls(), options.isSafeZip(), options.getSafeEncoding());
                if (isCancelled(indicator)) {
                    throw new CancellationException();
                }

                // We count from 1 to 0 as it's timeout connection
                indicator.setIndeterminate(false);
                indicator.setFraction(1);

                // Prepare progress and cancellation monitoring
                progressFeature = connectionProgressExecutor.scheduleAtFixedRate(new Runnable() {
                    int i = 0;

                    @Override
                    public void run() {
                        if (myConnection.state == InstanceState.DISCONNECTED) {
                            progressFeature.cancel(true);
                            return;
                        }

                        if (i >= ticks || indicator.isCanceled()) {
                            // cancel connection
                            indicator.setFraction(0);
                            progressFeature.cancel(true);
                            connection.close();

                            if (indicator.isCanceled()) {
                                indicator.setText2("Cancelling connection...");
                            } else if (i == ticks) {
                                indicator.setText2("Connection expiring. Interruption...");
                            }
                        } else {
                            indicator.setFraction(1 - (i / ticks));
                            indicator.setText2("Waiting " + (timeout - i * PROGRESS_TICK_MILLIS) + "ms more before interruption");
                            i++;
                        }
                    }
                }, PROGRESS_TICK_MILLIS, PROGRESS_TICK_MILLIS, TimeUnit.MILLISECONDS);

                // and try to get response from KDB
                connection.authenticate(credentials);
                if (isCancelled(indicator)) {
                    throw new CancellationException();
                }

                // connected
                myConnection.connected(connection);
            } catch (Exception ex) {
                if (connection != null) {
                    connection.close();
                }

                if (isCancelled(indicator)) {
                    myConnection.close(null);
                } else {
                    if (indicator.getFraction() == 0) {
                        myConnection.close(new IOException("Interrupted by timeout after " + timeout + "ms"));
                    } else {
                        myConnection.close(ex);
                    }
                }
            } finally {
                if (progressFeature != null) {
                    progressFeature.cancel(true);
                    progressFeature = null;
                }
            }
        }
    }
}