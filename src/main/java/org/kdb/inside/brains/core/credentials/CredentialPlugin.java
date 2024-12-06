package org.kdb.inside.brains.core.credentials;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public final class CredentialPlugin implements Closeable {
    private final Path resource;
    private final PluginDescriptor descriptor;

    private URLClassLoader classLoader;
    private CredentialProvider provider;

    CredentialPlugin(Path resource) throws IOException, CredentialPluginException {
        this.resource = Objects.requireNonNull(resource);

        try (JarInputStream i = new JarInputStream(Files.newInputStream(resource))) {
            final Manifest manifest = i.getManifest();
            if (manifest == null) {
                throw new CredentialPluginException("Resource doesn't have Manifest file");
            }

            final String credentialsProvider = manifest.getMainAttributes().getValue("CredentialsProvider");
            if (credentialsProvider == null) {
                throw new CredentialPluginException("Manifest doesn't have mandatory CredentialsProvider attribute");
            }

            classLoader = new URLClassLoader(new URL[]{resource.toUri().toURL()}, CredentialProvider.class.getClassLoader());
            final Class<?> aClass = classLoader.loadClass(credentialsProvider);
            if (!(CredentialProvider.class.isAssignableFrom(aClass))) {
                throw new CredentialPluginException("CredentialProvider class doesn't implement CredentialProvider interface: " + aClass);
            }

            final Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();

            this.provider = (CredentialProvider) declaredConstructor.newInstance();
            this.descriptor = PluginDescriptor.from(provider);
        } catch (ClassNotFoundException ex) {
            throw new CredentialPluginException("Resource doesn't have CredentialProvider class", ex);
        } catch (NoSuchMethodException ex) {
            throw new CredentialPluginException("CredentialProvider class doesn't have default constructor", ex);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new CredentialPluginException("CredentialProvider object can't be created", ex);
        }
    }

    public String getId() {
        return descriptor.id();
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public void close() throws IOException {
        provider = null;

        classLoader.close();
        classLoader = null;
    }

    Path getResource() {
        return resource;
    }

    CredentialProvider getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return "CredentialPlugin{" + descriptor + '}';
    }
}