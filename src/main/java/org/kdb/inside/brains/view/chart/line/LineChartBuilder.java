package org.kdb.inside.brains.view.chart.line;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ListTableModel;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.chart.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LineChartBuilder extends ChartBuilder {
    private final ComboBox<AxisConfig> domainComponent = new ComboBox<>();
    private final TableView<AxisConfig> rangesComponent = new TableView<>();
    private final TableView<SeriesConfig> seriesComponent = new TableView<>();
    private final JCheckBox shapesCheckbox = new JCheckBox("Draw shapes where possible", false);

    public LineChartBuilder(ChartDataProvider dataProvider) {
        super("Line Chart", KdbIcons.Chart.Line, dataProvider);
    }

    @Override
    public JPanel createConfigPanel() {
        initOptions();
        initializeSeriesComponent();
        initializeDomainComponent();
        initializeRangeValuesTable();

        final FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Domain axis: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(domainComponent);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Series definition: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(ScrollPaneFactory.createScrollPane(seriesComponent));

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Range axes: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(ToolbarDecorator.createDecorator(rangesComponent).disableAddAction().disableRemoveAction().createPanel());

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new JLabel("Options: "));
        formBuilder.setFormLeftIndent(10);
        formBuilder.addComponent(shapesCheckbox);

        JPanel p = new JPanel(new BorderLayout());
        p.add(formBuilder.getPanel(), BorderLayout.PAGE_START);
        return p;
    }

    @Override
    public BaseChartPanel createChartPanel() {
        final ChartConfig chartConfig = createChartConfig();
        return chartConfig.isEmpty() ? null : new LineChartPanel(chartConfig, dataProvider);
    }

    private ChartConfig createChartConfig() {
        final List<AxisConfig> list = rangesComponent.getItems().stream().filter(c -> c.getSeries() != null && !c.getSeries().getName().isBlank()).collect(Collectors.toList());
        return new ChartConfig(domainComponent.getItem(), list, shapesCheckbox.isSelected());
    }

    private void initializeDomainComponent() {
        createColumnConfigs(false).forEach(domainComponent::addItem);

        domainComponent.addActionListener(e -> processConfigChanged());
        domainComponent.setRenderer(ColumnConfig.createListCellRenderer());
    }

    private void initOptions() {
        shapesCheckbox.addActionListener(l -> processConfigChanged());
    }

    private void initializeSeriesComponent() {
        final ListTableModel<SeriesConfig> model = new ListTableModel<>(
                new ChartColumnInfo<>("Name", SeriesConfig::getName, SeriesConfig::setName),
                new ChartColumnInfo<>("Style", SeriesConfig::getType, SeriesConfig::setType),
                new ChartColumnInfo<>("Lower Margin", SeriesConfig::getLowerMargin, SeriesConfig::setLowerMargin),
                new ChartColumnInfo<>("Upper Margin", SeriesConfig::getUpperMargin, SeriesConfig::setUpperMargin)
        );
        model.addRow(new SeriesConfig("Value", SeriesType.LINE));
        model.addRow(new SeriesConfig("", SeriesType.LINE));
        model.addTableModelListener(e -> {
            final int firstRow = e.getFirstRow();
            final int lastRowIndex = model.getRowCount() - 1;
            if (model.getItem(firstRow).getName().isEmpty() && firstRow != lastRowIndex) {
                model.removeRow(e.getFirstRow());
            } else if (!model.getItem(lastRowIndex).getName().isEmpty()) {
                model.addRow(new SeriesConfig("", SeriesType.LINE));
            }
            rangesComponent.repaint();
            processConfigChanged();
        });

        seriesComponent.setVisibleRowCount(5);
        UIUtil.putClientProperty(seriesComponent, JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        seriesComponent.setModelAndUpdateColumns(model);
        seriesComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final TableColumnModel columnModel = seriesComponent.getColumnModel();

        final TableColumn style = columnModel.getColumn(1);
        final ComboBoxTableRenderer<SeriesType> cellEditor = new ComboBoxTableRenderer<>(SeriesType.values()) {
            @Override
            protected String getTextFor(@NotNull SeriesType value) {
                return value.getLabel();
            }

            @Override
            protected Icon getIconFor(@NotNull SeriesType value) {
                return value.getIcon();
            }
        }.withClickCount(1);
        style.setCellEditor(cellEditor);
        style.setCellRenderer(cellEditor);

        columnModel.getColumn(2).setCellEditor(new IntSpinnerCellEditor());
        columnModel.getColumn(3).setCellEditor(new IntSpinnerCellEditor());
    }

    private void initializeRangeValuesTable() {
        final List<AxisConfig> columnConfigs = createColumnConfigs(true);
        final Optional<String> maxLabelName = columnConfigs.stream().map(AxisConfig::getLabelWidth).reduce((s, s2) -> s.length() > s2.length() ? s : s2);
        final ListTableModel<AxisConfig> model = new ListTableModel<>(
                new ChartColumnInfo<>("Axis", AxisConfig::getSeries, AxisConfig::setSeries, "Range Axis Name"),
                new ChartColumnInfo<>("Column", AxisConfig::getLabel, null, maxLabelName.orElse("")),
                new ChartColumnInfo<>("Color", AxisConfig::getColor, AxisConfig::setColor),
                new ChartColumnInfo<>("Width", AxisConfig::getWidth, AxisConfig::setWidth)
        );
        model.addRows(columnConfigs);
        model.addTableModelListener(e -> processConfigChanged());

        rangesComponent.setModelAndUpdateColumns(model);
        rangesComponent.setVisibleRowCount(columnConfigs.size());
        UIUtil.putClientProperty(rangesComponent, JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        final TableColumnModel columnModel = rangesComponent.getColumnModel();

        final ComboBox<SeriesConfig> seriesBox = new ComboBox<>(new SeriesComboboxModel(seriesComponent));
        seriesBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final SeriesConfig sc = (SeriesConfig) value;
                return super.getListCellRendererComponent(list, sc == null ? "" : sc.getLabel(), index, isSelected, cellHasFocus);
            }
        });

        final TableColumn groupCol = columnModel.getColumn(0);
        groupCol.setCellEditor(new DefaultCellEditor(seriesBox));
        groupCol.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final SeriesConfig sc = (SeriesConfig) value;
                return super.getTableCellRendererComponent(table, sc == null ? "" : sc.getLabel(), isSelected, hasFocus, row, column);
            }
        });

        final TableColumn colorCol = columnModel.getColumn(2);
        colorCol.setCellEditor(new ColorTableCellEditor());
        colorCol.setCellRenderer(new ColorTableCellEditor());

        final TableColumn width = columnModel.getColumn(3);
        final DefaultCellEditor widthEditor = new DefaultCellEditor(new JFormattedTextField(0.0D)) {
            @Override
            public Object getCellEditorValue() {
                return textField().getValue();
            }

            @Override
            public boolean stopCellEditing() {
                try {
                    textField().commitEdit();
                    return super.stopCellEditing();
                } catch (Exception ex) {
                    return false;
                }
            }

            private JFormattedTextField textField() {
                return (JFormattedTextField) editorComponent;
            }
        };
        widthEditor.setClickCountToStart(1);
        width.setCellEditor(widthEditor);
    }

    private List<AxisConfig> createColumnConfigs(boolean range) {
        final int columnCount = dataProvider.getColumnCount();
        final List<AxisConfig> res = new ArrayList<>(columnCount);
        for (int i = 0, c = 0; i < columnCount; i++) {
            final String name = dataProvider.getColumnName(i);
            final KdbType type = dataProvider.getColumnType(i);
            if ((range && AxisConfig.isRangeAllowed(type)) || (!range && AxisConfig.isDomainAllowed(type))) {
                res.add(new AxisConfig(i, name, type, ChartColors.getDefaultColor(c++)));
            }
        }
        return res;
    }

    private static class SeriesComboboxModel extends AbstractListModel<SeriesConfig> implements ComboBoxModel<SeriesConfig> {
        private Object selected;
        private final TableView<SeriesConfig> table;

        public SeriesComboboxModel(TableView<SeriesConfig> table) {
            this.table = table;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            selected = anItem;
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        @Override
        public int getSize() {
            return table.getRowCount();
        }

        @Override
        public SeriesConfig getElementAt(int index) {
            return table.getRow(index);
        }
    }

    private static class IntSpinnerCellEditor extends AbstractTableCellEditor {
        private final JBIntSpinner intSpinner = new JBIntSpinner(5, 0, 1000, 5);

        public IntSpinnerCellEditor() {
            final JSpinner.NumberEditor editor = (JSpinner.NumberEditor) intSpinner.getEditor();
            editor.getTextField().addActionListener(e -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            intSpinner.setValue(value);
            return intSpinner;
        }

        @Override
        public boolean stopCellEditing() {
            try {
                intSpinner.commitEdit();
            } catch (Exception ignore) {
                // do nothing - just revert it back
            }
            return super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            return intSpinner.getValue();
        }
    }

    private static class ColorTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private Color editingColor;
        private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
            label.setIcon(AxisConfig.createIcon((Color) value));
            return label;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            final JLabel res = (JLabel) getTableCellRendererComponent(table, value, true, false, row, column);

            editingColor = (Color) value;
            SwingUtilities.invokeLater(() -> {
                final Color c = ColorChooser.chooseColor(table, IdeBundle.message("dialog.title.choose.color"), editingColor, true);
                if (c == null) {
                    cancelCellEditing();
                } else {
                    editingColor = c;
                    stopCellEditing();
                }
            });
            return res;
        }

        @Override
        public Object getCellEditorValue() {
            return editingColor;
        }
    }
}