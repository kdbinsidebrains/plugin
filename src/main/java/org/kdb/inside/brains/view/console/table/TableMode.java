package org.kdb.inside.brains.view.console.table;

public enum TableMode {
    NORMAL(true),
    COMPACT(false);

    private final boolean showStatusBar;

    TableMode(boolean showStatusBar) {
        this.showStatusBar = showStatusBar;
    }

    public boolean isShowStatusBar() {
        return showStatusBar;
    }
}
