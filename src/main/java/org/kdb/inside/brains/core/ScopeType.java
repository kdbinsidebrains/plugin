package org.kdb.inside.brains.core;

import com.intellij.openapi.project.Project;
import icons.KdbIcons;

import javax.swing.*;

public enum ScopeType {
    LOCAL(KdbIcons.Scope.Local),
    SHARED(KdbIcons.Scope.Shared);

    private final Icon icon;

    ScopeType(Icon icon) {
        this.icon = icon;
    }

    public Icon getIcon() {
        return icon;
    }

    public static ScopeType getType(Project project) {
        return project == null ? SHARED : LOCAL;
    }
}
