package org.kdb.inside.brains.view.chart.types.line;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.ui.*;
import com.intellij.ui.components.JBViewport;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.chart.ChartDataProvider;
import org.kdb.inside.brains.view.chart.ColumnDefinition;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.kdb.inside.brains.view.chart.types.line.SeriesDefinition.DEFAULT_LOWER_MARGIN;
import static org.kdb.inside.brains.view.chart.types.line.SeriesDefinition.DEFAULT_UPPER_MARGIN;

public class LineConfigPanel extends JPanel {
    private boolean ignoreUpdate = false;

    private final Runnable callback;
    private final ChartDataProvider dataProvider;

    private static final int LEFT_INDENT = 10;
    private static final int VERTICAL_GAP = 5;
    private final ComboBox<ColumnDefinition> domainComponent = new ComboBox<>();
    private final TableView<SeriesItem> seriesTable = new TableView<>();
    private final JCheckBox shapesCheckbox = new JCheckBox("Draw shapes where possible", false);
    private final TableView<ValuesItem> valuesTable = new TableView<>();
    private final TableView<ExpansionItem> expansionTable = new TableView<>();

    public LineConfigPanel(ChartDataProvider dataProvider, Runnable callback) {
        super(new BorderLayout());

        this.callback = callback;
        this.dataProvider = dataProvider;

        initOptions();
        initializeDomainComponent();
        initializeSeriesComponent();
        initializeValuesComponent();
        initializeExpansionComponent();

        final FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new TitledSeparator("Domain Axis"));
        formBuilder.setFormLeftIndent(LEFT_INDENT);
        formBuilder.addComponent(domainComponent);
        formBuilder.addVerticalGap(VERTICAL_GAP);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new TitledSeparator("Series Definition"));
        formBuilder.setFormLeftIndent(LEFT_INDENT);
        formBuilder.addComponent(ScrollPaneFactory.createScrollPane(seriesTable));
        formBuilder.addVerticalGap(VERTICAL_GAP);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new TitledSeparator("Values Axes"));
        formBuilder.setFormLeftIndent(LEFT_INDENT);
        formBuilder.addComponent(ToolbarDecorator.createDecorator(valuesTable).disableAddAction().disableRemoveAction().createPanel());
        formBuilder.addVerticalGap(VERTICAL_GAP);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new TitledSeparator("Values Expansion"));
        formBuilder.setFormLeftIndent(LEFT_INDENT);
        formBuilder.addComponent(ToolbarDecorator.createDecorator(expansionTable).disableAddAction().disableRemoveAction().createPanel());
        formBuilder.addVerticalGap(VERTICAL_GAP);

        formBuilder.setFormLeftIndent(0);
        formBuilder.addComponent(new TitledSeparator("Options"));
        formBuilder.setFormLeftIndent(LEFT_INDENT);
        formBuilder.addComponent(shapesCheckbox);

        add(formBuilder.getPanel(), BorderLayout.PAGE_START);
    }

    private void initOptions() {
        shapesCheckbox.addActionListener(l -> processConfigChanged());
    }

    private void initializeDomainComponent() {
        Stream.of(dataProvider.getColumns()).filter(c -> c.isTemporal() || c.isNumber()).forEach(domainComponent::addItem);

        domainComponent.addActionListener(e -> processConfigChanged());
        domainComponent.setRenderer(ColumnDefinition.createListCellRenderer());
    }

    private void initializeSeriesComponent() {
        final ListTableModel<SeriesItem> model = new ListTableModel<>(
                new TheColumnInfo<>("Name", SeriesItem::getName, SeriesItem::setName),
                new TheColumnInfo<>("Style", SeriesItem::getStyle, SeriesItem::setStyle),
                new TheColumnInfo<>("Low. Margin", SeriesItem::getLowerMargin, SeriesItem::setLowerMargin),
                new TheColumnInfo<>("Upp. Margin", SeriesItem::getUpperMargin, SeriesItem::setUpperMargin)
        );
        model.addRow(new SeriesItem("Value", SeriesStyle.LINE));
        model.addRow(new SeriesItem("", SeriesStyle.LINE));
        model.addTableModelListener(e -> {
            if (ignoreUpdate) {
                return;
            }
            final int firstRow = e.getFirstRow();
            final int lastRowIndex = model.getRowCount() - 1;
            if (model.getItem(firstRow).getName().isEmpty() && firstRow != lastRowIndex) {
                model.removeRow(e.getFirstRow());
            } else if (!model.getItem(lastRowIndex).getName().isEmpty()) {
                model.addRow(new SeriesItem("", SeriesStyle.LINE));
            }
            valuesTable.repaint();
            processConfigChanged();
        });

        seriesTable.setVisibleRowCount(VERTICAL_GAP);
        seriesTable.putClientProperty(JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        seriesTable.setModelAndUpdateColumns(model);
        seriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final TableColumnModel columnModel = seriesTable.getColumnModel();

        final TableColumn style = columnModel.getColumn(1);
        final ComboBoxTableRenderer<SeriesStyle> cellEditor = new ComboBoxTableRenderer<>(SeriesStyle.values()) {
            @Override
            protected String getTextFor(@NotNull SeriesStyle value) {
                return value.getLabel();
            }

            @Override
            protected Icon getIconFor(@NotNull SeriesStyle value) {
                return value.getIcon();
            }
        }.withClickCount(1);
        style.setCellEditor(cellEditor);
        style.setCellRenderer(cellEditor);

        columnModel.getColumn(2).setCellEditor(new IntSpinnerCellEditor());
        columnModel.getColumn(3).setCellEditor(new IntSpinnerCellEditor());
    }

    private void initializeValuesComponent() {
        final List<ValuesItem> values = Stream.of(dataProvider.getColumns())
                .filter(ColumnDefinition::isNumber)
                .map(ValuesItem::new)
                .toList();

        final Optional<String> maxLabelName = values.stream().map(ValuesItem::getLabelWidthTemplate).reduce((s, s2) -> s.length() > s2.length() ? s : s2);
        final ListTableModel<ValuesItem> model = new ListTableModel<>(
                new TheColumnInfo<>("Series", ValuesItem::getSeries, ValuesItem::setSeries, "Series Name"),
                new TheColumnInfo<>("Column", ValuesItem::getLabel, null, maxLabelName.orElse("")),
                new TheColumnInfo<>("Operation", ValuesItem::getOperation, ValuesItem::setOperation)
        );
        model.addRows(values);
        model.addTableModelListener(e -> processConfigChanged());

        valuesTable.setModelAndUpdateColumns(model);
        valuesTable.setVisibleRowCount(model.getRowCount());
        valuesTable.putClientProperty(JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        final TableColumnModel columnModel = valuesTable.getColumnModel();

        final ComboBox<SeriesItem> seriesBox = new ComboBox<>(new SeriesComboboxModel(seriesTable));
        seriesBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final SeriesItem sc = (SeriesItem) value;
                return super.getListCellRendererComponent(list, sc == null ? "" : sc.name, index, isSelected, cellHasFocus);
            }
        });

        final TableColumn seriesCol = columnModel.getColumn(0);
        seriesCol.setCellEditor(new DefaultCellEditor(seriesBox));
        seriesCol.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final SeriesItem sc = (SeriesItem) value;
                return super.getTableCellRendererComponent(table, sc == null ? "" : sc.name, isSelected, hasFocus, row, column);
            }
        });

        final TableColumn operationCol = columnModel.getColumn(2);
        final ComboBox<Operation> operationComboBox = new ComboBox<>(new DefaultComboBoxModel<>(Operation.values()));
        operationComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                final Operation operation = (Operation) value;
                return super.getListCellRendererComponent(list, operation.getLabel(), index, isSelected, cellHasFocus);
            }
        });

        operationCol.setCellEditor(new DefaultCellEditor(operationComboBox));
        operationCol.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Operation operation = (Operation) value;
                return super.getTableCellRendererComponent(table, operation.getLabel(), isSelected, hasFocus, row, column);
            }
        });
    }

    private void initializeExpansionComponent() {
        final List<ExpansionItem> expansions = Stream.of(dataProvider.getColumns())
                .filter(ColumnDefinition::isSymbol)
                .map(cc -> new ExpansionItem(cc, false, dataProvider.getDistinctCount(cc)))
                .toList();

        final ListTableModel<ExpansionItem> model = new ListTableModel<>(
                new TheColumnInfo<>("Enable", ExpansionItem::isEnabled, ExpansionItem::setEnabled),
                new TheColumnInfo<>("Column", ExpansionItem::getColumn, null),
                new TheColumnInfo<>("Distinct values", ExpansionItem::getValuesCount, null)
        );
        model.addRows(expansions);
        model.addTableModelListener(e -> processConfigChanged());

        expansionTable.setVisibleRowCount(3);
        expansionTable.putClientProperty(JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true);

        expansionTable.setModelAndUpdateColumns(model);
        expansionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        final TableColumnModel columnModel = expansionTable.getColumnModel();
        final TableColumn column = columnModel.getColumn(0);
        column.setCellEditor(new BooleanTableCellEditor());
        column.setCellRenderer(new BooleanTableCellRenderer());

        columnModel.getColumn(1).setCellRenderer(ColumnDefinition.createTableCellRenderer());
    }

    public LineChartConfig createChartConfig() {
        final Map<SeriesItem, SeriesDefinition> series = new HashMap<>();

        final List<ValuesDefinition> values = valuesTable.getItems().stream().filter(i -> i.series != null && !i.series.name.isBlank()).map(i -> {
            final SeriesDefinition def = series.computeIfAbsent(i.series, SeriesItem::createDefinition);
            return new ValuesDefinition(i.column, def, i.operation);
        }).toList();

        final List<ColumnDefinition> expansions = expansionTable.getItems().stream().filter(ExpansionItem::isEnabled).map(ExpansionItem::getColumn).toList();
        return new LineChartConfig(domainComponent.getItem(), values, expansions, shapesCheckbox.isSelected());
    }

    public void updateChartConfig(LineChartConfig config) {
        ignoreUpdate = true;
        try {
            domainComponent.setItem(config.domain());
            shapesCheckbox.setSelected(config.isDrawShapes());

            final List<SeriesItem> seriesItems = new ArrayList<>();
            final List<ValuesItem> valuesItems = new ArrayList<>();
            final Map<SeriesDefinition, SeriesItem> seriesMap = new HashMap<>();

            for (ValuesDefinition value : config.values()) {
                final SeriesDefinition series = value.series();

                SeriesItem item = seriesMap.get(series);
                if (item == null) {
                    item = new SeriesItem(series);
                    seriesItems.add(item);
                    seriesMap.put(series, item);
                }

                final ValuesItem e = new ValuesItem(value.column());
                e.setSeries(item);
                e.setOperation(value.operation());

                valuesItems.add(e);
            }

            final ListTableModel<SeriesItem> seriesModel = seriesTable.getListTableModel();
            seriesModel.setItems(seriesItems);
            seriesModel.addRow(new SeriesItem("", SeriesStyle.LINE));

            final ListTableModel<ValuesItem> valuesModel = valuesTable.getListTableModel();
            final List<ValuesItem> values = new ArrayList<>(valuesModel.getItems());
            final Set<ColumnDefinition> valuesCols = valuesItems.stream().map(v -> v.column).collect(Collectors.toSet());
            values.removeIf(v -> valuesCols.contains(v.column));
            valuesItems.addAll(values);
            valuesModel.setItems(valuesItems);

            final List<ExpansionItem> expansions = new ArrayList<>();
            final ListTableModel<ExpansionItem> expansionModel = expansionTable.getListTableModel();
            final Map<ColumnDefinition, ExpansionItem> collect = expansionModel.getItems().stream().collect(Collectors.toMap(i -> i.column, i -> i));
            for (ColumnDefinition column : config.expansions()) {
                final ExpansionItem remove = collect.remove(column);
                if (remove != null) {
                    remove.setEnabled(true);
                    expansions.add(remove);
                }
            }
            expansions.addAll(collect.values());
            expansionModel.setItems(expansions);
        } finally {
            ignoreUpdate = false;
        }
        processConfigChanged();
    }

    private void processConfigChanged() {
        if (ignoreUpdate) {
            return;
        }
        callback.run();
    }

    private interface Item {
        String getName();
    }

    private static class SeriesComboboxModel extends AbstractListModel<SeriesItem> implements ComboBoxModel<SeriesItem> {
        private final TableView<SeriesItem> table;
        private Object selected;

        public SeriesComboboxModel(TableView<SeriesItem> table) {
            this.table = table;
        }

        @Override
        public Object getSelectedItem() {
            return selected;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            selected = anItem;
        }

        @Override
        public int getSize() {
            return table.getRowCount();
        }

        @Override
        public SeriesItem getElementAt(int index) {
            return table.getRow(index);
        }
    }

    private static class IntSpinnerCellEditor extends AbstractTableCellEditor {
        private final JBIntSpinner intSpinner = new JBIntSpinner(VERTICAL_GAP, 0, 1000, VERTICAL_GAP);

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

    private static class TheColumnInfo<T, V> extends ColumnInfo<T, V> {
        private final String maxStringValue;
        private final Function<T, V> getter;
        private final BiConsumer<T, V> setter;

        public TheColumnInfo(String name, Function<T, V> getter, BiConsumer<T, V> setter) {
            this(name, getter, setter, null);
        }

        public TheColumnInfo(String name, Function<T, V> getter, BiConsumer<T, V> setter, String maxStringValue) {
            super(name);
            this.getter = getter;
            this.setter = setter;
            this.maxStringValue = maxStringValue;
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
            return setter != null;
        }

        @Override
        public @Nullable String getMaxStringValue() {
            return maxStringValue;
        }
    }

    private static class SeriesItem implements Item {
        String name;
        SeriesStyle style;
        int lowerMargin = DEFAULT_LOWER_MARGIN;
        int upperMargin = DEFAULT_UPPER_MARGIN;

        public SeriesItem(SeriesDefinition s) {
            this(s.name(), s.style());
        }

        public SeriesItem(String name, SeriesStyle style) {
            this.name = name;
            this.style = style;
        }

        public int getLowerMargin() {
            return lowerMargin;
        }

        public void setLowerMargin(int lowerMargin) {
            this.lowerMargin = lowerMargin;
        }

        @Override
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SeriesStyle getStyle() {
            return style;
        }

        public void setStyle(SeriesStyle style) {
            this.style = style;
        }

        public int getUpperMargin() {
            return upperMargin;
        }

        public void setUpperMargin(int upperMargin) {
            this.upperMargin = upperMargin;
        }

        public SeriesDefinition createDefinition() {
            return new SeriesDefinition(name, style, lowerMargin, upperMargin);
        }
    }

    private static class ValuesItem implements Item {
        private final ColumnDefinition column;
        private SeriesItem series;
        private Operation operation = Operation.SUM;

        public ValuesItem(ColumnDefinition column) {
            this.column = column;
        }

        @Override
        public String getName() {
            return column.name();
        }

        public String getLabel() {
            return column.getLabel();
        }

        public String getLabelWidthTemplate() {
            return column.getLabelWidthTemplate();
        }

        public Operation getOperation() {
            return operation;
        }

        public void setOperation(Operation operation) {
            this.operation = operation;
        }

        public SeriesItem getSeries() {
            return series;
        }

        public void setSeries(SeriesItem series) {
            this.series = series;
        }
    }

    private static class ExpansionItem implements Item {
        final ColumnDefinition column;
        final long valuesCount;
        boolean enabled;

        public ExpansionItem(ColumnDefinition column, boolean enabled, long valuesCount) {
            this.column = column;
            this.enabled = enabled;
            this.valuesCount = valuesCount;
        }

        public String getName() {
            return column.name();
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public ColumnDefinition getColumn() {
            return column;
        }

        public long getValuesCount() {
            return valuesCount;
        }
    }
}