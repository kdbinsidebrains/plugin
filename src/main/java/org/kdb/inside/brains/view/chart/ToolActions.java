package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.AnAction;

public record ToolActions(String name, AnAction... actions) {
    public static final ToolActions NO_ACTIONS = new ToolActions(null);

    public boolean hasActions() {
        return actions.length != 0;
    }
}
