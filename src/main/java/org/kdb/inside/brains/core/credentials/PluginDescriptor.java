package org.kdb.inside.brains.core.credentials;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record PluginDescriptor(String id, String name, String version, String description) {
    public PluginDescriptor(String name, String version, String description) {
        this(generateId(name), name, version, description);
    }

    private static @NotNull String generateId(String name) {
        return Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8)).toLowerCase();
    }

    public static PluginDescriptor from(CredentialProvider provider) {
        return new PluginDescriptor(provider.getName(), provider.getVersion(), provider.getDescription());
    }
}