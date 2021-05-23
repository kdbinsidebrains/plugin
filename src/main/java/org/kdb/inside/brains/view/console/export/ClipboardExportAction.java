package org.kdb.inside.brains.view.console.export;

import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.plaf.basic.core.BasicTransferable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.settings.KdbSettingsService;
import org.kdb.inside.brains.view.console.ConsoleOptions;
import org.kdb.inside.brains.view.console.TableResult;
import org.kdb.inside.brains.view.console.TableResultView;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.Transferable;

public final class ClipboardExportAction extends AnExportAction {
    public ClipboardExportAction(String text, ExportingType type, TableResultView resultView) {
        super(text, type, resultView);
    }

    public ClipboardExportAction(String text, ExportingType type, TableResultView resultView, String description) {
        super(text, type, resultView, description);
    }

    public ClipboardExportAction(String text, ExportingType type, TableResultView resultView, String description, Icon icon) {
        super(text, type, resultView, description, icon);
    }

    @Override
    protected void performAction(Project project, TableResultView view, ExportingType type) {
        final JBTable table = view.getTable();

        final StringBuilder htmlStr = new StringBuilder();
        final StringBuilder plainStr = new StringBuilder();

        final ExportingType.IndexIterator ri = type.rowsIterator(table);
        final ExportingType.IndexIterator ci = type.columnsIterator(table);

        final Colors header = getHeader(table);
        final Colors[] tableO = getTable(table, true);
        final Colors[] tableE = getTable(table, false);

        final ConsoleOptions options = KdbSettingsService.getInstance().getConsoleOptions();

        htmlStr.append("<html>\n");
        htmlStr.append("<style>\n");
        htmlStr.append("body {\n" +
                "    background-color: #93B874;\n" +
                "}\n");
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

        for (int r = ri.reset(); r != -1; r = ri.next()) {
            String t = r % 2 == 0 ? "o" : "e";
            htmlStr.append("<tr>\n");
            ci.reset();
            for (int c = ci.reset(); c != -1; c = ci.next()) {
                Object obj = table.getValueAt(r, c);
                String val = view.convertValue(obj);
                plainStr.append(val).append('\t');

                htmlStr.append("  <td class=\"").append(t).append(c).append("\">").append(val).append("</td>\n");
            }
            // we want a newline at the end of each line and not a tab
            plainStr.deleteCharAt(plainStr.length() - 1).append('\n');
            htmlStr.append("</tr>\n");
        }

        // remove the last newline
        plainStr.deleteCharAt(plainStr.length() - 1);
        htmlStr.append("</table>\n</body>\n</html>");

        final Transferable transferable = new BasicTransferable(plainStr.toString(), htmlStr.toString());
        CopyPasteManager.getInstance().setContents(transferable);
    }

    private Colors getHeader(JBTable table) {
        final JTableHeader tableHeader = table.getTableHeader();
        return new Colors(tableHeader.getForeground(), tableHeader.getBackground());
    }

    private Colors[] getTable(JBTable table, boolean odd) {
        final TableResult.QTableModel model = (TableResult.QTableModel) table.getModel();
        final Colors[] res = new Colors[table.getColumnCount()];
        for (int i = 0; i < res.length; i++) {
            Colors c;
            if (odd) {
                c = new Colors(table.getForeground(), table.getBackground());
            } else {
                c = new Colors(table.getForeground(), UIUtil.getDecoratedRowColor());
            }

            if (model.isKeyColumn(i)) {
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