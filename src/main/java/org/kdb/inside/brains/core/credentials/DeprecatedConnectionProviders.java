package org.kdb.inside.brains.core.credentials;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal class for the component migration. Must be removed one day.
 * <p>
 * Loaded only once and removes everything after startup.
 */
@Service(Service.Level.APP)
@State(name = "ConnectionProviders", storages = {@Storage("kdb-settings.xml")})
final class DeprecatedConnectionProviders implements PersistentStateComponent<Element> {
    private final Path rootDir;
    private final List<Path> plugins = new ArrayList<>();

    public DeprecatedConnectionProviders() {
        rootDir = Path.of(PathManager.getSystemPath(), "KdbInsideBrains");
    }

    List<Path> getPlugins() {
        return plugins;
    }

    /**
     * Always return null
     */
    @Override
    public @Nullable Element getState() {
        return null;
    }

    @Override
    public void loadState(@NotNull Element state) {
        for (Element element : state.getChildren("plugin")) {
            final String id = element.getAttributeValue("id");
            plugins.add(rootDir.resolve(id + ".jar"));
        }
    }
}