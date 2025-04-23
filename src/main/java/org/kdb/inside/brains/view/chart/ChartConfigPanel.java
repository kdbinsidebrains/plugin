package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tabs.*;
import com.intellij.util.ui.JBUI;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.view.chart.template.ChartTemplate;
import org.kdb.inside.brains.view.chart.template.ChartTemplatesService;
import org.kdb.inside.brains.view.chart.template.CreateTemplateDialog;
import org.kdb.inside.brains.view.chart.template.TemplatesEditorDialog;
import org.kdb.inside.brains.view.chart.types.ChartType;
import org.kdb.inside.brains.view.chart.types.line.LineChartProvider;
import org.kdb.inside.brains.view.chart.types.ohlc.OHLCChartViewProvider;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChartConfigPanel extends JPanel implements Disposable {
    private ChartView activeChartView;

    private final JBTabs chartTabs;
    private final Consumer<ChartView> chartViewSupplier;

    private final JButton templateButton = new JButton("Create Template");
    private final ComboBox<ChartTemplate> templatesComboBox = new ComboBox<>();

    public ChartConfigPanel(@NotNull Project project,
                            @NotNull ChartDataProvider dataProvider,
                            @NotNull Consumer<ChartView> chartViewSupplier,
                            @Nullable Runnable closeHandler) {
        super(new BorderLayout());

        this.chartViewSupplier = chartViewSupplier;

        chartTabs = createTabs(project, dataProvider);

        templateButton.setEnabled(false);
        templateButton.addActionListener(e -> upsertTemplate(project, dataProvider));

        final JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBorder(JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 1, 0, 0, 0));
        buttonPanel.add(templateButton, BorderLayout.WEST);

        if (closeHandler != null) {
            final JButton close = new JButton("Close");
            close.addActionListener(e -> closeHandler.run());
            buttonPanel.add(close, BorderLayout.EAST);
        }

        final JComponent templatePanel = createTemplatePanel(project, dataProvider);

        final JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(templatePanel, BorderLayout.NORTH);
        topPanel.add(chartTabs.getComponent(), BorderLayout.CENTER);

        add(topPanel, BorderLayout.EAST);
        add(buttonPanel, BorderLayout.SOUTH);
        setBorder(JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 0, 1, 0, 0));
    }

    private ChartViewProvider<JPanel, ChartConfig> getSelectedProvider() {
        return getProvider(chartTabs.getSelectedInfo());
    }

    private TabInfo findTabInfo(ChartType type) {
        final List<TabInfo> tabs = chartTabs.getTabs();
        for (TabInfo tab : tabs) {
            @SuppressWarnings("unchecked")
            ChartViewProvider<JPanel, ChartConfig> provider = (ChartViewProvider<JPanel, ChartConfig>) tab.getObject();
            if (provider != null && provider.getType() == type) {
                return tab;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ChartViewProvider<JPanel, ChartConfig> getProvider(TabInfo info) {
        if (info == null) {
            return null;
        }
        return (ChartViewProvider<JPanel, ChartConfig>) info.getObject();
    }

    private JBTabs createTabs(Project project, ChartDataProvider dataProvider) {
        final JBTabs tabs = JBTabsFactory.createTabs(project, this);

        final JBTabsPresentation presentation = tabs.getPresentation();
        presentation.setSingleRow(true);
        presentation.setSupportsCompression(true);
        presentation.setTabsPosition(JBTabsPosition.top);

        final List<ChartViewProvider<?, ?>> builders = List.of(new LineChartProvider(dataProvider), new OHLCChartViewProvider(dataProvider));

        final Insets borderInsets = UIManager.getBorder("Button.border").getBorderInsets(new JButton());
        for (ChartViewProvider<?, ?> builder : builders) {
            builder.addConfigListener(this::configChanged);

            final JComponent panel = builder.getConfigPanel();
            panel.setBorder(JBUI.Borders.empty(0, borderInsets.right));

            final TabInfo info = new TabInfo(ScrollPaneFactory.createScrollPane(panel));
            info.setIcon(builder.getIcon());
            info.setText(builder.getName());
            info.setObject(builder);

            tabs.addTab(info);
        }

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                configChanged();
            }
        });
        return tabs;
    }

    private void configChanged() {
        final ChartViewProvider<JPanel, ChartConfig> provider = getSelectedProvider();
        if (provider == null) {
            templateButton.setEnabled(false);
            notifyChartChanged(null);
        } else {
            final ChartView chartView = provider.createChartView();

            final ChartTemplate template = templatesComboBox.getItem();
            templateButton.setEnabled(chartView != null && (template == null || !chartView.config().equals(template.getConfig())));
            notifyChartChanged(chartView);
        }
    }

    private void notifyChartChanged(ChartView chartView) {
        boolean notify;
        if (chartView == null) {
            notify = activeChartView != null;
        } else {
            notify = activeChartView == null || !Objects.equals(chartView.config(), activeChartView.config());
        }
        activeChartView = chartView;

        if (notify) {
            chartViewSupplier.accept(chartView);
        }
    }

    public void updateChart(ChartTemplate template) {
        if (templatesComboBox.getItem() != template) {
            templatesComboBox.setSelectedItem(template);
        }

        templateButton.setText(template == null ? "Create Template" : "Update Template");
        if (template == null) {
            templateButton.setEnabled(!getSelectedProvider().createChartConfig().isInvalid());
            return;
        }

        final ChartConfig config = template.getConfig();

        final ChartType type = config.getChartType();
        final TabInfo tab = findTabInfo(type);
        if (tab == null) {
            return;
        }
        chartTabs.select(tab, false);
        getProvider(tab).updateChartConfig(config);
    }

    private void upsertTemplate(@NotNull Project project, ChartDataProvider dataProvider) {
        final ChartViewProvider<JPanel, ChartConfig> provider = getSelectedProvider();
        if (provider == null) {
            return;
        }

        final ChartConfig config = provider.createChartConfig();
        ChartTemplate template = templatesComboBox.getItem();
        if (template != null) {
            template.setConfig(config);
            templateButton.setEnabled(false);
        } else {
            template = new ChartTemplate(config);
            final CreateTemplateDialog d = new CreateTemplateDialog(project, template);
            if (d.showAndGet()) {
                ChartTemplatesService.getService(project).addTemplate(template);
                invalidateTemplatesList(project, dataProvider);
                templateButton.setEnabled(false);
                templatesComboBox.setSelectedItem(template);
            }
        }
    }

    private void invalidateTemplatesList(@NotNull Project project, ChartDataProvider dataProvider) {
        final ChartTemplatesService service = ChartTemplatesService.getService(project);

        final List<ChartTemplate> templates = service.getTemplates().stream().filter(t -> t.getConfig().isApplicable(dataProvider)).collect(Collectors.toList());
        templates.add(0, null); // No template element. Selected by default

        final Object selectedItem = templatesComboBox.getSelectedItem();
        templatesComboBox.setModel(new DefaultComboBoxModel<>(templates.toArray(ChartTemplate[]::new)));
        templatesComboBox.setSelectedItem(selectedItem);
    }

    private JPanel createTemplatePanel(@NotNull Project project, @NotNull ChartDataProvider dataProvider) {
        invalidateTemplatesList(project, dataProvider);
        templatesComboBox.setEditable(false);
        templatesComboBox.addItemListener(e -> updateChart(templatesComboBox.getItem()));

        templatesComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends ChartTemplate> list, ChartTemplate value, int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    setIcon(null);
                    append("No template", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                } else {
                    setIcon(value.getIcon());
                    append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES, true);
                }
            }
        });

        final AnAction action = new AnAction("Modify Templates", "Manage charting templates", KdbIcons.Chart.Templates) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                TemplatesEditorDialog.showDialog(project, templatesComboBox.getItem());
                invalidateTemplatesList(project, dataProvider);
            }
        };
        final ActionButton manage = new ActionButton(action, action.getTemplatePresentation().clone(), ActionPlaces.CHARTS_PANEL_TOOLBAR, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);

        final JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel("Templates:"), BorderLayout.WEST);
        p1.add(manage, BorderLayout.EAST);

        final JPanel p = new JPanel(new BorderLayout());
        p.add(p1, BorderLayout.NORTH);
        p.add(templatesComboBox, BorderLayout.CENTER);

        p.setBorder(new CompoundBorder(JBUI.Borders.customLine(JBColor.LIGHT_GRAY, 0, 0, 1, 0), JBUI.Borders.empty(5, 3)));
        return p;
    }

    @Override
    public void dispose() {
    }
}