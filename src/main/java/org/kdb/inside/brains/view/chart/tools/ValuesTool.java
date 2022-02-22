package org.kdb.inside.brains.view.chart.tools;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import kx.c;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.export.ExportDataProvider;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.sql.Timestamp;
import java.util.Vector;

public class ValuesTool implements ExportDataProvider, ChartMouseListener {
    private ChartPanel myPanel;
    private boolean enabled = false;

    private final JPanel component;
    private final JBTable pointsTable;

    public ValuesTool(Project project) {
        final DefaultTableCellRenderer cellRenderer = KdbOutputFormatter.getInstance().createCellRenderer(false);

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

        final ActionGroup contextMenu = ExportDataProvider.createActionGroup(project, this);
        PopupHandler.installPopupHandler(pointsTable, contextMenu, "ChartValuesTool.Context");

        final ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("ChartValuesTool.Toolbar", contextMenu, false);
        actionToolbar.setTargetComponent(pointsTable);

        component = new JPanel(new BorderLayout());
        component.add(BorderLayout.WEST, actionToolbar.getComponent());
        component.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(pointsTable));
    }

    public void setChartPanel(ChartPanel panel) {
        if (myPanel != null) {
            myPanel.removeChartMouseListener(this);
        }

        myPanel = panel;

        if (myPanel != null) {
            myPanel.addChartMouseListener(this);
            pointsTable.setModel(createTableModel(panel));
            initializeColumnModel();
        } else {
            pointsTable.setModel(new DefaultTableModel());
        }
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

    private DefaultTableModel createTableModel(ChartPanel panel) {
        final JFreeChart chart = panel.getChart();

        final XYPlot plot = chart.getXYPlot();
        final ValueAxis domainAxis = plot.getDomainAxis();
        final Vector<String> columns = new Vector<>();
        columns.add("Key");
        columns.add(domainAxis.getLabel());

        final int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            final ValueAxis rangeAxis = plot.getRangeAxis(i);
            columns.add(rangeAxis.getLabel());
        }

        final int dsCount = plot.getDatasetCount();
        for (int i = 0; i < dsCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            final int seriesCount = dataset.getSeriesCount();
            for (int j = 0; j < seriesCount; j++) {
                columns.add(String.valueOf(dataset.getSeriesKey(j)));
            }
        }
        return new DefaultTableModel(columns, 0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JComponent getComponent() {
        return component;
    }

    @Override
    public void chartMouseMoved(ChartMouseEvent event) {
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent event) {
        if (!enabled) {
            return;
        }

        final JFreeChart chart = event.getChart();

        final XYPlot plot = (XYPlot) chart.getPlot();
        final ValueAxis domain = plot.getDomainAxis();
        final MouseEvent trigger = event.getTrigger();
        final Rectangle2D dataArea = myPanel.getScreenDataArea();

        final double x = domain.java2DToValue(trigger.getX(), dataArea, RectangleEdge.BOTTOM);
        final Object val = domain instanceof DateAxis ? new Timestamp((long) x) : Double.valueOf(x);

        final Vector<Object> values = new Vector<>();
        values.add(getKeyValue(trigger));
        values.add(val);

        final int rangeCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeCount; i++) {
            values.add(plot.getRangeAxis(i).java2DToValue(trigger.getY(), dataArea, RectangleEdge.LEFT));
        }

        final int dsCount = plot.getDatasetCount();
        for (int i = 0; i < dsCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            final int seriesCount = dataset.getSeriesCount();
            for (int s = 0; s < seriesCount; s++) {
                values.add(DatasetUtils.findYValue(dataset, s, x));
            }
        }
        ((DefaultTableModel) pointsTable.getModel()).insertRow(0, values);
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

    private Object createColumn(TableModel model, int col) {
        if (col == 0) {
            return createStringCol(model, col);
        } else if (col == 1) {
            final XYPlot plot = myPanel.getChart().getXYPlot();
            final ValueAxis domainAxis = plot.getDomainAxis();
            if (domainAxis instanceof DateAxis) {
                return createTimestampsCol(model, col);
            }
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
}
