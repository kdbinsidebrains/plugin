package org.kdb.inside.brains.view.console.chart.ohlc;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.ui.FormBuilder;
import icons.KdbIcons;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.console.chart.BaseChartPanel;
import org.kdb.inside.brains.view.console.chart.ChartBuilder;
import org.kdb.inside.brains.view.console.chart.ChartDataProvider;
import org.kdb.inside.brains.view.console.chart.ColumnConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

public class OHLCChartBuilder extends ChartBuilder {
    private JPanel configPanel;

    private final ComboBox<ColumnConfig> dateComponent = new ComboBox<>();
    private final ComboBox<ColumnConfig> openComponent = new ComboBox<>();
    private final ComboBox<ColumnConfig> highComponent = new ComboBox<>();
    private final ComboBox<ColumnConfig> lowComponent = new ComboBox<>();
    private final ComboBox<ColumnConfig> closeComponent = new ComboBox<>();
    private final ComboBox<ColumnConfig> volumeComponent = new ComboBox<>();

    private final List<ComboBox<ColumnConfig>> rangeComponents = List.of(openComponent, highComponent, lowComponent, closeComponent, volumeComponent);

    public OHLCChartBuilder(ChartDataProvider dataProvider) {
        super("Candlestick", KdbIcons.Chart.Candlestick, dataProvider);
    }

    @Override
    public JPanel getConfigPanel() {
        if (configPanel == null) {
            final int columnCount = dataProvider.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                final String name = dataProvider.getColumnName(i);
                final KdbType type = dataProvider.getColumnType(i);

                final ColumnConfig cc = new ColumnConfig(i, name, type);
                if (ColumnConfig.isTemporalType(type)) {
                    dateComponent.addItem(cc);
                } else if (ColumnConfig.isNumberType(type)) {
                    rangeComponents.forEach(c -> c.addItem(cc));
                }
            }

            final FormBuilder formBuilder = FormBuilder.createFormBuilder();
            addComponent("Time: ", initializeComponent(dateComponent), formBuilder);
            addComponent("Open: ", initializeComponent(openComponent), formBuilder);
            addComponent("High: ", initializeComponent(highComponent), formBuilder);
            addComponent("Low: ", initializeComponent(lowComponent), formBuilder);
            addComponent("Close: ", initializeComponent(closeComponent), formBuilder);
            addComponent("Volume: ", initializeComponent(volumeComponent), formBuilder);
            formBuilder.addComponentFillVertically(new JPanel(), 0);

            rangeComponents.forEach(e -> {
                e.setItem(null);
                e.addItemListener(this::processRangeItemChanged);
            });

            configPanel = formBuilder.getPanel();
        }
        return configPanel;
    }

    @Override
    public BaseChartPanel createChartPanel() {
        final ChartConfig chartConfig = createChartConfig();
        return chartConfig.isEmpty() ? null : new OHLCCharPanel(chartConfig, dataProvider);
    }

    private ChartConfig createChartConfig() {
        return new ChartConfig(
                dateComponent.getItem(),
                openComponent.getItem(),
                highComponent.getItem(),
                lowComponent.getItem(),
                closeComponent.getItem(),
                volumeComponent.getItem()
        );
    }

    private void processRangeItemChanged(ItemEvent e) {
        final Object item = e.getItem();
        if (item == null || e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        final Object source = e.getSource();
        for (ComboBox<ColumnConfig> component : rangeComponents) {
            if (component.getItem() == null || source == component) {
                continue;
            }
            if (component.getItem() == item) {
                component.setSelectedItem(null);
            }
        }
    }

    private ComboBox<ColumnConfig> initializeComponent(ComboBox<ColumnConfig> cmp) {
        cmp.addActionListener(e -> processConfigChanged());
        cmp.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("<no selection>");
                } else {
                    setText(((ColumnConfig) value).getLabel());
                }
                return this;
            }
        });
        return cmp;
    }

    private void addComponent(String name, ComboBox<?> cmp, FormBuilder formBuilder) {
        formBuilder.addLabeledComponent(name, cmp);
    }
}
