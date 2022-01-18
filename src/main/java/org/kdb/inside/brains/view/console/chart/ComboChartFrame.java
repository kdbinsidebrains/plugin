package org.kdb.inside.brains.view.console.chart;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.FormBuilder;
import icons.KdbIcons;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.console.chart.line.LineChartBuilder;
import org.kdb.inside.brains.view.console.chart.ohlc.OHLCChartBuilder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;

// TODO: to be removed
@Deprecated
public class ComboChartFrame extends FrameWrapper {
    private final JPanel chartPanel = new JPanel(new BorderLayout());
    private final JPanel configPanel = new JPanel(new BorderLayout());

    private final ComboBox<ChartBuilder> buildersBox = new ComboBox<>();

    protected ComboChartFrame(@Nullable Project project, String title, ChartDataProvider dataProvider) {
        super(project, "KdbInsideBrains-ChartFrameDimension", false, title);

        final JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(chartPanel, BorderLayout.CENTER);
        rootPanel.add(createConfigPanel(), BorderLayout.EAST);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // All possible configs
        final List<ChartBuilder> builders = createBuilders(dataProvider);
        for (ChartBuilder builder : builders) {
            builder.addConfigListener(this::configChanged);
            buildersBox.addItem(builder);
        }

        buildersBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                final ChartBuilder cb = (ChartBuilder) value;
                setText(cb.getName());
                setIcon(cb.getIcon());
                return this;
            }
        });

        buildersBox.setSelectedIndex(0);
        buildersBox.addItemListener(this::chartTypeChanged);
        chartTypeChanged(null);

        setComponent(rootPanel);
        setImage(IconLoader.toImage(KdbIcons.Chart.Icon));
        closeOnEsc();
    }

    private List<ChartBuilder> createBuilders(ChartDataProvider dataProvider) {
        return List.of(
                new LineChartBuilder(dataProvider),
                new OHLCChartBuilder(dataProvider)
        );
    }

    private void configChanged() {
        final ChartBuilder item = buildersBox.getItem();

        final BaseChartPanel panel = item.createChartPanel();
        chartPanel.removeAll();
        chartPanel.add(panel == null ? createEmptyPanel() : panel, BorderLayout.CENTER);
        chartPanel.revalidate();
    }

    private void chartTypeChanged(ItemEvent e) {
        final ChartBuilder item = buildersBox.getItem();

        configPanel.removeAll();
        configPanel.add(item.getConfigPanel(), BorderLayout.CENTER);

        configChanged();
    }

    private JPanel createConfigPanel() {
        final FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Chart type: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(buildersBox);
        formBuilder.setFormLeftIndent(0);
        formBuilder.addSeparator();

        final JPanel p = new JPanel(new BorderLayout());
        p.add(formBuilder.getPanel(), BorderLayout.PAGE_START);
        p.add(configPanel, BorderLayout.CENTER);
        p.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        return p;
    }

    private static JPanel createEmptyPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(JBColor.WHITE);
        panel.add(new JLabel("<html><center><h1>There is no data to show.</h1><br><br>Please select the chart type and appropriate configuration.<center></html>"));
        return panel;
    }
}
