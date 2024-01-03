package org.kdb.inside.brains.core;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KdbScope extends StructuralItem implements CredentialsItem {
    private ScopeType type;
    private String credentials;
    private InstanceOptions options;

    private final List<KdbScopeListener> listeners = new ArrayList<>();

    public KdbScope(@NotNull String name, @NotNull ScopeType type) {
        this(name, type, InstanceOptions.INHERITED, null);
    }

    public KdbScope(@NotNull String name, @NotNull ScopeType type, @NotNull InstanceOptions options, String credentials) {
        super(name);
        this.type = Objects.requireNonNull(type);
        this.credentials = credentials;
        this.options = options;
    }

    public void addScopeListener(KdbScopeListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    public void removeScopeListener(KdbScopeListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    public ScopeType getType() {
        return type;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    public InstanceOptions getOptions() {
        return options;
    }

    public void update(KdbScope sourceScope) {
        update(sourceScope.getName(), sourceScope.type, sourceScope.credentials, sourceScope.options);
    }

    public void update(String name, ScopeType type, String credentials, InstanceOptions options) {
        setName(name);
        this.type = type;
        this.credentials = credentials;
        this.options = options;

        notifyItemUpdated();
    }

    void processItemUpdated(InstanceItem item) {
        listeners.forEach(l -> l.itemUpdated(this, item));
    }

    void processItemAdded(StructuralItem parent, InstanceItem child, int index) {
        listeners.forEach(l -> l.itemCreated(this, parent, child, index));
    }

    void processItemRemoved(StructuralItem parent, InstanceItem child, int index) {
        listeners.forEach(l -> l.itemRemoved(this, parent, child, index));
    }

    @Override
    public KdbScope copy() {
        final KdbScope s = new KdbScope(getName(), type, options, credentials);
        for (InstanceItem child : children) {
            s.copyItem(child);
        }
        return s;
    }
}
