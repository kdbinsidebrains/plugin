package org.kdb.inside.brains.core;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.settings.KdbSettingsService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.DosFileAttributeView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@State(name = "KdbQueryLogger", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class KdbQueryLogger implements PersistentStateComponent<KdbQueryLogger.State>, Disposable, DumbAware {
    private State state;
    private Path logsFolder;

    private static final String FILE_HEADER = "" + "/\n" + "File format:\n" + " <timestamp>, <instance uri>, <instance name>, <roundtrip, nanos>, <result type or error message>\n" + " <Full query>\n" + " <line separator>\n" + "\\\n";

    private static final Logger log = Logger.getInstance(KdbQueryLogger.class);

    private static final DateTimeFormatter SPLIT_BY_MONTHS = DateTimeFormatter.ofPattern("yyyy.MM");

    public KdbQueryLogger(Project project) {
        final String basePath = project.getBasePath();
        if (basePath != null) {
            logsFolder = Path.of(basePath, ".kdbinb");
        } else {
            logsFolder = null;
        }
    }

    public void logQueryStarted(InstanceConnection connection, KdbQuery query) {
    }

    public void processQueryFinished(InstanceConnection connection, KdbQuery query, KdbResult result) {
        if (logsFolder == null) {
            return;
        }

        final Path logFile = getCurrentLogFile();
        lock(logFile, false);
        try {
            final StringBuilder b = new StringBuilder("/ ");
            b.append(result.getTimeAsTimestampString());
            b.append(", ");
            b.append(connection.getDetails());
            b.append(", ");
            b.append(connection.getCanonicalName());
            b.append(", ");
            b.append(result.getRoundtripNanos());
            b.append(", ");
            if (result.getObject() == null) {
                b.append("null");
            } else if (result.isError()) {
                b.append("'").append(((Exception) result.getObject()).getMessage());
            } else {
                b.append(result.getObject().getClass().getSimpleName().toLowerCase());
            }
            b.append(System.lineSeparator());
            b.append(query.getExpression());
            if (query.getArguments() != null) {
                b.append("[");
                for (Object argument : query.getArguments()) {
                    b.append(argument);
                    b.append(";");
                }
                b.setCharAt(b.length() - 1, ']');
            }
            b.append(System.lineSeparator());
            b.append(System.lineSeparator());

            Files.writeString(logFile, b, StandardOpenOption.APPEND);
        } catch (Exception ex) {
            log.error("Query can't be stored in logs csv: " + logFile, ex);
        } finally {
            lock(logFile, true);
        }
    }

    private void lock(Path logFile, boolean b) {
        try {
            final DosFileAttributeView attrs = Files.getFileAttributeView(logFile, DosFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            if (attrs != null) {
                attrs.setReadOnly(b);
            }
        } catch (IOException ignore) {
        }
    }

    private Path getCurrentLogFile() {
        final LocalDate now = LocalDate.now();

        Path folder = logsFolder;
        if (KdbSettingsService.getInstance().getConnectionOptions().isSplitLogsByMonths()) {
            folder = folder.resolve(now.format(SPLIT_BY_MONTHS));
        }

        if (Files.notExists(folder)) {
            try {
                Files.createDirectories(folder);
            } catch (IOException ex) {
                log.error("Logs folder can't be created: " + folder, ex);
            }
        }

        final Path resolve = folder.resolve("queries" + now + ".log");
        if (Files.notExists(resolve)) {
            try {
                Files.writeString(resolve, FILE_HEADER, StandardOpenOption.CREATE);
            } catch (IOException ex) {
                log.error("Log file can't be created or header can't be written: " + resolve, ex);
            }
        }
        return resolve;
    }

    @Override
    public @Nullable KdbQueryLogger.State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    @Override
    public void dispose() {
        if (logsFolder != null) {
            logsFolder = null;
        }
    }

    public static class State {
    }
}