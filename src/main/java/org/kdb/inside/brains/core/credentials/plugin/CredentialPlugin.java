package org.kdb.inside.brains.core.credentials.plugin;

import org.kdb.inside.brains.core.credentials.CredentialProvider;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public final class CredentialPlugin {
    private final URL resource;
    private final CredentialProvider provider;
    private final URLClassLoader classLoader;

    private CredentialPlugin(URL resource) throws IOException, CredentialPluginException {
        try (JarInputStream i = new JarInputStream(resource.openStream())) {
            final Manifest manifest = i.getManifest();
            if (manifest == null) {
                throw new CredentialPluginException("Resource doesn't have Manifest file");
            }

            final String credentialsProvider = manifest.getMainAttributes().getValue("CredentialsProvider");
            if (credentialsProvider == null) {
                throw new CredentialPluginException("Manifest doesn't have mandatory CredentialsProvider attribute");
            }

            classLoader = new URLClassLoader(new URL[]{resource}, CredentialProvider.class.getClassLoader());
            final Class<?> aClass = classLoader.loadClass(credentialsProvider);
            if (!(CredentialProvider.class.isAssignableFrom(aClass))) {
                throw new CredentialPluginException("CredentialProvider class doesn't implement CredentialProvider interface: " + aClass);
            }
            final Constructor<?> declaredConstructor = aClass.getDeclaredConstructor();


            this.resource = resource;
            this.provider = (CredentialProvider) declaredConstructor.newInstance();
        } catch (ClassNotFoundException ex) {
            throw new CredentialPluginException("Resource doesn't have CredentialProvider class", ex);
        } catch (NoSuchMethodException ex) {
            throw new CredentialPluginException("CredentialProvider class doesn't have default constructor", ex);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new CredentialPluginException("CredentialProvider object can't be created", ex);
        }
    }

    public String getName() {
        return provider.getName();
    }

    public String getVersion() {
        return provider.getVersion();
    }

    public URL getResource() {
        return resource;
    }

    public CredentialProvider getProvider() {
        return provider;
    }

    public void destroy() throws IOException {
        classLoader.close();
    }

    public static void test(URL url) throws IOException, CredentialPluginException {
        load(url);
    }

    public static CredentialPlugin load(URL url) throws IOException, CredentialPluginException {
        return new CredentialPlugin(url);
    }
}