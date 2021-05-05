package org.kdb.inside.brains.core;

import java.util.List;

public interface KdbScopesListener {
    void scopeCreated(KdbScope scope);

    void scopeRemoved(KdbScope scope);

    void scopeUpdated(KdbScope scope);

    void scopesReordered(List<String> oldOrder, List<KdbScope> scopes);
}