package org.kdb.inside.brains.core;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.UIUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@State(name = "KdbScopeHolder", storages = {@Storage("kdb-instances.xml")})
public class KdbScopeHolder implements PersistentStateComponent<Element> {
    private final ScopeType type;

    private final List<KdbScope> scopes = new ArrayList<>();

//    private final Set<String> credentialsId = new HashSet<>();

    private static final String ROOT_ELEMENT_NAME = "scopes";
    private static final String PASSWORD_SAFE_SERVICE_NAME = "KdbInsideBrains";

    private static final Logger log = Logger.getInstance(KdbScopeHolder.class);

    KdbScopeHolder() {
        this.type = ScopeType.SHARED;
        log.info("Creating new global holder");
    }

    KdbScopeHolder(Project project) {
        this.type = ScopeType.LOCAL;
        log.info("Creating new local holder for project: " + project.getName());
    }

    List<KdbScope> getScopes() {
        return Collections.unmodifiableList(scopes);
    }

    void addScope(KdbScope scope) {
        scopes.add(scope);
    }

    void removeScope(KdbScope scope) {
        scopes.remove(scope);
    }

    boolean containsScope(KdbScope scope) {
        return scopes.contains(scope);
    }

    @NotNull
    @Override
    public Element getState() {
/*
        // get copy
        final Set<String> oldCredIds = new HashSet<>(credentialsId);
        // clear
        credentialsId.clear();

*/
        // encode
        final Element element = new Element(ROOT_ELEMENT_NAME);
        scopes.forEach(s -> element.addContent(writeScope(s)));
/*

        // And remove all old
        oldCredIds.removeAll(credentialsId);
        for (String name : oldCredIds) {
            writeCredentials(name, null);
        }
*/
        return element;
    }

    @NotNull
    private Element writeScope(KdbScope scope) {
        final String name = scope.getName();

        final Element scopeEl = new Element("scope");
        scopeEl.setAttribute("name", name);
        writeColor(scope, scopeEl);
        writeCredentials(scope, scopeEl);
        encodeInstanceOptions(scopeEl, scope.getOptions());
        writeChildren(scope, scopeEl, name);

//        writeCredentials(name, scope.getCredentials());

        return scopeEl;
    }

    private void writeChildren(StructuralItem si, Element parent, String parentId) {
        int i = 0;
        for (InstanceItem item : si) {
            final String name = item.getName();
            final String id = generateNextId(parentId, name, i);

            if (item instanceof PackageItem) {
                final PackageItem packageItem = (PackageItem) item;
                final Element packEl = new Element("package");
                packEl.setAttribute("name", name);
                writeColor(item, packEl);
                parent.addContent(packEl);

                writeChildren(packageItem, packEl, id);
            } else if (item instanceof KdbInstance) {
                final KdbInstance instance = (KdbInstance) item;

                final Element instEl = new Element("instance");
                instEl.setAttribute("name", name);
                instEl.setAttribute("host", instance.getHost());
                instEl.setAttribute("port", String.valueOf(instance.getPort()));
                writeColor(instance, instEl);
                writeCredentials(instance, instEl);
                encodeInstanceOptions(instEl, instance.getOptions());

//                writeCredentials(id, instance.getCredentials());

                parent.addContent(instEl);
            }
            i++;
        }
    }

    private void writeColor(InstanceItem item, Element el) {
        if (item.getColor() == null) {
            return;
        }
        el.setAttribute("color", UIUtils.encodeColor(item.getColor()));
    }

    @Override
    public void loadState(@NotNull Element state) {
        scopes.clear();
//        credentialsId.clear();

        state.getChildren().stream().map(this::readScope).forEach(this.scopes::add);
    }

    @NotNull
    private KdbScope readScope(@NotNull Element el) {
        final String name = el.getAttributeValue("name");
        final String credentials = readCredentials(el);
        final InstanceOptions options = decodeInstanceOptions(el);

        final KdbScope scope = new KdbScope(name, type, credentials, options);
        readColor(el, scope);
        readChildren(scope, el, name);

        return scope;
    }

    private void readChildren(StructuralItem item, @NotNull Element el, String parentId) {
        int i = 0;
        for (Element chEl : el.getChildren()) {
            final String name = chEl.getAttributeValue("name");
            final String id = generateNextId(parentId, name, i);

            final String elName = chEl.getName();
            if ("package".equalsIgnoreCase(elName)) {
                final PackageItem pkg = item.createPackage(name);
                readColor(chEl, pkg);
                readChildren(pkg, chEl, id);
            } else if ("instance".equalsIgnoreCase(elName)) {
                final String host = chEl.getAttributeValue("host");
                final int port = Integer.parseInt(chEl.getAttributeValue("port"));
                final InstanceOptions options = decodeInstanceOptions(chEl);
                final String credentials = readCredentials(chEl);

                final KdbInstance instance = item.createInstance(name, host, port, credentials, options);
                readColor(chEl, instance);
            }
            i++;
        }
    }

    private void readColor(Element el, InstanceItem item) {
        final String color = el.getAttributeValue("color");
        if (color == null) {
            return;
        }
        item.setColor(UIUtils.decodeColor(color));
    }

    private InstanceOptions decodeInstanceOptions(@NotNull Element el) {
        final String timeout = el.getAttributeValue("timeout");
        final String tls = el.getAttributeValue("tls");
        final String compression = el.getAttributeValue("compression");

        if (timeout == null && tls == null && compression == null) {
            return null;
        }

        final InstanceOptions o = new InstanceOptions();
        if (timeout != null) {
            o.setTimeout(Integer.parseInt(timeout));
        }
        if (tls != null) {
            o.setTls(Boolean.parseBoolean(tls));
        }
        if (compression != null) {
            o.setCompression(Boolean.parseBoolean(compression));
        }
        return o;
    }

    @NotNull
    private String generateNextId(String parentId, String name, int index) {
        return parentId + '/' + name + '[' + index + ']';
    }

    private void encodeInstanceOptions(@NotNull Element el, InstanceOptions options) {
        if (options == null) {
            return;
        }
        el.setAttribute("timeout", String.valueOf(options.getTimeout()));
        el.setAttribute("tls", String.valueOf(options.isTls()));
        el.setAttribute("compression", String.valueOf(options.isCompression()));
    }

    private void writeCredentials(CredentialsItem item, Element el) {
        final String credentials = item.getCredentials();
        if (credentials != null) {
            el.setAttribute("credentials", Base64.getEncoder().encodeToString(credentials.getBytes()));
        }
    }

    private String readCredentials(Element el) {
        final String credentials = el.getAttributeValue("credentials");
        if (credentials == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(credentials));
    }

/*
    private void writeCredentials(String id, String credential) {
        if (credential != null) {
            credentialsId.add(id);
            PasswordSafe.getInstance().setPassword(createAttribute(id), credential);
        }
    }

    private String readCredentials(String id) {
        final String pwd = PasswordSafe.getInstance().getPassword(createAttribute(id));
        if (pwd != null) {
            credentialsId.add(id);
            return pwd;
        }
        return null;
    }

    @NotNull
    static CredentialAttributes createAttribute(String id) {
        return new CredentialAttributes(CredentialAttributesKt.generateServiceName(PASSWORD_SAFE_SERVICE_NAME, "KdbScopeHolder"), id, KdbScopeHolder.class, false, true);
    }
*/
}
