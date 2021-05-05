package org.kdb.inside.brains.core;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@State(name = "KdbScopesManager", storages = {@Storage(StoragePathMacros.PRODUCT_WORKSPACE_FILE)})
public class KdbScopesManager implements PersistentStateComponent<KdbScopesManager.State> {
    private final KdbScopeHolder localScopeHolder;
    private final KdbScopeHolder sharedScopeHolder;

    private final List<KdbScope> scopes = new ArrayList<>();

    private final TheScopeListener scopeListener = new TheScopeListener();
    private final List<KdbScopesListener> listeners = new CopyOnWriteArrayList<>();

    private State state = new State();

    public KdbScopesManager(Project project) {
        this.localScopeHolder = ServiceManager.getService(project, KdbScopeHolder.class);
        this.sharedScopeHolder = ServiceManager.getService(KdbScopeHolder.class);
        restoreManager();
    }

    public void addScopesListener(KdbScopesListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeScopesListener(KdbScopesListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    public void addScope(KdbScope scope) {
        if (scopes.contains(scope)) {
            throw new IllegalStateException("The scope already associated with a manager");
        }

        if (scope.getType() == ScopeType.LOCAL) {
            localScopeHolder.addScope(scope);
        } else {
            sharedScopeHolder.addScope(scope);
        }
        scope.addScopeListener(scopeListener);
        scopes.add(scope);

        listeners.forEach(l -> l.scopeCreated(scope));
    }

    public void removeScope(KdbScope scope) {
        if (!scopes.remove(scope)) {
            return;
        }
        scope.removeScopeListener(scopeListener);

        localScopeHolder.removeScope(scope);
        sharedScopeHolder.removeScope(scope);

        listeners.forEach(l -> l.scopeRemoved(scope));
    }

    public boolean containsScope(KdbScope scope) {
        return scopes.contains(scope);
    }

    public List<KdbScope> getScopes() {
        return Collections.unmodifiableList(scopes);
    }

    public void reorderScopes(List<String> orderedNames) {
        if (state.orderedNames.equals(orderedNames)) {
            return;
        }

        final List<String> oldSort = new ArrayList<>(state.orderedNames);

        state.orderedNames.clear();
        state.orderedNames.addAll(orderedNames);

        sortScopes();
        listeners.forEach(l -> l.scopesReordered(oldSort, getScopes()));
    }

    private void restoreManager() {
        scopes.addAll(localScopeHolder.getScopes());
        scopes.addAll(sharedScopeHolder.getScopes());

        for (KdbScope scope : scopes) {
            scope.addScopeListener(scopeListener);
        }

        sortScopes();
    }

    private void sortScopes() {
        scopes.sort((s1, s2) -> {
            final int idx1 = state.orderedNames.indexOf(s1.getName());
            final int idx2 = state.orderedNames.indexOf(s2.getName());
            return idx1 - idx2;
        });
    }

    @Nullable
    @Override
    public State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull KdbScopesManager.State state) {
        this.state = state;
        sortScopes();
    }

    public static KdbScopesManager getManager(Project project) {
        return project.getService(KdbScopesManager.class);
    }

    private class TheScopeListener implements KdbScopeListener {
        @Override
        public void itemUpdated(KdbScope scope, InstanceItem item) {
            if (!(item instanceof KdbScope)) {
                return;
            }

            final ScopeType type = ((KdbScope) item).getType();
            if (type == ScopeType.LOCAL) {
                if (!localScopeHolder.containsScope(scope)) {
                    sharedScopeHolder.removeScope(scope);
                    localScopeHolder.addScope(scope);
                }
            } else {
                if (!sharedScopeHolder.containsScope(scope)) {
                    localScopeHolder.removeScope(scope);
                    sharedScopeHolder.addScope(scope);
                }
            }
            listeners.forEach(l -> l.scopeUpdated((KdbScope) item));
        }

        @Override
        public void itemCreated(KdbScope scope, StructuralItem parent, InstanceItem item, int index) {

        }

        @Override
        public void itemRemoved(KdbScope scope, StructuralItem parent, InstanceItem item, int index) {

        }
    }

    public static class State {
        @XCollection(propertyElementName = "order", elementName = "scope", valueAttributeName = "name")
        private final List<String> orderedNames = new ArrayList<>();
    }
}