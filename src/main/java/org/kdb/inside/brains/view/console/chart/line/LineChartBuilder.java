package org.kdb.inside.brains.view.console.chart.line;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.ColorChooser;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ComboBoxCellEditor;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ListTableModel;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.view.console.chart.BaseChartPanel;
import org.kdb.inside.brains.view.console.chart.ChartBuilder;
import org.kdb.inside.brains.view.console.chart.ChartDataProvider;
import org.kdb.inside.brains.view.console.chart.ColumnConfig;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LineChartBuilder extends ChartBuilder {
    private JPanel configPanel;

    private final ComboBox<AxisConfig> domainComponent = new ComboBox<>();
    private final TableView<AxisConfig> rangesComponent = new TableView<>();

    public LineChartBuilder(ChartDataProvider dataProvider) {
        super("Line Chart", KdbIcons.Chart.Line, dataProvider);
    }

    @Override
    public JPanel getConfigPanel() {
        if (configPanel == null) {
            final FormBuilder formBuilder = FormBuilder.createFormBuilder();
            formBuilder.setFormLeftIndent(0);
            formBuilder.addComponent(new JLabel("Domain axis: "));
            formBuilder.setFormLeftIndent(10);
            formBuilder.addComponent(domainComponent);
            formBuilder.setFormLeftIndent(0);
            formBuilder.addComponent(new JLabel("Range axes: "));
            formBuilder.setFormLeftIndent(10);
            formBuilder.addComponent(ScrollPaneFactory.createScrollPane(rangesComponent));

            initializeDomainComponent();
            initializeRangeValuesTable();

            configPanel = formBuilder.getPanel();
        }
        return configPanel;
    }

    @Override
    public BaseChartPanel createChartPanel() {
        final ChartConfig chartConfig = createChartConfig();
        return chartConfig.isEmpty() ? null : new LineChartPanel(chartConfig, dataProvider);
    }

    private ChartConfig createChartConfig() {
        final List<AxisConfig> list = rangesComponent.getItems().stream().filter(c -> c.getGroup() != null && !c.getGroup().isBlank()).collect(Collectors.toList());
        return new ChartConfig(domainComponent.getItem(), list);
    }

    private List<AxisConfig> createColumnConfigs(boolean range) {
        final int columnCount = dataProvider.getColumnCount();
        final List<AxisConfig> res = new ArrayList<>(columnCount);
        for (int i = 0, c = 0; i < columnCount; i++) {
            final String name = dataProvider.getColumnName(i);
            final KdbType type = dataProvider.getColumnType(i);
            if ((range && AxisConfig.isRangeAllowed(type)) || (!range && AxisConfig.isDomainAllowed(type))) {
                res.add(new AxisConfig(i, name, type, ColumnConfig.getDefaultColor(c++)));
            }
        }
        return res;
    }

    private void initializeDomainComponent() {
        createColumnConfigs(false).forEach(domainComponent::addItem);

        domainComponent.addActionListener(e -> processConfigChanged());
        domainComponent.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText(((ColumnConfig) value).getLabel());
                return this;
            }
        });
    }

    private void initializeRangeValuesTable() {
        final List<AxisConfig> columnConfigs = createColumnConfigs(true);
        final Optional<String> maxLabelName = columnConfigs.stream().map(AxisConfig::getLabelWidth).reduce((s, s2) -> s.length() > s2.length() ? s : s2);
        final ListTableModel<AxisConfig> model = new ListTableModel<>(
                new ConfigColumnInfo<>("Axis", true, AxisConfig::getGroup, AxisConfig::setGroup).withMaxStringValue("Range Axis Name"),
                new ConfigColumnInfo<>("Column", false, AxisConfig::getLabel).withMaxStringValue(maxLabelName.orElse("")),
                new ConfigColumnInfo<>("Color", true, AxisConfig::getColor, AxisConfig::setColor).withMaxStringValue("COL"),
                new ConfigColumnInfo<>("Style", true, AxisConfig::getLineStyle, AxisConfig::setLineStyle)
        );
        model.addRows(columnConfigs);
        model.addTableModelListener(e -> processConfigChanged());

        rangesComponent.setModelAndUpdateColumns(model);

        final TableColumnModel columnModel = rangesComponent.getColumnModel();

        final TableColumn groupCol = columnModel.getColumn(0);
        groupCol.setCellEditor(new ConfigComboBoxCellEditor());

        final TableColumn colorCol = columnModel.getColumn(2);
        colorCol.setCellEditor(new ColorTableCellEditor());
        colorCol.setCellRenderer(new ColorTableCellEditor());

        final TableColumn style = columnModel.getColumn(3);
        final ComboBoxTableRenderer<LineStyle> cellEditor = new ComboBoxTableRenderer<>(LineStyle.values()) {
            @Override
            protected String getTextFor(@NotNull LineStyle value) {
                return value.getLabel();
            }

            @Override
            protected Icon getIconFor(@NotNull LineStyle value) {
                return value.getIcon();
            }
        }.withClickCount(1);
        style.setCellEditor(cellEditor);
        style.setCellRenderer(cellEditor);
    }

    private static class ConfigColumnInfo<T, V> extends ColumnInfo<T, V> {
        private final boolean editable;
        private final Function<T, V> getter;
        private final BiConsumer<T, V> setter;

        private String maxStringValue;

        public ConfigColumnInfo(String name, boolean editable, Function<T, V> getter) {
            this(name, editable, getter, null);
        }

        public ConfigColumnInfo(String name, boolean editable, Function<T, V> getter, BiConsumer<T, V> setter) {
            super(name);
            this.getter = getter;
            this.setter = setter;
            this.editable = editable;
        }

        @Override
        public @Nullable V valueOf(T cc) {
            return getter.apply(cc);
        }

        @Override
        public void setValue(T config, V value) {
            if (setter != null) {
                setter.accept(config, value);
            }
        }

        @Override
        public boolean isCellEditable(T cc) {
            return editable;
        }

        @Override
        public @Nullable String getMaxStringValue() {
            return maxStringValue;
        }

        public ConfigColumnInfo<T, V> withMaxStringValue(String str) {
            maxStringValue = str;
            return this;
        }
    }

    private class ConfigComboBoxCellEditor extends ComboBoxCellEditor {
        public ConfigComboBoxCellEditor() {
            setClickCountToStart(1);

            final JComboBox<?> c = (JComboBox<?>) editorComponent;
            final JTextComponent editor = (JTextComponent) c.getEditor().getEditorComponent();
            editor.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    c.setPopupVisible(true);
                    editor.setSelectionStart(0);
                    editor.setSelectionEnd(editor.getText().length());
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            // Don't select the item in the list but set the value
            final JComboBox<?> box = (JComboBox<?>) super.getTableCellEditorComponent(table, null, isSelected, row, column);
            box.getEditor().setItem(value);
            return box;
        }

        @Override
        protected List<String> getComboBoxItems() {
            final ChartConfig chartConfig = createChartConfig();
            if (chartConfig.isEmpty()) {
                return List.of("Value");
            }
            return new ArrayList<>(chartConfig.dataset().keySet());
        }

        @Override
        protected boolean isComboboxEditable() {
            return true;
        }
    }

    private static class ColorTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        private Color editingColor;
        private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final JLabel label = (JLabel) renderer.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
            label.setIcon(AxisConfig.creaColorIcon((Color) value));
            return label;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            final JLabel res = (JLabel) getTableCellRendererComponent(table, value, true, false, row, column);

            editingColor = (Color) value;
            SwingUtilities.invokeLater(() -> {
                final Color c = ColorChooser.chooseColor(table, IdeBundle.message("dialog.title.choose.color"), editingColor);
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