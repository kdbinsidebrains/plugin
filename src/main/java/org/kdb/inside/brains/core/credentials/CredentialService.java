package org.kdb.inside.brains.core.credentials;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import icons.KdbIcons;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.settings.KdbSettingsConfigurable;
import org.kdb.inside.brains.settings.KdbSettingsService;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@com.intellij.openapi.components.State(name = "ConnectionProviders", storages = {@Storage("kdb-settings.xml")})
public class CredentialService implements PersistentStateComponent<Element> {
    private final File credentialsDir;
    private final List<CredentialPlugin> my_plugins = new ArrayList<>();

    private static final Logger log = Logger.getInstance(CredentialService.class);
    private static CredentialService instance = null;

    private CredentialService() {
        credentialsDir = new File(PathManager.getSystemPath(), "KdbInsideBrains");
        if (!credentialsDir.exists() && !credentialsDir.mkdirs()) {
            log.error("System folder for KdbInsideBrains can't be created");
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

    public static String resolveCredentials(KdbInstance instance) throws CredentialsResolvingException {
        String credentials = instance.getCredentials();
        // Inherit from scope
        if (credentials == null && instance.getScope() != null) {
            credentials = instance.getScope().getCredentials();
        }
        // Or take default if no luck
        if (credentials == null) {
            credentials = KdbSettingsService.getInstance().getDefaultCredentials();
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
        return StrSubstitutor.replaceSystemProperties(credentials);
    }

    public List<CredentialProvider> getProviders() {
        return Stream.concat(my_plugins.stream().map(CredentialPlugin::getProvider), Stream.of(UsernameCredentialProvider.INSTANCE)).collect(Collectors.toList());
    }

    public List<CredentialPlugin> getPlugins() {
        return Collections.unmodifiableList(my_plugins);
    }

    public void setPlugins(List<CredentialPlugin> plugins) throws IOException, CredentialPluginException {
        if (my_plugins.equals(plugins)) {
            return;
        }

        log.info("Updating plugins list from " + my_plugins.stream().map(CredentialPlugin::getName) + " to " + plugins.stream().map(CredentialPlugin::getName));

        my_plugins.forEach(p -> {
            try {
                log.info("Destroying plugin " + p.getName());
                p.close();
            } catch (IOException e) {
                log.warn("Plugin can't be closed correctly: " + p, e);
            }
        });
        my_plugins.clear();

        final Set<CredentialPlugin> added = new HashSet<>();
        for (CredentialPlugin plugin : plugins) {
            final URL origResource = plugin.getResource();
            final CredentialPlugin p = grabPlugin(plugin.getId(), origResource);
            if (added.add(p)) {
                my_plugins.add(p);
            }
        }

        log.info("Plugins updated");
    }

    private File createLocalFile(String id) {
        return new File(credentialsDir, id + ".jar");
    }

    private URL createLocalResource(String id) throws MalformedURLException {
        return createLocalFile(id).toURI().toURL();
    }

    private CredentialPlugin grabPlugin(String id, URL from) throws IOException, CredentialPluginException {
        final URL localResource = createLocalResource(id);
        if (localResource.equals(from)) {
            return new CredentialPlugin(localResource);
        }

        try (final InputStream in = from.openStream(); final OutputStream out = new FileOutputStream(localResource.getFile())) {
            FileUtil.copy(in, out);
        }
        return new CredentialPlugin(localResource);
    }

    public CredentialPlugin verifyPlugin(URL url) throws CredentialPluginException, IOException {
        // It closes the plugin just after loading
        try (CredentialPlugin plugin = new CredentialPlugin(url)) {
            return plugin;
        }
    }

    @Override
    public @Nullable Element getState() {
        final Element e = new Element("plugins");
        for (CredentialPlugin plugin : my_plugins) {
            e.addContent(new Element("plugin").setAttribute("id", plugin.getId()));
        }
        return e;
    }

    @Override
    public void loadState(@NotNull Element state) {
        final List<CredentialPlugin> plugins = new ArrayList<>();
        for (Element element : state.getChildren("plugin")) {
            final CredentialPlugin plugin;
            final String id = element.getAttributeValue("id");
            try {
                if (id != null) {
                    plugin = new CredentialPlugin(createLocalResource(id));
                } else {
                    final String oldUrl = element.getAttributeValue("url");
                    if (oldUrl == null) {
                        throw new IllegalStateException("Plugin element has nor id nor url attributes");
                    }

                    final CredentialPlugin remotePlugin = verifyPlugin(new URL(oldUrl));
                    plugin = grabPlugin(remotePlugin.getId(), remotePlugin.getResource());
                }
                plugins.add(plugin);
            } catch (Exception ex) {
                log.error("Plugin can't be for element: " + element.getText(), ex);
                notifyPluginFailedPlugin(ex);
            }
        }

        try {
            setPlugins(plugins);
        } catch (Exception ex) {
            log.error("Plugin can't be updated", ex);
            notifyPluginFailedPlugin(ex);
        }
    }

    public static CredentialProvider findProvider(Collection<CredentialProvider> providers, String credentials) {
        return providers.stream().filter(p -> p.isSupported(credentials)).findFirst().orElse(UsernameCredentialProvider.INSTANCE);
    }

    private void notifyPluginFailedPlugin(Throwable ex) {
        final String content = "Credentials plugin can't be loaded: " + ex.getMessage();

        NotificationGroupManager.getInstance().getNotificationGroup("Kdb.CredentialsService")
                .createNotification(content, NotificationType.ERROR)
                .setIcon(KdbIcons.Main.Notification)
                .addAction(new DumbAwareAction("Change Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        final Project project = e.getProject();
                        if (project != null) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, KdbSettingsConfigurable.class);
                        }
                    }
                }).notify(null);
    }
}