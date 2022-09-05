package org.kdb.inside.brains.core;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@State(name = "KdbScopeHolder", storages = {@Storage("kdb-instances.xml")})
public class KdbScopeHolder implements PersistentStateComponent<Element>, DumbAware {
    private final ScopeType type;

    private final KdbScopeHelper dom = new KdbScopeHelper();

    private final List<KdbScope> scopes = new ArrayList<>();

//    private final Set<String> credentialsId = new HashSet<>();

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
        return dom.writeScopes(scopes, true);
/*

        // And remove all old
        oldCredIds.removeAll(credentialsId);
        for (String name : oldCredIds) {
            writeCredentials(name, null);
        }
*/
    }

    @Override
    public void loadState(@NotNull Element state) {
        scopes.clear();
//        credentialsId.clear();

        scopes.addAll(dom.readScopes(state, type));
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
