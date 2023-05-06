package org.kdb.inside.brains.view.export;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.view.PopupActionGroup;

import javax.swing.*;
import java.awt.event.KeyEvent;

public interface ExportDataProvider {
    DataKey<ExportDataProvider> DATA_KEY = DataKey.create("KdbConsole.ExportDataProvider");

    static ActionGroup createActionGroup(Project project, ExportDataProvider dataProvider) {
        final JTable table = dataProvider.getTable();

        final DefaultActionGroup group = new DefaultActionGroup();

        final ClipboardExportAction copy_all = new ClipboardExportAction("_Copy", ExportingType.SELECTION_WITH_HEADER, dataProvider, "Copy selected cells into the clipboard", KdbIcons.Console.CopyTable);
        copy_all.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, table);
        group.add(copy_all);

        final PopupActionGroup copyGroup = new PopupActionGroup("Copy _Special", KdbIcons.Console.CopySpecial);

        final ClipboardExportAction copy_values = new ClipboardExportAction("Copy _Values", ExportingType.SELECTION, dataProvider, "Copy selected cells into the clipboard", KdbIcons.Console.CopyValues);
        copy_values.registerCustomShortcutSet(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, table);
        copyGroup.add(copy_values);
        copyGroup.addSeparator();
        copyGroup.add(new ClipboardExportAction("Copy Only Rows", ExportingType.ROWS, dataProvider, "Copy the whole row values"));
        copyGroup.add(new ClipboardExportAction("Copy Rows with Header", ExportingType.ROWS_WITH_HEADER, dataProvider, "Copy the whole row values including column names"));
        copyGroup.addSeparator();
        copyGroup.add(new ClipboardExportAction("Copy Only Columns", ExportingType.COLUMNS, dataProvider, "Copy the whole columns values"));
        copyGroup.add(new ClipboardExportAction("Copy Columns with Header", ExportingType.COLUMNS_WITH_HEADER, dataProvider, "Copy the whole columns values including column names"));
        group.add(copyGroup);

        group.addSeparator();
        group.add(new ExcelExportAction("Open in _Excel", ExportingType.ALL_WITH_HEADER, dataProvider, "Open current table in Excel or compatible application", false));

        group.addSeparator();
        final OpenInEditorAction open_in_editor = new OpenInEditorAction("_Open in Editor", dataProvider, "Open cell content in separate editor tab");
        group.add(open_in_editor);

        group.add(new FlipTableExportAction(dataProvider));

        group.addSeparator();
        final DefaultActionGroup exportGroup = new PopupActionGroup("Export Data _Into ...", KdbIcons.Console.Export);
        exportGroup.add(new CsvExportAction("CSV format", ExportingType.ALL_WITH_HEADER, dataProvider, "Export current table into Comma Separated File format"));
        exportGroup.add(new ExcelExportAction("Excel xls format", ExportingType.ALL_WITH_HEADER, dataProvider, "Export current table into Excel XLS format", true, null));
        exportGroup.add(new BinaryExportAction("KDB binary format", ExportingType.ALL_WITH_HEADER, dataProvider, "Binary KDB IPC file format. Can be imported directly into KDB."));
        group.add(exportGroup);

        group.addSeparator();
        final ActionGroup sendTo = new PopupActionGroup("Send Data Into ...", KdbIcons.Console.SendInto) {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return KdbConnectionManager.getManager(project).getConnections().stream().map(c -> new SendIntoAction(dataProvider, c)).toArray(AnAction[]::new);
            }
        };
        group.add(sendTo);

        return group;
    }

    JTable getTable();

    String getExportName();

    Object getNativeObject();
}
