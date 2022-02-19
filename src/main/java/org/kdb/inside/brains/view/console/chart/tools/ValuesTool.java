package org.kdb.inside.brains.view.console.chart.tools;

import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
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

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class ValuesTool implements ChartMouseListener {
    private ChartPanel myPanel;
    private boolean enabled = false;

    private final JBTable pointsTable = new JBTable() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JComponent pointsComponent = ScrollPaneFactory.createScrollPane(pointsTable);

    private final NumberFormat NUMBER_FORMATTER = new DecimalFormat("0.###");
    private final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd'T'HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    public ValuesTool() {
        pointsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

    public JComponent getPointsComponent() {
        return pointsComponent;
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
        final ValueToString domainConverter;
        if (domain instanceof DateAxis) {
            domainConverter = value -> TIME_FORMATTER.format(Instant.ofEpochMilli((long) value));
        } else {
            domainConverter = NUMBER_FORMATTER::format;
        }

        final MouseEvent trigger = event.getTrigger();
        final Rectangle2D dataArea = myPanel.getScreenDataArea();
        final double x = domain.java2DToValue(trigger.getX(), dataArea, RectangleEdge.BOTTOM);

        final Vector<String> values = new Vector<>();
        values.add(getKeyValue(trigger));
        values.add(domainConverter.toString(x));

        final int rangeCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeCount; i++) {
            final double v = plot.getRangeAxis(i).java2DToValue(trigger.getY(), dataArea, RectangleEdge.LEFT);
            values.add(NUMBER_FORMATTER.format(v));
        }

        final int dsCount = plot.getDatasetCount();
        for (int i = 0; i < dsCount; i++) {
            final XYDataset dataset = plot.getDataset(i);
            final int seriesCount = dataset.getSeriesCount();
            for (int s = 0; s < seriesCount; s++) {
                final double v = DatasetUtils.findYValue(dataset, s, x);
                values.add(NUMBER_FORMATTER.format(v));
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

    @FunctionalInterface
    private interface ValueToString {
        String toString(double value);
    }
}
