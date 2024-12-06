package org.kdb.inside.brains.core.credentials;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Property;
import com.intellij.util.xmlb.annotations.XCollection;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.settings.KdbSettingsConfigurable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.kdb.inside.brains.UIUtils.replaceSystemProperties;

@com.intellij.openapi.components.State(name = "CredentialService", storages = {@Storage("kdb-settings.xml")})
public class CredentialService implements PersistentStateComponent<CredentialService.State> {
    public static final String DEFAULT_CREDENTIALS = "${user.name}";
    private static final String CREDENTIAL_ATTRIBUTE = "KdbInsideBrainsGlobalCredentials";
    private static final Logger log = Logger.getInstance(CredentialService.class);
    private final Path rootDir;
    private final State state = new State();
    private final List<CredentialPlugin> plugins = new ArrayList<>();

    private static CredentialService instance = null;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private CredentialService() {
        rootDir = Path.of(System.getProperty("user.home"), ".kdbinb");
    }

    public static CredentialProvider findProvider(Collection<CredentialProvider> providers, String credentials) {
        return providers.stream().filter(p -> p.isSupported(credentials)).findFirst().orElse(UsernameCredentialProvider.INSTANCE);
    }

    public static String resolveCredentials(KdbInstance instance) throws CredentialsResolvingException {
        String credentials = instance.getCredentials();
        // Inherit from scope
        if (credentials == null && instance.getScope() != null) {
            credentials = instance.getScope().getCredentials();
        }
        // Or take default if no luck
        if (credentials == null) {
            credentials = getInstance().getDefaultCredentials();
        }

        // Get provider
        final CredentialProvider provider = findProvider(credentials);
        if (provider == null) {
            return credentials;
        }

        // Resolve
        final String resolved = provider.resolveCredentials(instance.getHost(), instance.getPort(), credentials);

        // Resolved or default?
        credentials = resolved != null ? resolved : credentials;

        // resolve system vars
        return replaceSystemProperties(credentials);
    }

    public static boolean isPlugin(Path resource) {
        if (resource == null || !resource.toString().endsWith(".jar")) {
            return false;
        }
        try {
            verifyPlugin(resource);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static PluginDescriptor verifyPlugin(Path resource) throws CredentialPluginException, IOException {
        if (!Files.exists(resource)) {
            throw new IOException("File doesn't exist: " + resource.toAbsolutePath());
        }

        try (CredentialPlugin plugin = new CredentialPlugin(resource)) {
            return plugin.getDescriptor();
        }
    }

    private static Path createPluginFile(Path root, String id) {
        return root.resolve("credentials-" + id + ".jar").toAbsolutePath();
    }

    public List<Path> getPluginResources() {
        return plugins.stream().map(CredentialPlugin::getResource).toList();
    }

    public List<CredentialPlugin> getPlugins() {
        readLock.lock();
        try {
            return Collections.unmodifiableList(plugins);
        } finally {
            readLock.unlock();
        }
    }

    public List<CredentialProvider> getProviders() {
        readLock.lock();
        try {
            return Stream.concat(plugins.stream().map(CredentialPlugin::getProvider), Stream.of(UsernameCredentialProvider.INSTANCE)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public void changePlugins(List<Path> sources) throws CredentialPluginException {
        final List<PluginError> errors = reloadPlugins(sources, true);
        if (!errors.isEmpty()) {
            throw new CredentialPluginException("Some plugins can't be loaded");
        }
    }

    public static CredentialService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(CredentialService.class);
        }
        return instance;
    }

    public static CredentialProvider findProvider(String credentials) {
        return findProvider(getInstance().getProviders(), credentials);
    }

    public String getDefaultCredentials() {
        return state.defaultCredential;
    }

    public void setDefaultCredentials(String credentials) {
        Objects.requireNonNull(credentials, "Default credentials can't be null");
        state.defaultCredential = credentials;
    }

    @Override
    public @Nullable State getState() {
        readLock.lock();
        try {
            return state;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state.defaultCredential = state.defaultCredential;

        initializeLoadedState(state.pluginIds.stream().map(id -> createPluginFile(rootDir, id)).toList());
    }

    // Migration from previous versions
    @Override
    public void noStateLoaded() {
        final String res = PasswordSafe.getInstance().getPassword(new CredentialAttributes(CREDENTIAL_ATTRIBUTE));
        state.defaultCredential = res == null ? DEFAULT_CREDENTIALS : res;

        final List<Path> plugins = ApplicationManager.getApplication()
                .getService(DeprecatedConnectionProviders.class)
                .getPlugins();
        initializeLoadedState(plugins);
    }

    private void initializeLoadedState(List<Path> sources) {
        final List<Path> completeList = new ArrayList<>(sources);
        // load all files for the folder
        try (final Stream<Path> list = Files.list(rootDir)) {
            list.filter(CredentialService::isPlugin).map(Path::toAbsolutePath).forEach(p -> {
                if (!completeList.contains(p)) {
                    completeList.add(p);
                }
            });
        } catch (Exception ignore) {
        }

        if (completeList.isEmpty()) {
            return;
        }

        final List<PluginError> pluginErrors = reloadPlugins(completeList, false);
        for (PluginError error : pluginErrors) {
            notifyPluginFailedPlugin(error.path, error.ex);
        }
    }

    private List<PluginError> reloadPlugins(List<Path> sources, boolean allOrNothing) {
        writeLock.lock();
        try {
            return reloadPluginsImpl(sources, allOrNothing);
        } finally {
            writeLock.unlock();
        }
    }

    private List<PluginError> reloadPluginsImpl(List<Path> sources, boolean allOrNothing) {
        final List<Path> currentPlugins = new ArrayList<>(plugins.stream().map(CredentialPlugin::getResource).toList());
        if (currentPlugins.equals(sources)) {
            return List.of();
        }

        log.info("Updating plugins list from " + currentPlugins + " to " + sources);
        // Remove exist plugins if any and exit
        if (sources.isEmpty() && currentPlugins.isEmpty()) {
            return List.of();
        }

        final Path root = getOrCreateRoot();

        // Verify all reloading plugins
        final Verifier verify = Verifier.verify(root, sources);
        if (!verify.errors.isEmpty() && allOrNothing) {
            return verify.errors;
        }

        // close exist plugins
        final List<Path> closedPlugins = closeAndClearPlugins();
        try {
            state.pluginIds.clear();
            for (PluginMapping mapping : verify.mapping) {
                try {
                    if (!mapping.source.equals(mapping.target)) {
                        // if source in the same root folder - move it!
                        if (mapping.source.startsWith(rootDir)) {
                            Files.move(mapping.source, mapping.target, StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            Files.copy(mapping.source, mapping.target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    final CredentialPlugin p = new CredentialPlugin(mapping.target);
                    plugins.add(p);
                    state.pluginIds.add(p.getId());
                    closedPlugins.remove(mapping.target);
                } catch (Exception ex) {
                    verify.errors.add(new PluginError(mapping.source, ex));
                }
            }
            log.info("Plugins updated with errors: " + verify.errors);
            return verify.errors;
        } finally {
            updateReadme();
            cleanupPluginFiles(closedPlugins);
        }
    }

    private void updateReadme() {
        try {
            final List<String> lines = new ArrayList<>();
            // header
            lines.add("This directory contains list of credential providers for KdbInsideBrains JetBrains IDEA plugin: https://www.kdbinsidebrains.dev");
            for (CredentialPlugin plugin : plugins) {
                lines.add("");
                final Path resource = plugin.getResource();
                final PluginDescriptor d = plugin.getDescriptor();
                lines.add(resource.getFileName().toString());
                lines.add("\tName: " + d.name());
                lines.add("\tVersion: " + d.version());
                lines.add("\tDescription: " + d.description());
            }
            Files.writeString(rootDir.resolve("readme.txt"), String.join(System.lineSeparator(), lines), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        } catch (Exception ex) {
            log.warn("Readme file can't be updated: " + ex.getMessage());
        }
    }

    private void cleanupPluginFiles(List<Path> closedPlugins) {
        if (closedPlugins.isEmpty()) {
            return;
        }

        log.info("Remove unused plugins: " + closedPlugins);
        for (Path closedPlugin : closedPlugins) {
            try {
                Files.deleteIfExists(closedPlugin);
            } catch (Exception ex) {
                log.warn("Credential plugin can't be removed: " + closedPlugin);
            }
        }
    }

    /**
     * Closes and clears the plugins' list returning list of closed resources.
     */
    private List<Path> closeAndClearPlugins() {
        final List<Path> paths = new ArrayList<>();
        for (CredentialPlugin p : plugins) {
            paths.add(p.getResource());
            try {
                log.info("Destroying plugin " + p.getDescriptor());
                p.close();
            } catch (IOException e) {
                log.warn("Plugin can't be closed correctly: " + p, e);
            }
        }
        plugins.clear();
        return paths;
    }

    private Path getOrCreateRoot() {
        if (Files.exists(rootDir)) {
            return rootDir;
        }

        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            log.error("System folder for KdbInsideBrains can't be created: " + e.getMessage(), e);
        }
        return rootDir;
    }

    private void notifyPluginFailedPlugin(Path path, Throwable ex) {
        final String content = "Credentials plugin can't be loaded from " + path.toAbsolutePath() + ": " + ex.getMessage();

        NotificationGroupManager.getInstance().getNotificationGroup("Kdb.CredentialsService").createNotification(content, NotificationType.ERROR).setIcon(KdbIcons.Main.Notification).addAction(new DumbAwareAction("Change Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final Project project = e.getProject();
                if (project != null) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, KdbSettingsConfigurable.class);
                }
            }
        }).notify(null);
    }

    private record PluginError(Path path, Exception ex) {
    }

    private record PluginMapping(String id, Path source, Path target) {
    }

    private record Verifier(List<PluginMapping> mapping, List<PluginError> errors) {
        static Verifier verify(Path root, List<Path> sources) {
            final List<PluginError> errors = new ArrayList<>();

            final Map<String, PluginDescriptor> conflicts = new HashMap<>();
            final List<PluginMapping> mappings = new ArrayList<>(sources.size());
            for (Path source : sources) {
                try {
                    final PluginDescriptor d = verifyPlugin(source);
                    final String id = d.id();
                    final PluginDescriptor existD = conflicts.get(id);
                    if (existD == null || d.version().compareTo(existD.version()) > 0) {
                        conflicts.put(id, d);

                        final PluginMapping plugin = new PluginMapping(id, source, createPluginFile(root, id));
                        final int i = indexOf(id, mappings);
                        if (i < 0) {
                            mappings.add(plugin);
                        } else {
                            mappings.set(i, plugin);
                        }
                    }
                } catch (Exception ex) {
                    errors.add(new PluginError(source, ex));
                }
            }
            return new Verifier(mappings, errors);
        }

        private static int indexOf(String id, List<PluginMapping> mappings) {
            int i = 0;
            for (PluginMapping p : mappings) {
                if (id.equals(p.id)) {
                    return i;
                }
                i++;
            }
            return -1;
        }
    }

    public static class State {
        @XCollection(propertyElementName = "plugins", elementName = "plugin")
        private final List<String> pluginIds = new ArrayList<>();
        @Property
        private String defaultCredential;
    }
}