package org.kdb.inside.brains.view.chart.ohlc;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.BaseChartPanel;
import org.kdb.inside.brains.view.chart.ChartBuilder;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ColumnConfig;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;

public class OHLCChartBuilder extends ChartBuilder {
    private final JBTable rangesComponent = new JBTable();
    private final ComboBox<ColumnConfig> domainComponent = new ComboBox<>();

    public OHLCChartBuilder(ChartDataProvider dataProvider) {
        super("Candlestick", KdbIcons.Chart.Candlestick, dataProvider);
    }

    @Override
    protected JPanel createConfigPanel() {
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

    private void initComponents() {
        final int columnCount = dataProvider.getColumnCount();

        final DefaultTableModel model = new DefaultTableModel(new Object[]{"Column", "Open", "High", "Low", "Close", "Volume"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) {
                    return ColumnConfig.class;
                }
                return Boolean.class;
            }
        };
        for (int i = 0; i < columnCount; i++) {
            final String name = dataProvider.getColumnName(i);
            final KdbType type = dataProvider.getColumnType(i);

            final ColumnConfig cc = new ColumnConfig(i, name, type);
            if (ColumnConfig.isTemporalType(type)) {
                domainComponent.addItem(cc);
            } else if (ColumnConfig.isNumberType(type)) {
                model.addRow(new Object[]{cc, false, false, false, false, false});
            }
        }

        domainComponent.setRenderer(ColumnConfig.createListCellRenderer());
        domainComponent.addActionListener(e -> processConfigChanged());

        rangesComponent.setModel(model);
        rangesComponent.setVisibleRowCount(columnCount);
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
    public BaseChartPanel createChartPanel() {
        final ChartConfig chartConfig = createChartConfig();
        return chartConfig.isEmpty() ? null : new OHLCCharPanel(chartConfig, dataProvider);
    }

    private ChartConfig createChartConfig() {
        final ColumnConfig open = getColumnConfig(1);
        final ColumnConfig high = getColumnConfig(2);
        final ColumnConfig low = getColumnConfig(3);
        final ColumnConfig close = getColumnConfig(4);
        final ColumnConfig volume = getColumnConfig(5);
        return new ChartConfig(domainComponent.getItem(), open, high, low, close, volume);
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
}
