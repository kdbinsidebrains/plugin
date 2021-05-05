package org.kdb.inside.brains.core;

public interface KdbScopeListener {
    default void itemUpdated(KdbScope scope, InstanceItem item) {
    }

    default void itemCreated(KdbScope scope, StructuralItem parent, InstanceItem item, int index) {
    }

    default void itemRemoved(KdbScope scope, StructuralItem parent, InstanceItem item, int index) {
    }
}