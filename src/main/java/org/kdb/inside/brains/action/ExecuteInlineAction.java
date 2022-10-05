package org.kdb.inside.brains.action;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.ide.TooltipEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.HintHint;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBScrollPane;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbQuery;
import org.kdb.inside.brains.view.KdbOutputFormatter;
import org.kdb.inside.brains.view.console.table.TableResult;
import org.kdb.inside.brains.view.console.table.TableResultView;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;

public class ExecuteInlineAction extends ExecuteAction implements DumbAware {
    public static final Dimension MAX_SIZE = new Dimension(800, 500);

    private TableResultView createTableHint(Project project, KdbOutputFormatter formatter, TableResult result) {
        final TableResultView view = new TableResultView(project, formatter, false, null);
        view.showResult(result);
        view.setPreferredSize(MAX_SIZE);
        return view;
    }

    private JTextPane createTextHint(String text, boolean error) {
        JTextPane p = new JTextPane();
        p.setEditable(false);

        if (error) {
            try {
                final StyledDocument doc = p.getStyledDocument();
                Style style = p.addStyle("error", null);
                StyleConstants.setForeground(style, JBColor.RED);
                doc.insertString(0, text, style);
            } catch (BadLocationException e) {
                p.setText(text);
            }
        } else {
            p.setText(text);
        }
        p.setCaretPosition(0);

        return p;
    }

    private void showHint(JComponent component, Editor editor) {
        final LightweightHint lightweightHint = new LightweightHint(component) {
            @Override
            protected boolean canAutoHideOn(TooltipEvent event) {
                return false;
            }
        };
        lightweightHint.setCancelOnClickOutside(false);
        lightweightHint.setCancelOnOtherWindowOpen(false);
        lightweightHint.setResizable(false);

        final LogicalPosition position = editor.getCaretModel().getLogicalPosition();

        final Point p = HintManagerImpl.getHintPosition(lightweightHint, editor, position, HintManager.UNDER);
        final HintHint hint = HintManagerImpl
                .createHintHint(editor, p, lightweightHint, HintManager.UNDER, true).setShowImmediately(true);

        final int flags = HintManager.HIDE_BY_ESCAPE | HintManager.HIDE_BY_OTHER_HINT | HintManager.HIDE_BY_CARET_MOVE | HintManager.HIDE_BY_TEXT_CHANGE;

        HintManagerImpl.getInstanceImpl().showEditorHint(lightweightHint, editor, p, flags, 0, true, hint);
    }

    private void showTypedHint(JComponent c, Editor editor) {
        final Dimension ps = c.getPreferredSize();
        if (ps.width > MAX_SIZE.width || ps.height > MAX_SIZE.height) {
            final JBScrollPane component = new JBScrollPane(c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            component.setPreferredSize(MAX_SIZE);
            showHint(component, editor);
        } else {
            showHint(c, editor);
        }
    }

    @Override
    protected void execute(Project project, Editor editor, InstanceConnection connection, TextRange range) {
        try {
            final KdbQuery query = new KdbQuery(editor.getDocument().getText(range));
            connection.query(query, res -> {
                if (res.isError()) {
                    showTypedHint(createTextHint(((Exception) res.getObject()).getMessage(), true), editor);
                } else {
                    final KdbOutputFormatter formatter = KdbOutputFormatter.getInstance();
                    final TableResult tableResult = TableResult.from(query, res);
                    if (tableResult != null) {
                        showTypedHint(createTableHint(project, formatter, tableResult), editor);
                    } else {
                        showTypedHint(createTextHint(formatter.resultToString(res, true, true), false), editor);
                    }
                }
            });
        } catch (Exception ex) {
            showTypedHint(createTextHint(ex.getMessage(), true), editor);
        }
    }
}
