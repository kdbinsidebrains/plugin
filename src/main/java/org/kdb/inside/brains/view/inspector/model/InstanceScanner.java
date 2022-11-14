package org.kdb.inside.brains.view.inspector.model;

import com.google.common.io.Resources;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceScanner {
    private static final Logger log = Logger.getInstance(InstanceScanner.class);
    private final String query;
    private final Project project;
    private final ScanListener listener;
    private final Map<InstanceConnection, Task> activeQueries = new ConcurrentHashMap<>();

    public InstanceScanner(@NotNull Project project, @NotNull ScanListener listener) {
        query = getScanQuery();
        this.project = project;
        this.listener = listener;
    }

    private static String getScanQuery() {
        try {
            final String name = "/org/kdb/inside/brains/inspector.q";
            final URL resource = InstanceScanner.class.getResource(name);
            if (resource != null) {
                return Resources.toString(resource, StandardCharsets.UTF_8);
            }
            log.error("Scan query can't be loaded. Resource not found: " + name);
        } catch (Exception ex) {
            log.error("Scan query can't be loaded", ex);
        }
        return null;
    }

    public void scanInstance(InstanceConnection connection) {
        activeQueries.computeIfAbsent(connection, this::startThread);
    }

    private void processError(InstanceConnection connection, Exception err) {
        ApplicationManager.getApplication().invokeLater(() -> listener.scanFailed(connection, err), ModalityState.any());
    }

    private void processResponse(InstanceConnection connection, KdbResult result) {
        final InstanceElement ie = new InstanceElement(connection, result);
        ApplicationManager.getApplication().invokeLater(() -> listener.scanFinished(connection, ie), ModalityState.any());
    }

    private Task startThread(InstanceConnection connection) {
        Task.Backgroundable task = new Task.Backgroundable(project, "Scanning instance " + connection.getCanonicalName(), false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                if (!indicator.isCanceled()) {
                    doQueryWithResult(connection);
                    activeQueries.remove(connection);
                }
            }
        };
        task.queue();
        return task;
    }

    private void doQueryWithResult(InstanceConnection connection) {
        try {
            if (query == null) {
                throw new IllegalStateException("Scan query can't be loaded from resources");
            }

            final KdbResult result = connection.query(new KdbQuery(query));
            final Object object = result.getObject();
            if (object instanceof Exception) {
                processError(connection, (Exception) object);
            } else {
                processResponse(connection, result);
            }
        } catch (Exception ex) {
            processError(connection, ex);
        }
    }

    public interface ScanListener {
        void scanFailed(InstanceConnection connection, Exception exception);

        void scanFinished(InstanceConnection connection, InstanceElement result);
    }
}