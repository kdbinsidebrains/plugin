package org.kdb.inside.brains.view.console.table;

import com.intellij.util.ui.GridBag;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;

class TableResultStatusPanel extends JPanel {
    private final JTable myTable;
    private final KdbOutputFormatter formatter;

    private final JLabel selAvgLabel = new JLabel();
    private final JLabel selSumLabel = new JLabel();
    private final JLabel selCountLabel = new JLabel();
    private final JLabel timestampLabel = new JLabel();
    private final JLabel durationLabel = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel queryLabel = new JLabel();
    private final JLabel rowsLabel = new JLabel();

    public TableResultStatusPanel(JTable table, KdbOutputFormatter formatter) {
        this.myTable = table;
        this.formatter = formatter;

        final GridBag c = new GridBag()
                .setDefaultAnchor(0, GridBagConstraints.LINE_START)
                .setDefaultWeightX(0, 1)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(3, 10, 3, 3);

        setLayout(new GridBagLayout());
        add(queryLabel, c.next().fillCell());
        add(selAvgLabel, c.next().fillCellNone());
        add(Box.createHorizontalStrut(10), c.next());
        add(selCountLabel, c.next());
        add(selSumLabel, c.next());
        add(timestampLabel, c.next());
        add(durationLabel, c.next());
        add(sizeLabel, c.next());
        add(rowsLabel, c.next());

        myTable.getSelectionModel().addListSelectionListener(e -> recalculateValues());
        myTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> recalculateValues());

        setMinimumSize(new Dimension(10, 10));
    }

    public void recalculateValues() {
        final int[] rows = myTable.getSelectionModel().getSelectedIndices();
        final int[] columns = myTable.getColumnModel().getSelectionModel().getSelectedIndices();
        double sum = 0;
        for (int row : rows) {
            for (int column : columns) {
                final Object value = myTable.getValueAt(row, column);
                if (value instanceof Number at && !KdbType.isNull(value)) {
                    sum += at.doubleValue();
                }
            }
        }

        final int cnt = rows.length * columns.length;
        if (cnt == 0) {
            selSumLabel.setText("");
            selAvgLabel.setText("");
            selCountLabel.setText("");
        } else {
            selSumLabel.setText("Sum: " + formatter.formatDouble(sum));
            selAvgLabel.setText("Average: " + formatter.formatDouble(sum / cnt));
            selCountLabel.setText("Count: " + cnt);
        }
    }

    public void invalidateRowsCount() {
        rowsLabel.setText(myTable.getRowCount() + " of " + myTable.getModel().getRowCount() + " rows");
    }

    public void showResult(TableResult tableResult) {
        if (tableResult == null) {
            timestampLabel.setText("");
            rowsLabel.setText("Empty");
            queryLabel.setText("");
            selSumLabel.setText("");
            selAvgLabel.setText("");
            selCountLabel.setText("");
            durationLabel.setText("");
            sizeLabel.setText("");
        } else {
            final KdbResult result = tableResult.result();

            timestampLabel.setText(formatter.formatTimestamp(new Timestamp(result.getFinishedMillis())).substring(0, 23));

            final double v = result.getRoundtripMillis() / 1000d;
            final double v1 = ((int) (v * 100)) / 100d;
            durationLabel.setText(v1 + "sec");

            queryLabel.setText(tableResult.query().getExpression());
            sizeLabel.setText(result.getHumanSize());

            invalidateRowsCount();
        }
    }


}