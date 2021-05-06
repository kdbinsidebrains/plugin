package org.kdb.inside.brains.core.credentials;

import com.google.common.collect.Lists;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.core.credentials.plugin.CredentialPlugin;
import org.kdb.inside.brains.settings.KdbSettingsConfigurable;
import org.kdb.inside.brains.settings.KdbSettingsService;

import java.net.URL;
import java.util.*;

@com.intellij.openapi.components.State(name = "ConnectionProviders", storages = {@Storage("kdb-settings.xml")})
public class CredentialProviderService implements PersistentStateComponent<Element> {
    private final List<CredentialProvider> providers = new ArrayList<>();

    private final List<URL> credentialPlugins = new ArrayList<>();
    private final Map<URL, CredentialPlugin> loadedCredentialPlugins = new HashMap<>();

    private static CredentialProviderService instance = null;

    private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("Kdb.CredentialsService");

    private CredentialProviderService() {
        providers.add(UsernameCredentialProvider.INSTANCE);
    }

    public List<CredentialProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public CredentialProvider getProvider(String credentials) {
        return CredentialProvider.findProvider(providers, credentials);
    }

    public CredentialProvider getProviderByName(String name) {
        return providers.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public void registerProvider(CredentialProvider provider) {
        registerProvider(-1, provider);
    }

    public void registerProvider(int priority, CredentialProvider provider) {
        if (provider == null) {
            return;
        }

        if (priority >= 0 && priority < providers.size()) {
            providers.add(priority, provider);
        } else {
            providers.add(providers.size(), provider);
        }
    }

    public void unregisterProvider(CredentialProvider provider) {
        if (provider != null) {
            providers.remove(provider);
        }
    }

    public void setCredentialPlugin(List<URL> data) {
        for (CredentialPlugin value : loadedCredentialPlugins.values()) {
            unregisterProvider(value.getProvider());
        }

        credentialPlugins.clear();
        credentialPlugins.addAll(data);

        reloadPlugins();
    }

    public List<URL> getCredentialPlugins() {
        return Collections.unmodifiableList(credentialPlugins);
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
        final CredentialProvider provider = getInstance().getProvider(credentials);
        if (provider == null) {
            return credentials;
        }

        // Resolve
        final String resolved = provider.resolveCredentials(credentials);

        // Resolved or default?
        return resolveProperties(resolved != null ? resolved : credentials);
    }

    static String resolveProperties(String credentials) {
        return StrSubstitutor.replaceSystemProperties(credentials);
    }

    private void reloadPlugins() {
        loadedCredentialPlugins.clear();
        for (URL url : Lists.reverse(credentialPlugins)) {
            try {
                final CredentialPlugin load = CredentialPlugin.load(url);
                loadedCredentialPlugins.put(url, load);
                registerProvider(0, load.getProvider());
            } catch (Throwable ex) {
                notifyPluginFailedPlugin(url, ex);
            }
        }
    }

    private void notifyPluginFailedPlugin(URL url, Throwable ex) {
        NOTIFICATION_GROUP.createNotification("Credentials plugin can't be loaded from " + url.toString() + ": " + ex.getMessage(), NotificationType.ERROR).addAction(new AnAction("Change Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final Project project = e.getProject();
                if (project != null) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, KdbSettingsConfigurable.class);
                }
//                ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowSettingsUtilImpl.getConfigurableGroups(project, true));
            }
        }).notify(null);
    }

    public static CredentialProviderService getInstance() {
        if (instance == null) {
            instance = ServiceManager.getService(CredentialProviderService.class);
        }
        return instance;
    }

    @Override
    public @Nullable Element getState() {
        final Element e = new Element("plugins");
        for (URL credentialPlugin : credentialPlugins) {
            final Element u = new Element("plugin");
            u.setAttribute("url", credentialPlugin.toString());
            e.addContent(u);
        }
        return e;
    }

    @Override
    public void loadState(@NotNull Element state) {
        credentialPlugins.clear();

        final List<Element> plugin = state.getChildren("plugin");
        for (Element element : plugin) {
            try {
                credentialPlugins.add(new URL(element.getAttributeValue("url")));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        reloadPlugins();
    }
}
