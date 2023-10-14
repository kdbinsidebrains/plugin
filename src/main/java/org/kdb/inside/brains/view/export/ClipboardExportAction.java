package org.kdb.inside.brains.view.export;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.jdesktop.swingx.plaf.basic.core.BasicTransferable;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.TableOptions;
import org.kdb.inside.brains.view.console.table.TableResult;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.datatransfer.Transferable;

public final class ClipboardExportAction extends AnExportAction<CopyPasteManager> {
    public ClipboardExportAction(String text, ExportingType type, ExportDataProvider resultView) {
        super(text, type, resultView);
    }

    public ClipboardExportAction(String text, ExportingType type, ExportDataProvider resultView, String description) {
        super(text, type, resultView, description);
    }

    public ClipboardExportAction(String text, ExportingType type, ExportDataProvider resultView, String description, Icon icon) {
        super(text, type, resultView, description, icon);
    }

    @Override
    protected CopyPasteManager getExportConfig(Project project, ExportDataProvider view) {
        return CopyPasteManager.getInstance();
    }

    @Override
    protected void exportResultView(Project project, ExportingType type, CopyPasteManager copyPasteManager, ExportDataProvider dataProvider, @NotNull ProgressIndicator indicator) {
        final JTable table = dataProvider.getTable();

        final StringBuilder htmlStr = new StringBuilder();
        final StringBuilder plainStr = new StringBuilder();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        final Colors header = getHeader(table);
        final Colors[] tableO = getTable(table, true);
        final Colors[] tableE = getTable(table, false);

        final TableOptions options = KdbSettingsService.getInstance().getTableOptions();

        htmlStr.append("<html>\n");
        htmlStr.append("<style>\n");
        htmlStr.append("""
                body {
                    background-color: #93B874;
                }
                """);
        if (options.isShowGrid()) {
            htmlStr.append("table {  border-collapse: collapse; }\n");
            htmlStr.append("table, th, td, tr { border: 1px solid black;}\n");
        }
        htmlStr.append("th { background-color: ").append("#F5F5F5").append("; font-color: ").append(header.f).append(";}\n");
        for (int i = 0; i < tableO.length; i++) {
            final Colors c = tableO[i];
            htmlStr.append("td.o").append(i).append(" { background-color: ").append(c.b).append("; font-color: ").append(c.f).append(";}\n");
        }
        for (int i = 0; i < tableE.length; i++) {
            final Colors c = tableE[i];
            htmlStr.append("td.e").append(i).append(" { background-color: ").append(c.b).append("; font-color: ").append(c.f).append(";}\n");
        }

        htmlStr.append("</style>\n");
        htmlStr.append("<body>\n<table>\n");
        if (type.withHeader()) {
            htmlStr.append("<tr>\n");

            for (int i = ci.reset(); i != -1; i = ci.next()) {
                String val = table.getColumnName(i);
                plainStr.append(val).append('\t');
                htmlStr.append("  <th>").append(val).append("</th>\n");
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
            htmlStr.append("</tr>\n");
        }

        int count = 0;
        double totalCount = ri.count() * ci.count();
        indicator.setIndeterminate(false);
        final KdbOutputFormatter formatter = dataProvider.getOutputFormatter();
        for (int r = ri.reset(); r != -1 && !indicator.isCanceled(); r = ri.next()) {
            String t = r % 2 == 0 ? "o" : "e";
            htmlStr.append("<tr>\n");
            for (int c = ci.reset(); c != -1 && !indicator.isCanceled(); c = ci.next()) {
                Object obj = table.getValueAt(r, c);
                String val = formatter.objectToString(obj);
                plainStr.append(val).append('\t');

                htmlStr.append("  <td class=\"").append(t).append(c).append("\">");
                htmlStr.append(StringEscapeUtils.escapeXml(val));
                htmlStr.append("</td>\n");
                indicator.setFraction(count++ / totalCount);
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
            htmlStr.append("</tr>\n");
        }

        if (indicator.isCanceled()) {
            return;
        }

        // remove the last newline
        plainStr.deleteCharAt(plainStr.length() - 1);
        htmlStr.append("</table>\n</body>\n</html>");

        final Transferable transferable = new BasicTransferable(plainStr.toString(), htmlStr.toString());
        copyPasteManager.setContents(transferable);
    }

    private Colors getHeader(JTable table) {
        final JTableHeader tableHeader = table.getTableHeader();
        return new Colors(tableHeader.getForeground(), tableHeader.getBackground());
    }

    private Colors[] getTable(JTable table, boolean odd) {
        final TableModel model = table.getModel();
        final Colors[] res = new Colors[table.getColumnCount()];
        for (int i = 0; i < res.length; i++) {
            Colors c;
            if (odd) {
                c = new Colors(table.getForeground(), table.getBackground());
            } else {
                c = new Colors(table.getForeground(), UIUtil.getDecoratedRowColor());
            }

            final TableColumn column = table.getColumnModel().getColumn(i);
            if (model instanceof TableResult.QTableModel m && m.isKeyColumn(column.getModelIndex())) {
                c = c.toKeyColors();
            }
            res[i] = c;
        }
        return res;
    }

    private static class Colors {
        final String f;
        final String b;

        final Color fc;
        final Color bc;

        public Colors(Color f, Color b) {
            fc = f;
            bc = b;
            this.f = UIUtils.encodeColor(f);
            this.b = UIUtils.encodeColor(b);
        }

        public Colors toKeyColors() {
            return new Colors(UIUtils.getKeyColumnColor(fc), UIUtils.getKeyColumnColor(bc));
        }
    }
}