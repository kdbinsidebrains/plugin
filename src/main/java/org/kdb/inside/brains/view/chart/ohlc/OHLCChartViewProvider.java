package org.kdb.inside.brains.view.chart.ohlc;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import kx.c;
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
import org.kdb.inside.brains.view.chart.ColumnConfig;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.Date;

public class OHLCChartViewProvider extends ChartViewProvider<JPanel> {
    private final JBTable rangesComponent = new JBTable();
    private final ComboBox<ColumnConfig> domainComponent = new ComboBox<>();

    public OHLCChartViewProvider(ChartDataProvider dataProvider) {
        super("Candlestick", KdbIcons.Chart.Candlestick, dataProvider);
    }

    private static JFreeChart createChart(OHLCChartConfig config, ChartDataProvider dataProvider) {
        final OHLCDataset dataset = createDataset(config, dataProvider);

        final JFreeChart chart = ChartFactory.createCandlestickChart(null, config.getDateColumn().getName(), "", dataset, false);

        final MyCandlestickRenderer renderer = new MyCandlestickRenderer();
        renderer.setUpPaint(ChartColors.POSITIVE);
        renderer.setDownPaint(ChartColors.NEGATIVE);

        final XYPlot plot = chart.getXYPlot();
        plot.setRenderer(renderer);

        return chart;
    }

    @NotNull
    private static OHLCDataset createDataset(OHLCChartConfig config, ChartDataProvider dataProvider) {
        final int rowCount = dataProvider.getRowsCount();
        final Date[] dates = new Date[rowCount];
        final double[] high = new double[rowCount];
        final double[] low = new double[rowCount];
        final double[] open = new double[rowCount];
        final double[] close = new double[rowCount];
        final double[] volume = new double[rowCount];

        final int dateIndex = config.getDateColumn().getIndex();
        final int openIndex = config.getOpenColumn().getIndex();
        final int highIndex = config.getHighColumn().getIndex();
        final int lowIndex = config.getLowColumn().getIndex();
        final int closeIndex = config.getCloseColumn().getIndex();

        final ColumnConfig volumeColumn = config.getVolumeColumn();
        final int volumeIndex = volumeColumn == null ? -1 : config.getVolumeColumn().getIndex();

        for (int row = 0; row < rowCount; row++) {
            dates[row] = createDate(dataProvider.getValueAt(row, dateIndex));
            open[row] = ((Number) dataProvider.getValueAt(row, openIndex)).doubleValue();
            high[row] = ((Number) dataProvider.getValueAt(row, highIndex)).doubleValue();
            low[row] = ((Number) dataProvider.getValueAt(row, lowIndex)).doubleValue();
            close[row] = ((Number) dataProvider.getValueAt(row, closeIndex)).doubleValue();
            if (volumeColumn != null) {
                volume[row] = ((Number) dataProvider.getValueAt(row, volumeIndex)).doubleValue();
            }
        }
        return new DefaultHighLowDataset("", dates, high, low, open, close, volume);
    }

    private static Date createDate(Object value) {
        // SQL Date, Time, Timestamp are here
        if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof c.Second) {
            final c.Second v = (c.Second) value;
            return new Date(v.i * 1000L);
        } else if (value instanceof c.Minute) {
            final c.Minute v = (c.Minute) value;
            return new Date(v.i * 60 * 1000L);
        } else if (value instanceof c.Month) {
            final c.Month v = (c.Month) value;
            return new Date(v.i * 12 * 24 * 60 * 1000L);
        } else if (value instanceof c.Timespan) {
            final c.Timespan v = (c.Timespan) value;
            return new Date(v.j / 1_000_000L);
        }
        throw new IllegalArgumentException("Invalid value type: " + value.getClass());
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

    private ColumnConfig getColumnConfig(int column) {
        final int rowCount = rangesComponent.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            final Object valueAt = rangesComponent.getValueAt(row, column);
            if (Boolean.TRUE.equals(valueAt)) {
                return (ColumnConfig) rangesComponent.getValueAt(row, 0);
            }
        }
        return null;
    }

    private void initComponents() {
        final DefaultTableModel model = new DefaultTableModel(new Object[]{"Column", "Open", "High", "Low", "Close", "Volume"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ColumnConfig.class;
                }
                return Boolean.class;
            }
        };

        final ColumnConfig[] columns = dataProvider.getColumns();
        for (final ColumnConfig cc : columns) {
            if (cc.isTemporal()) {
                domainComponent.addItem(cc);
            } else if (cc.isNumber()) {
                model.addRow(new Object[]{cc, false, false, false, false, false});
            }
        }

        domainComponent.setRenderer(ColumnConfig.createListCellRenderer());
        domainComponent.addActionListener(e -> processConfigChanged());

        rangesComponent.setModel(model);
        rangesComponent.setVisibleRowCount(model.getRowCount());
        rangesComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UIUtil.putClientProperty(rangesComponent, JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        final TableColumnModel columnModel = rangesComponent.getColumnModel();
        final TableColumn col = columnModel.getColumn(0);
        col.setResizable(false);
        col.setCellRenderer(ColumnConfig.createTableCellRenderer());

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
    public JFreeChart getJFreeChart() {
        final OHLCChartConfig chartConfig = createChartConfig();
        return chartConfig.isEmpty() ? null : createChart(chartConfig, dataProvider);
    }

    private OHLCChartConfig createChartConfig() {
        final ColumnConfig open = getColumnConfig(1);
        final ColumnConfig high = getColumnConfig(2);
        final ColumnConfig low = getColumnConfig(3);
        final ColumnConfig close = getColumnConfig(4);
        final ColumnConfig volume = getColumnConfig(5);
        return new OHLCChartConfig(domainComponent.getItem(), open, high, low, close, volume);
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
