package org.kdb.inside.brains.view.export;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import icons.KdbIcons;
import kx.c;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.TableResult;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;
import java.awt.*;

@Deprecated
/**
 * I'm not happy with this functionality. See no reason. Probably more useful is show/hide columns instead.
 */
public class FlipTableExportAction extends AnExportAction<Boolean> {
    public FlipTableExportAction(String text, ExportingType type, ExportDataProvider exportingView, String description) {
        super(text, type, exportingView, description, KdbIcons.Console.FlipTable);
    }

    @Override
    protected Boolean getExportConfig(Project project, ExportDataProvider view) {
        return Boolean.TRUE;
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, Boolean cfg, ExportDataProvider dataProvider, KdbOutputFormatter formatter, @NotNull ProgressIndicator indicator) throws Exception {
        final JTable table = dataProvider.getTable();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        final String[] columns = new String[ri.count() + 1];
        columns[0] = "Column";
        for (int r = ri.reset(), j = 1; r != -1; r = ri.next(), j++) {
            columns[j] = "#" + (r + 1);
        }

        final String[] names = new String[ci.count()];
        for (int c = ci.reset(), i = 0; c != -1; c = ci.next(), i++) {
            names[i] = table.getColumnName(c);
        }

        final Object[] values = new Object[ri.count() + 1];
        values[0] = names;
        for (int r = ri.reset(), j = 1; r != -1; r = ri.next(), j++) {
            final Object[] row = new Object[ci.count()];
            for (int c = ci.reset(), i = 0; c != -1; c = ci.next(), i++) {
                row[i] = table.getValueAt(r, c);
            }
            values[j] = row;
        }

        if (indicator.isCanceled()) {
            return;
        }

        final c.Flip data = new c.Flip(new c.Dict(columns, values));
        SwingUtilities.invokeAndWait(() -> {
            final TableResultView view = new TableResultView(project, formatter, null); // Compact mode is required here
            view.showResult(TableResult.from(new KdbQuery(""), KdbResult.with(data)));

            final FrameWrapper frame = new FrameWrapper(project, "TableResultView.FlipTable", true, "Flipped Table Result", view);
            frame.closeOnEsc();
            var ideFrame = WindowManagerEx.getInstanceEx().getIdeFrame(project);
            if (ideFrame != null) {
                final Rectangle rectangle = ideFrame.suggestChildFrameBounds();
                frame.setSize(rectangle.getSize());
                frame.setLocation(rectangle.getLocation());
            }
            frame.setTitle("");
            frame.show(false);
        });
    }
}