package org.kdb.inside.brains.view.console.table;

import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;

public record TableResult(KdbQuery query, KdbResult result, QTableModel tableModel) {
    public TableResult copy() {
        return new TableResult(query, result, tableModel);
    }

    public static TableResult from(KdbQuery query, KdbResult result) {
        final Object k = result.getObject();
        if (k == null) {
            return null;
        }
        QTableModel model = QTableModel.from(k);
        return model == null ? null : new TableResult(query, result, model);
    }
}