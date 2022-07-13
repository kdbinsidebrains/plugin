package org.kdb.inside.brains.core.credentials;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

final class CredentialPlugin implements Closeable {
    private final String id;
    private final String name;
    private final String version;
    private final String description;

    private final URL resource;

    private URLClassLoader classLoader;
    private CredentialProvider provider;

    CredentialPlugin(File file) throws IOException, CredentialPluginException {
        this(file.toURI().toURL());
    }

    CredentialPlugin(URL url) throws IOException, CredentialPluginException {
        this.resource = url;

        try (JarInputStream i = new JarInputStream(url.openStream())) {
            final Manifest manifest = i.getManifest();
            if (manifest == null) {
                throw new CredentialPluginException("Resource doesn't have Manifest file");
            }

            final String credentialsProvider = manifest.getMainAttributes().getValue("CredentialsProvider");
            if (credentialsProvider == null) {
                throw new CredentialPluginException("Manifest doesn't have mandatory CredentialsProvider attribute");
            }

            classLoader = new URLClassLoader(new URL[]{url}, CredentialProvider.class.getClassLoader());
            final Class<?> aClass = classLoader.loadClass(credentialsProvider);
            if (!(CredentialProvider.class.isAssignableFrom(aClass))) {
                throw new CredentialPluginException("CredentialProvider class doesn't implement CredentialProvider interface: " + aClass);
            }

            final Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();

            this.provider = (CredentialProvider) declaredConstructor.newInstance();

            this.id = Base64.getEncoder().encodeToString(provider.getName().getBytes(StandardCharsets.UTF_8)).toLowerCase();
            this.name = provider.getName();
            this.version = provider.getVersion();
            this.description = provider.getDescription();
        } catch (ClassNotFoundException ex) {
            throw new CredentialPluginException("Resource doesn't have CredentialProvider class", ex);
        } catch (NoSuchMethodException ex) {
            throw new CredentialPluginException("CredentialProvider class doesn't have default constructor", ex);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new CredentialPluginException("CredentialProvider object can't be created", ex);
        }
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getVersion() {
        return version;
    }

    String getDescription() {
        return description;
    }

    URL getResource() {
        return resource;
    }

    CredentialProvider getProvider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        provider = null;

        classLoader.close();
        classLoader = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CredentialPlugin plugin = (CredentialPlugin) o;
        return id.equals(plugin.id) && resource.equals(plugin.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, resource);
    }

    @Override
    public String toString() {
        return "CredentialPlugin{" + "id=" + id + ", name=" + getName() + ", version=" + getVersion() + '}';
    }
}