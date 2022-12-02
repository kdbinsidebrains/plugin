package org.kdb.inside.brains.lang.binding;

public enum EditorsBindingStrategy {
    MANUAL("Manual binding", "You can bind a connection to an editor manually by 'bind' toolbar action."),
    TAB_TO_CONNECT("Bind a tab to the connection", "Each activated editor is binding to active connection and the connection will be selected each time when the editor is activated."),
    CONNECT_TO_TAB("Bind a connection to the tab", "When active connection is changed, new connection is binding to current editor and will be auto-selected each time when the tab is activated. No binding is happened if active connection is not changed.");

    private final String name;
    private final String description;

    EditorsBindingStrategy(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }
}
