package org.kdb.inside.brains.view.chart.tools.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.chart.ChartColumn;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ChartView;
import org.kdb.inside.brains.view.chart.SnapType;
import org.kdb.inside.brains.view.chart.tools.AbstractChartTool;
import org.kdb.inside.brains.view.chart.tools.DataChartTool;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ValuesTool extends AbstractChartTool implements DataChartTool, ExportDataProvider {
    private KdbType domainType;

    private SnapType snapType;

    private final JPanel component;
    private final JBTable pointsTable;
    private final KdbOutputFormatter formatter;

    public static final String ID = "VALUES";

    public ValuesTool(Project project) {
        super(ID, "Points Collector", "Writes each click into a table", KdbIcons.Chart.ToolPoints);

        formatter = KdbOutputFormatter.getDefault();

        final DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setText(value instanceof String s ? s : formatter.objectToString(value));
            }
        };

        pointsTable = new JBTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                return cellRenderer;
            }
        };

        pointsTable.setShowColumns(true);
        pointsTable.setColumnSelectionAllowed(true);

        final DefaultActionGroup contextMenu = new DefaultActionGroup();
        contextMenu.addAll(ExportDataProvider.createActionGroup(project, this));
        contextMenu.addSeparator();
        contextMenu.add(new EdtAction("Clear All", "Clear all stored point", AllIcons.Actions.GC) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!pointsTable.isEmpty());
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final DefaultTableModel model = (DefaultTableModel) pointsTable.getModel();
                model.getDataVector().clear();
                model.fireTableDataChanged();
            }
        });
        PopupHandler.installPopupMenu(pointsTable, contextMenu, "ChartValuesTool.Context");

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ChartValuesTool.Toolbar", contextMenu, false);
        actionToolbar.setTargetComponent(pointsTable);

        component = new JPanel(new BorderLayout());
        component.add(BorderLayout.WEST, actionToolbar.getComponent());
        component.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(pointsTable));
    }

    @Override
    public void chartChanged(ChartView view, SnapType snapType) {
        this.snapType = snapType;
        if (view != null) {
            domainType = view.config().getDomainType();

            final DefaultTableModel tableModel = createTableModel(view.chart());

            final List<String> curNames = getColumnNames(pointsTable.getModel());
            final List<String> newNames = getColumnNames(tableModel);
            if (!curNames.equals(newNames)) {
                pointsTable.setModel(tableModel);
                initializeColumnModel();
            }
        } else {
            domainType = null;
            pointsTable.setModel(new DefaultTableModel());
        }
    }

    private List<String> getColumnNames(TableModel model) {
        final int columnCount = model.getColumnCount();
        final List<String> res = new ArrayList<>(columnCount);
        for (int i = 0; i < columnCount; i++) {
            res.add(model.getColumnName(i));
        }
        return res;
    }

    private void initializeColumnModel() {
        final TableColumnModel columnModel = pointsTable.getColumnModel();

        final TableColumn key = columnModel.getColumn(0);
        key.setMaxWidth(50);
        key.setPreferredWidth(50);

        final TableColumn domain = columnModel.getColumn(1);
        domain.setMaxWidth(300);
        domain.setPreferredWidth(200);
    }

    private DefaultTableModel createTableModel(JFreeChart chart) {
        final XYPlot plot = chart.getXYPlot();
        final ValueAxis domainAxis = plot.getDomainAxis();
        final Vector<String> columns = new Vector<>();
        columns.add("Key");
        columns.add(domainAxis.getLabel());

        final int dsCount = plot.getDatasetCount();
        for (int i = 0; i < dsCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            if (dataset instanceof OHLCDataset) {
                columns.add("Open");
                columns.add("High");
                columns.add("Low");
                columns.add("Close");
                columns.add("Volume");
            } else {
                final int seriesCount = dataset.getSeriesCount();
                for (int j = 0; j < seriesCount; j++) {
                    columns.add(String.valueOf(dataset.getSeriesKey(j)));
                }
            }
        }
        return new DefaultTableModel(columns, 0);
    }

    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event, Rectangle2D dataArea) {
        if (domainType == null) {
            return;
        }

        final JFreeChart chart = event.getChart();

        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis domain = plot.getDomainAxis();
        final MouseEvent trigger = event.getTrigger();

        final double x = calculateDomainPoint(event, dataArea, snapType == SnapType.VERTEX);
        if (Double.isNaN(x)) {
            return;
        }

        final Vector<Object> values = new Vector<>();
        values.add(getKeyValue(trigger));
        values.add(getDomainValue(domain, x));

        final int dsCount = plot.getDatasetCount();
        for (int i = 0; i < dsCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            if (dataset instanceof OHLCDataset ds) {
                final double[] doubles = calculateOHLCValues(ds, 0, x);
                if (doubles == null) {
                    return;
                }
                for (double d : doubles) {
                    values.add(d);
                }
            } else {
                final int seriesCount = dataset.getSeriesCount();
                for (int s = 0; s < seriesCount; s++) {
                    values.add(DatasetUtils.findYValue(dataset, s, x));
                }
            }
        }
        ((DefaultTableModel) pointsTable.getModel()).insertRow(0, values);
    }

    private @NotNull Object getDomainValue(ValueAxis axis, double x) {
        final Object val;
        if (axis instanceof DateAxis) {
            final long v = new Timestamp((long) x)
                    .toInstant()
                    .atZone(ZoneOffset.systemDefault())
                    .toLocalDateTime()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli();
            val = ChartDataProvider.createKdbTemporal(v, domainType);
        } else {
            val = x;
        }
        return val;
    }

    private String getKeyValue(MouseEvent event) {
        final StringBuilder buf = new StringBuilder();
/*
JVM issue:
        if (event.isMetaDown()) {
            buf.append('M');
        }
        if (event.isAltDown()) {
            buf.append('A');
        }
*/
        if (event.isControlDown()) {
            buf.append('C');
        }
        if (event.isShiftDown()) {
            buf.append('S');
        }
        if (event.isAltGraphDown()) {
            buf.append('G');
        }

        if (SwingUtilities.isLeftMouseButton(event)) {
            buf.append("L");
        } else if (SwingUtilities.isMiddleMouseButton(event)) {
            buf.append("M");
        } else if (SwingUtilities.isRightMouseButton(event)) {
            buf.append("R");
        }

        return buf.toString();
    }

    @Override
    public JTable getTable() {
        return pointsTable;
    }

    @Override
    public String getExportName() {
        return "Chart Values";
    }

    @Override
    public Object getNativeObject() {
        final TableModel model = pointsTable.getModel();
        final int columnCount = model.getColumnCount();
        final String[] names = new String[columnCount];
        for (int i = 0; i < names.length; i++) {
            names[i] = model.getColumnName(i);
        }

        final Object[] values = new Object[columnCount];
        for (int i = 0; i < values.length; i++) {
            values[i] = createColumn(model, i);
        }
        return new c.Flip(new c.Dict(names, values));
    }

    @Override
    public KdbOutputFormatter getOutputFormatter() {
        return formatter;
    }

    private Object createColumn(TableModel model, int col) {
        if (col == 0) {
            return createStringCol(model, col);
        } else if (col == 1 && ChartColumn.isTemporal(domainType)) {
            return createTimestampsCol(model, col);
        }
        return createDoubleCol(model, col);
    }

    private String[] createStringCol(TableModel model, int col) {
        final int rowCount = model.getRowCount();
        final String[] res = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            res[i] = (String) model.getValueAt(i, col);
        }
        return res;
    }

    private double[] createDoubleCol(TableModel model, int col) {
        final int rowCount = model.getRowCount();
        final double[] res = new double[rowCount];
        for (int i = 0; i < rowCount; i++) {
            res[i] = (double) model.getValueAt(i, col);
        }
        return res;
    }

    private Timestamp[] createTimestampsCol(TableModel model, int col) {
        final int rowCount = model.getRowCount();
        final Timestamp[] res = new Timestamp[rowCount];
        for (int i = 0; i < rowCount; i++) {
            res[i] = (Timestamp) model.getValueAt(i, col);
        }
        return res;
    }

    @Override
    public void paintOverlay(Graphics2D g2, ChartPanel chartPanel) {
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (ExportDataProvider.DATA_KEY.is(dataId)) {
            return this;
        }
        return null;
    }
}
