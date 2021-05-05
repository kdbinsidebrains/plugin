package org.kdb.inside.brains.settings;

import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NotNull;

public abstract class KdbConfigurable implements SearchableConfigurable {
    private final String id;
    private final String name;

    protected static final int FORM_LEFT_INDENT = 15;

    protected KdbConfigurable(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return name;
    }
}
