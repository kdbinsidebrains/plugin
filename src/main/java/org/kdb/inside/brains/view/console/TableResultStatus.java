package org.kdb.inside.brains.view.console;

import com.intellij.util.ui.GridBag;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.KdbOutputFormatter;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;

public class TableResultStatus extends JPanel {
    private final JTable myTable;
    private final KdbOutputFormatter formatter;

    private final JLabel avgLabel = new JLabel();
    private final JLabel sumLabel = new JLabel();
    private final JLabel countLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel sizeLabel = new JLabel();
    private final JLabel queryLabel = new JLabel();

    public TableResultStatus(JTable table, KdbOutputFormatter formatter) {
        this.myTable = table;
        this.formatter = formatter;

        final GridBag c = new GridBag()
                .setDefaultAnchor(0, GridBagConstraints.LINE_START)
                .setDefaultWeightX(0, 1)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(3, 10, 3, 3);

        setLayout(new GridBagLayout());
        add(queryLabel, c.next());
        add(avgLabel, c.next());
        add(Box.createHorizontalStrut(10), c.next());
        add(countLabel, c.next());
        add(sumLabel, c.next());
        add(timeLabel, c.next());
        add(sizeLabel, c.next());

        myTable.getSelectionModel().addListSelectionListener(e -> selectionChanged());
        myTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> selectionChanged());
    }

    private void selectionChanged() {
        final int[] rows = myTable.getSelectionModel().getSelectedIndices();
        final int[] columns = myTable.getColumnModel().getSelectionModel().getSelectedIndices();
        double sum = 0;
        for (int row : rows) {
            for (int column : columns) {
                final Object value = myTable.getValueAt(row, column);
                if (value instanceof Number && !kx.c.qn(value)) {
                    Number at = (Number) value;
                    sum += at.doubleValue();
                }
            }
        }

        final int cnt = rows.length * columns.length;
        if (cnt == 0) {
            sumLabel.setText("");
            avgLabel.setText("");
            countLabel.setText("");
        } else {
            sumLabel.setText("Sum: " + formatter.formatDouble(sum));
            avgLabel.setText("Average: " + formatter.formatDouble(sum / cnt));
            countLabel.setText("Count: " + cnt);
        }
    }

    public void invalidateRowsCount() {
        sizeLabel.setText(myTable.getRowCount() + " of " + myTable.getModel().getRowCount() + " rows");
    }

    public void showResult(TableResult tableResult) {
        if (tableResult == null) {
            timeLabel.setText("");
            sizeLabel.setText("Empty");
            queryLabel.setText("");
            sumLabel.setText("");
            avgLabel.setText("");
            countLabel.setText("");
        } else {
            final KdbResult result = tableResult.getResult();

            final double v = result.getRoundtripMillis() / 1000d;
            final double v1 = ((int) (v * 100)) / 100d;
            timeLabel.setText(formatter.formatTimestamp(new Timestamp(result.getFinishedMillis())) + " (" + v1 + "sec)");
            queryLabel.setText(tableResult.getQuery().getExpression());

            invalidateRowsCount();
        }
    }
}
