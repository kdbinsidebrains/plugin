package org.kdb.inside.brains.view.console.table;

import com.intellij.find.FindModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.SortableColumnModel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.TableOptions;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.max;

class QTable extends JBTable {
    private final TableOptions tableOptions;
    private final KdbOutputFormatter formatter;
    private final TableResultSearchSession searchSession;

    private final TheDefaultTableCellRenderer valueCellRenderer = new TheDefaultTableCellRenderer();

    private static final Color DECORATED_ROW_COLOR = UIUtil.getDecoratedRowColor();
    private static final JBColor SEARCH_FOREGROUND = new JBColor(Gray._50, Gray._0);
    private static final JBColor SEARCH_BACKGROUND = UIUtil.getSearchMatchGradientStartColor();

    public QTable(Project project, TableOptions tableOptions, KdbOutputFormatter formatter) {
        super(QTableModel.EMPTY_MODEL);

        this.formatter = formatter;
        this.tableOptions = tableOptions;

        updateHeaderRenderer();

        setStriped(false);
        setShowGrid(tableOptions.isShowGrid());

        setShowColumns(true);
        setColumnSelectionAllowed(true);

        setAutoCreateRowSorter(false);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        searchSession = new TableResultSearchSession(project, this);
        searchSession.getFindModel().addObserver(this::processFindModelChanged);
    }

    public TableResultSearchSession getSearchSession() {
        return searchSession;
    }

    @SuppressWarnings("unchecked")
    public TableRowSorter<QTableModel> getRowSorter() {
        return (TableRowSorter<QTableModel>) super.getRowSorter();
    }

    @Override
    public @NotNull Component prepareRenderer(@NotNull TableCellRenderer renderer, int row, int column) {
        final Component c = super.prepareRenderer(renderer, row, column);

        final TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(max(c.getPreferredSize().width + getIntercellSpacing().width + 20, tableColumn.getPreferredWidth()));

        final boolean keyColumn = getModel().isKeyColumn(tableColumn);
        if (keyColumn) {
            c.setBackground(UIUtils.getKeyColumnColor(c.getBackground()));
        }
        return c;
    }

    @Override
    public final TableCellRenderer getCellRenderer(int row, int column) {
        return valueCellRenderer;
    }

    @Override
    public final QTableModel getModel() {
        return (QTableModel) super.getModel();
    }

    @Override
    public final void setModel(@NotNull TableModel model) {
        if (!(model instanceof QTableModel qModel)) {
            throw new UnsupportedOperationException("Only QTableModel is supported");
        }
        super.setModel(qModel);
    }

    @Override
    public final QColumnModel getColumnModel() {
        return (QColumnModel) super.getColumnModel();
    }

    @Override
    public final void createDefaultColumnsFromModel() {
        final QTableModel m = getModel();

        final QColumnModel cm = getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0));
        }

        final QColumnInfo[] columns = m.getColumnInfos();
        for (int i = 0; i < columns.length; i++) {
            final TableColumn c = new TableColumn(i);
            c.setHeaderValue(columns[i].getDisplayName());
            addColumn(c);
        }
    }

    @Override
    protected final QColumnModel createDefaultColumnModel() {
        return new QColumnModel(this);
    }

    @Override
    public final void setColumnModel(@NotNull TableColumnModel model) {
        if (!(model instanceof QColumnModel qModel)) {
            throw new UnsupportedOperationException("Only QColumnModel is supported");
        }
        super.setColumnModel(qModel);
    }

    @Override
    protected final TableRowSorter<TableModel> createRowSorter(TableModel model) {
        final TableRowSorter<TableModel> sorter = new TableRowSorter<>(model) {
            protected boolean useToString(int column) {
                return false;
            }

            @Override
            public TableStringConverter getStringConverter() {
                return super.getStringConverter();
            }

            public Comparator<?> getComparator(int column) {
                if (getModel() instanceof SortableColumnModel model) {
                    ColumnInfo<?, ?>[] columnInfos = model.getColumnInfos();
                    if (column >= 0 && column < columnInfos.length) {
                        Comparator<?> comparator = columnInfos[column].getComparator();
                        if (comparator != null) {
                            return comparator;
                        }
                    }
                }
                return super.getComparator(column);
            }

            public boolean isSortable(int column) {
                if (getModel() instanceof SortableColumnModel model) {
                    final ColumnInfo<?, ?>[] columnInfos = model.getColumnInfos();
                    if (column >= 0 && column < columnInfos.length) {
                        return columnInfos[column].isSortable();
                    }
                }
                return false;
            }
        };
        sorter.setStringConverter(new TableStringConverter() {
            @Override
            public String toString(TableModel model, int row, int column) {
                final Object valueAt = model.getValueAt(row, column);
                if (valueAt == null) {
                    return "";
                }
                return formatter.objectToString(valueAt);
            }
        });
        sorter.setMaxSortKeys(1);
        return sorter;
    }

    void setTableResult(@Nullable TableResult tableResult) {
        if (tableResult == null) {
            setModel(QTableModel.EMPTY_MODEL);
            searchSession.setDelaySearchEnabled(false);
        } else {
            final QTableModel tableModel = tableResult.tableModel();
            setModel(tableModel);

            // 10_000 rows by 20 columns can be sorted fast. No reason for delay
            searchSession.setDelaySearchEnabled(tableModel.getRowCount() * tableModel.getColumnCount() > 200_000);

            updateHeaderWidth();
            processFindModelChanged(searchSession.getFindModel());
        }
    }

    @Override
    public final String getToolTipText(@NotNull MouseEvent event) {
        return null;
    }

    protected void processFindModelChanged(FindModel findModel) {
        getRowSorter().setRowFilter(searchSession.createTableFilter());
        repaint();
    }

    private void updateHeaderRenderer() {
        final JTableHeader header = getTableHeader();

        final ListSelectionModel columnSelectionModel = getColumnModel().getSelectionModel();

        final Border normalBorder = JBUI.Borders.emptyBottom(1);
        final Border selectedBorder = JBUI.Borders.customLine(getSelectionBackground(), 0, 0, 1, 0);
        final TableCellRenderer headerRenderer = header.getDefaultRenderer();
        header.setDefaultRenderer((tbl, val, sel, focus, row, column) -> {
            final boolean selectedIndex = columnSelectionModel.isSelectedIndex(column);
            final JComponent cmp = (JComponent) headerRenderer.getTableCellRendererComponent(tbl, val, sel, focus, row, column);

            final Border border = selectedIndex ? selectedBorder : normalBorder;
            final Border merge = JBUI.Borders.merge(cmp.getBorder(), border, true);
            cmp.setBorder(merge);
            return cmp;
        });
        columnSelectionModel.addListSelectionListener(e -> header.repaint());
    }

    private void updateHeaderWidth() {
        final TableCellRenderer renderer = getTableHeader().getDefaultRenderer();
        final TableColumnModel columnModel = getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); ++i) {
            final TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(renderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, 0, i).getPreferredSize().width);
        }
    }

    private class TheDefaultTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                if (tableOptions.isStriped() && row % 2 == 0) {
                    c.setBackground(DECORATED_ROW_COLOR);
                } else {
                    c.setBackground(table.getBackground());
                }
            }
            return c;
        }

        @NotNull
        private String createColoredText(String text, java.util.List<TextRange> ranges) {
            int s = 0;
            boolean rollover = false;
            final StringBuilder b = new StringBuilder("<html>");
            for (int i = 0; i < text.length(); i++) {
                boolean a = inRanges(i, ranges);
                if (rollover != a) {
                    b.append(StringUtil.escapeXmlEntities(text.substring(s, i)));
                    if (a) {
                        b.append("<span bgcolor=\"").append(ColorUtil.toHtmlColor(SEARCH_BACKGROUND)).append("\" color=\"").append(ColorUtil.toHtmlColor(SEARCH_FOREGROUND)).append("\">");
                    } else {
                        b.append("</span>");
                    }
                    s = i;
                    rollover = a;
                }
            }
            b.append(StringUtil.escapeXmlEntities(text.substring(s)));
            b.append("</html>");
            return b.toString();
        }

        private boolean inRanges(int offset, java.util.List<TextRange> ranges) {
            for (TextRange range : ranges) {
                if (range.contains(offset)) {
                    return true;
                }
            }
            return false;
        }

        private String fixSystemChars(String text) {
            return text.replace('\n', '\u21B5').replace('\t', ' ');
        }

        @Override
        protected void setValue(Object value) {
            String text = formatter.objectToString(value);
            final List<TextRange> extract = searchSession.extract(text);
            if (!extract.isEmpty()) {
                text = createColoredText(text, extract);
            }
            text = fixSystemChars(text);
            super.setText(text);
        }
    }

    private static boolean containsIgnoreCase(String str, String searchStr, boolean ignoreCase) {
        if (str == null || searchStr == null) {
            return false;
        }

        final int length = searchStr.length();
        if (length == 0) {
            return true;
        }

        for (int i = str.length() - length; i >= 0; i--) {
            if (str.regionMatches(ignoreCase, i, searchStr, 0, length)) {
                return true;
            }
        }
        return false;
    }
}