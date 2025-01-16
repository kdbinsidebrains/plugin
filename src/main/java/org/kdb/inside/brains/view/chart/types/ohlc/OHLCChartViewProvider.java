package org.kdb.inside.brains.view.chart.types.ohlc;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.kdb.inside.brains.view.chart.ChartColors;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ChartViewProvider;
import org.kdb.inside.brains.view.chart.ColumnDefinition;
import org.kdb.inside.brains.view.chart.types.ChartType;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Date;

public class OHLCChartViewProvider extends ChartViewProvider<JPanel, OHLCChartConfig> {
    private boolean ignoreUpdate = false;

    private final JBTable rangesComponent = new JBTable();
    private final ComboBox<ColumnDefinition> domainComponent = new ComboBox<>();

    public OHLCChartViewProvider(ChartDataProvider dataProvider) {
        super("Candlestick", ChartType.OHLC, dataProvider);
    }

    private static JFreeChart createChart(OHLCChartConfig config, ChartDataProvider dataProvider) {
        final OHLCDataset dataset = createDataset(config, dataProvider);

        final JFreeChart chart = ChartFactory.createCandlestickChart(null, config.domain().name(), "", dataset, false);

        final MyCandlestickRenderer renderer = new MyCandlestickRenderer();
        renderer.setUpPaint(ChartColors.POSITIVE);
        renderer.setDownPaint(ChartColors.NEGATIVE);

        final XYPlot plot = chart.getXYPlot();
        plot.setRenderer(renderer);

        return chart;
    }

    @NotNull
    private static OHLCDataset createDataset(OHLCChartConfig config, ChartDataProvider dataProvider) {
        final Date[] dates = dataProvider.getDates(config.domain());
        final double[] high = dataProvider.getDoubles(config.highColumn());
        final double[] low = dataProvider.getDoubles(config.lowColumn());
        final double[] open = dataProvider.getDoubles(config.openColumn());
        final double[] close = dataProvider.getDoubles(config.closeColumn());

        final ColumnDefinition volumeColumn = config.volumeColumn();
        final double[] volume = volumeColumn == null ? new double[dataProvider.getRowsCount()] : dataProvider.getDoubles(volumeColumn);

        return new DefaultHighLowDataset("", dates, high, low, open, close, volume);
    }

    @Override
    protected JPanel createConfigPanel(ChartDataProvider provider) {
        initComponents();

        final FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Domain axis: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(domainComponent);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Series definition: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(ScrollPaneFactory.createScrollPane(rangesComponent));

        final JPanel p = new JPanel(new BorderLayout());
        p.add(formBuilder.getPanel(), BorderLayout.PAGE_START);
        return p;
    }

    private ColumnDefinition getSelectedConfig(int column) {
        final int rowCount = rangesComponent.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final Object valueAt = rangesComponent.getValueAt(row, column);
            if (Boolean.TRUE.equals(valueAt)) {
                return (ColumnDefinition) rangesComponent.getValueAt(row, 0);
            }
        }
        return null;
    }

    private void setSelectedConfig(int column, ColumnDefinition config) {
        final int rowCount = rangesComponent.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final ColumnDefinition cc = (ColumnDefinition) rangesComponent.getValueAt(row, 0);
            rangesComponent.setValueAt(cc.equals(config), row, column);
        }
    }

    private void initComponents() {
        final DefaultTableModel model = new DefaultTableModel(new Object[]{"Column", "Open", "High", "Low", "Close", "Volume"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ColumnDefinition.class;
                }
                return Boolean.class;
            }
        };

        final ColumnDefinition[] columns = dataProvider.getColumns();
        for (final ColumnDefinition cc : columns) {
            if (cc.isTemporal()) {
                domainComponent.addItem(cc);
            } else if (cc.isNumber()) {
                model.addRow(new Object[]{cc, false, false, false, false, false});
            }
        }

        domainComponent.setRenderer(ColumnDefinition.createListCellRenderer());
        domainComponent.addActionListener(e -> {
            if (ignoreUpdate) {
                return;
            }
            processConfigChanged();
        });

        rangesComponent.setModel(model);
        rangesComponent.setVisibleRowCount(model.getRowCount());
        rangesComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rangesComponent.putClientProperty(JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        final TableColumnModel columnModel = rangesComponent.getColumnModel();
        final TableColumn col = columnModel.getColumn(0);
        col.setResizable(false);
        col.setCellRenderer(ColumnDefinition.createTableCellRenderer());

        final int width = 5 + rangesComponent.getIntercellSpacing().width;
        final TableCellRenderer headerRenderer = rangesComponent.getTableHeader().getDefaultRenderer();
        for (int i = 1; i < 6; i++) {
            final TableColumn column = columnModel.getColumn(i);
            column.setResizable(false);

            final Component c = headerRenderer.getTableCellRendererComponent(null, column.getHeaderValue(), false, false, 0, 0);
            column.setWidth(column.getPreferredWidth() + width);
            column.setMinWidth(c.getMinimumSize().width + width);
            column.setMaxWidth(c.getMaximumSize().width + width);
            column.setPreferredWidth(c.getPreferredSize().width + width);
        }

        model.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (ignoreUpdate) {
                    return;
                }

                final int column = e.getColumn();
                if (column != 0) {
                    final int row = e.getFirstRow();

                    model.removeTableModelListener(this);
                    for (int c = 1; c < rangesComponent.getColumnCount(); c++) {
                        if (c == column) {
                            continue;
                        }
                        model.setValueAt(false, row, c);
                    }

                    for (int r = 0; r < rangesComponent.getRowCount(); r++) {
                        if (r == row) {
                            continue;
                        }
                        model.setValueAt(false, r, column);
                    }

                    model.addTableModelListener(this);
                }
                processConfigChanged();
            }
        });
    }

    @Override
    public OHLCChartConfig createChartConfig() {
        final ColumnDefinition domain = domainComponent.getItem();

        final ColumnDefinition open = getSelectedConfig(1);
        final ColumnDefinition high = getSelectedConfig(2);
        final ColumnDefinition low = getSelectedConfig(3);
        final ColumnDefinition close = getSelectedConfig(4);
        final ColumnDefinition volume = getSelectedConfig(5);
        return new OHLCChartConfig(domain, open, high, low, close, volume);
    }

    @Override
    public void updateChartConfig(OHLCChartConfig config) {
        ignoreUpdate = true;
        try {
            domainComponent.setSelectedItem(config.domain());

            setSelectedConfig(1, config.openColumn());
            setSelectedConfig(2, config.highColumn());
            setSelectedConfig(3, config.lowColumn());
            setSelectedConfig(4, config.closeColumn());
            setSelectedConfig(5, config.volumeColumn());
        } finally {
            ignoreUpdate = false;
        }
        processConfigChanged();
    }

    @Override
    public JFreeChart getJFreeChart(OHLCChartConfig config) {
        return config == null || config.isInvalid() ? null : createChart(config, dataProvider);
    }

    private static class MyCandlestickRenderer extends CandlestickRenderer {
        @Override
        public Paint getItemPaint(int series, int item) {
            final OHLCDataset highLowData = (OHLCDataset) getPlot().getDataset();
            final Number yOpen = highLowData.getOpen(series, item);
            final Number yClose = highLowData.getClose(series, item);
            final boolean isUpCandle = yClose.doubleValue() > yOpen.doubleValue();
            if (isUpCandle) {
                return getUpPaint();
            } else {
                return getDownPaint();
            }
        }
    }
}
