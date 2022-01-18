package org.kdb.inside.brains.view.console.chart.ohlc;

import org.kdb.inside.brains.view.console.chart.ColumnConfig;

public class ChartConfig {
    private final ColumnConfig dateColumn;
    private final ColumnConfig openColumn;
    private final ColumnConfig highColumn;
    private final ColumnConfig lowColumn;
    private final ColumnConfig closeColumn;
    private final ColumnConfig volumeColumn;

    public ChartConfig(ColumnConfig dateColumn, ColumnConfig openColumn, ColumnConfig highColumn, ColumnConfig lowColumn, ColumnConfig closeColumn, ColumnConfig volumeColumn) {
        this.dateColumn = dateColumn;
        this.openColumn = openColumn;
        this.highColumn = highColumn;
        this.lowColumn = lowColumn;
        this.closeColumn = closeColumn;
        this.volumeColumn = volumeColumn;
    }

    public ColumnConfig getDateColumn() {
        return dateColumn;
    }

    public ColumnConfig getOpenColumn() {
        return openColumn;
    }

    public ColumnConfig getHighColumn() {
        return highColumn;
    }

    public ColumnConfig getLowColumn() {
        return lowColumn;
    }

    public ColumnConfig getCloseColumn() {
        return closeColumn;
    }

    public ColumnConfig getVolumeColumn() {
        return volumeColumn;
    }

    public boolean isEmpty() {
        return dateColumn == null || openColumn == null || highColumn == null || lowColumn == null || closeColumn == null;
    }
}
