package org.kdb.inside.brains.view.chart.ohlc;

import kx.c;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.jfree.data.xy.OHLCDataset;
import org.kdb.inside.brains.view.chart.BaseChartPanel;
import org.kdb.inside.brains.view.chart.ChartColors;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ColumnConfig;

import java.awt.*;
import java.util.Date;

public class OHLCCharPanel extends BaseChartPanel {
    public OHLCCharPanel(ChartConfig config, ChartDataProvider dataProvider) {
        super(createChart(config, dataProvider));
    }

    private static JFreeChart createChart(ChartConfig config, ChartDataProvider dataProvider) {
        final OHLCDataset dataset = createDataset(config, dataProvider);

        final JFreeChart chart = ChartFactory.createCandlestickChart(null, config.getDateColumn().getName(), "", dataset, false);

        final MyCandlestickRenderer renderer = new MyCandlestickRenderer();
        renderer.setUpPaint(ChartColors.POSITIVE);
        renderer.setDownPaint(ChartColors.NEGATIVE);

        final XYPlot plot = chart.getXYPlot();
        plot.setRenderer(renderer);

        final NumberAxis axis = (NumberAxis) applyAxisColorSchema(plot.getRangeAxis());
        axis.setAutoRangeIncludesZero(false);

        return chart;
    }

    @NotNull
    private static OHLCDataset createDataset(ChartConfig config, ChartDataProvider dataProvider) {
        final int rowCount = dataProvider.getRowCount();
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
