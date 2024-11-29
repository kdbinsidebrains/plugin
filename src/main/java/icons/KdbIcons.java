package icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class KdbIcons {
    private static @NotNull Icon load(String path) {
        return IconLoader.getIcon(path, KdbIcons.class);
    }

    private static @NotNull Icon row(@NotNull Icon icon1, @NotNull Icon icon2) {
        return IconManager.getInstance().createRowIcon(icon1, icon2);
//        LayeredIcon icon = new LayeredIcon(2);
//        icon.setIcon(icon1, 0, 0, 0);
//        icon.setIcon(icon2, 1, icon1.getIconWidth(), 0);
//        return icon;
    }

    private static @NotNull Icon layer(@NotNull Icon icon1, @NotNull Icon icon2) {
        return IconManager.getInstance().createLayered(icon1, icon2);
    }

    public static final class Main {
        public static final @NotNull Icon File = load("/org/kdb/inside/brains/icons/qFile.svg");
        public static final @NotNull Icon Module = load("/org/kdb/inside/brains/icons/q.svg");
        public static final @NotNull Icon Library = load("/org/kdb/inside/brains/icons/q.svg");
        public static final @NotNull Icon Application = load("/org/kdb/inside/brains/icons/q.svg");
        public static final @NotNull Icon Notification = load("/org/kdb/inside/brains/icons/q.svg");

        public static final @NotNull Icon ToolWindow = load("/org/kdb/inside/brains/icons/windows/instances.svg");
        public static final @NotNull Icon ConsoleWindow = load("/org/kdb/inside/brains/icons/windows/console.svg");
        public static final @NotNull Icon InspectorWindow = load("/org/kdb/inside/brains/icons/windows/inspector.svg");
    }

    public static final class Scope {
        public static final @NotNull Icon Icon = AllIcons.Ide.LocalScope;
        public static final @NotNull Icon Local = AllIcons.Ide.LocalScope;
        public static final @NotNull Icon Shared = layer(AllIcons.Ide.LocalScope, AllIcons.Nodes.Shared);
    }

    public static final class Chart {
        public static final @NotNull Icon Icon = load("/org/kdb/inside/brains/icons/charting.svg");
        public static final @NotNull Icon Line = load("/org/kdb/inside/brains/icons/chart/line.svg");
        public static final @NotNull Icon Candlestick = load("/org/kdb/inside/brains/icons/chart/candlestick.svg");

        public static final @NotNull Icon TypeLine = load("/org/kdb/inside/brains/icons/chart/type_line.svg");
        public static final @NotNull Icon TypeSpline = load("/org/kdb/inside/brains/icons/chart/type_spline.svg");
        public static final @NotNull Icon TypeSteps = load("/org/kdb/inside/brains/icons/chart/type_steps.svg");
        public static final @NotNull Icon TypeArea = load("/org/kdb/inside/brains/icons/chart/type_area.svg");
        public static final @NotNull Icon TypeScatter = load("/org/kdb/inside/brains/icons/chart/type_scatter.svg");
        public static final @NotNull Icon TypeBar = load("/org/kdb/inside/brains/icons/chart/type_bar.svg");
        public static final @NotNull Icon TypeDiff = load("/org/kdb/inside/brains/icons/chart/type_diff.svg");

        public static final @NotNull Icon ToolMagnet = load("/org/kdb/inside/brains/icons/chart/magnet.svg");

        public static final @NotNull Icon ToolCrosshair = load("/org/kdb/inside/brains/icons/chart/tool_crosshair.svg");
        public static final @NotNull Icon ToolPoints = load("/org/kdb/inside/brains/icons/chart/tool_points.svg");
        public static final @NotNull Icon ToolMeasure = load("/org/kdb/inside/brains/icons/chart/tool_measure.svg");

        public static final @NotNull Icon Templates = load("/org/kdb/inside/brains/icons/chart/template.svg");
    }

    public static final class Inspector {
        public static final @NotNull Icon Refresh = AllIcons.Actions.Refresh;
    }

    public static final class Console {
        public static final @NotNull Icon Kill = AllIcons.Debugger.KillProcess;

        public static final @NotNull Icon Table = AllIcons.Nodes.DataTables;
        public static final @NotNull Icon Console = AllIcons.Debugger.Console;

        public static final @NotNull Icon ShowHistory = AllIcons.Debugger.AddToWatch;

        public static final @NotNull Icon CopyTable = load("/org/kdb/inside/brains/icons/console/copyTable.svg");
        public static final @NotNull Icon CopyValues = load("/org/kdb/inside/brains/icons/console/copyValues.svg");
        public static final @NotNull Icon CopySpecial = load("/org/kdb/inside/brains/icons/console/copySpecial.svg");

        public static final @NotNull Icon SendInto = AllIcons.Actions.Upload;
        public static final @NotNull Icon Export = AllIcons.ToolbarDecorator.Export;
        public static final @NotNull Icon ImportBinary = load("/org/kdb/inside/brains/icons/console/importBinary.svg");
        public static final @NotNull Icon ExportExcel = load("/org/kdb/inside/brains/icons/console/exportExcel.svg");

        public static final @NotNull Icon DelaySearchUpdate = load("/org/kdb/inside/brains/icons/console/delaySearch.svg");

        public static final @NotNull Icon Layout = AllIcons.Debugger.RestoreLayout;
        public static final @NotNull Icon LayoutNo = AllIcons.Actions.MoveToTopLeft;
        public static final @NotNull Icon LayoutDown = AllIcons.Actions.SplitVertically;
        public static final @NotNull Icon LayoutRight = AllIcons.Actions.SplitHorizontally;

        public static final @NotNull Icon OpenInEditor = AllIcons.General.FitContent;
        public static final @NotNull Icon FlipTable = load("/org/kdb/inside/brains/icons/console/flipTable.svg");
        public static final @NotNull Icon TableIndex = load("/org/kdb/inside/brains/icons/console/tableIndex.svg");
        public static final @NotNull Icon TableThousands = load("/org/kdb/inside/brains/icons/console/thousandsSeparator.svg");
        public static final @NotNull Icon TableScientific = load("/org/kdb/inside/brains/icons/console/scientificNotation.svg");

        public static final @NotNull Icon SelectAll = AllIcons.Actions.Selectall;
        public static final @NotNull Icon UnselectAll = AllIcons.Actions.Unselectall;

        public static final @NotNull Icon ColumnFilter = AllIcons.General.Filter;

        public static final @NotNull Icon UploadFile = load("/org/kdb/inside/brains/icons/console/uploadFile.svg");
        public static final @NotNull Icon DownloadFile = load("/org/kdb/inside/brains/icons/console/downloadFile.svg");

        public static final @NotNull Icon SearchCommaSeparator = load("/org/kdb/inside/brains/icons/console/searchCommaSeparator.svg");
        public static final @NotNull Icon SearchCommaSeparatorHovered = load("/org/kdb/inside/brains/icons/console/searchCommaSeparatorHovered.svg");
        public static final @NotNull Icon SearchCommaSeparatorSelected = load("/org/kdb/inside/brains/icons/console/searchCommaSeparatorSelected.svg");
    }

    public static final class Node {
        public static final @NotNull Icon PublicItem = AllIcons.Nodes.C_public;
        public static final @NotNull Icon PrivateItem = AllIcons.Nodes.C_private;

        public static final @NotNull Icon Package = AllIcons.Nodes.Folder;
        public static final @NotNull Icon Instance = load("/org/kdb/inside/brains/icons/instance.svg");

        public static final @NotNull Icon InstanceQueryRunning = row(Instance, AllIcons.Actions.Execute);
        public static final @NotNull Icon InstanceQueryCancelled = row(Instance, AllIcons.Actions.Suspend);

        public static final @NotNull Icon NewPackage = AllIcons.Actions.NewFolder;
        public static final @NotNull Icon NewInstance = load("/org/kdb/inside/brains/icons/newInstance.svg");

        public static final @NotNull Icon File = Main.File;
        public static final @NotNull Icon Import = load("/org/kdb/inside/brains/icons/nodes/import.svg");
        public static final @NotNull Icon Command = load("/org/kdb/inside/brains/icons/nodes/command.svg");
        public static final @NotNull Icon Context = load("/org/kdb/inside/brains/icons/nodes/context.svg");
        public static final @NotNull Icon Symbol = AllIcons.Nodes.Static;
        public static final @NotNull Icon Lambda = AllIcons.Nodes.Lambda;
        public static final @NotNull Icon LambdaPublic = row(Lambda, Node.PublicItem);
        public static final @NotNull Icon LambdaPrivate = row(Lambda, Node.PrivateItem);
        public static final @NotNull Icon Variable = AllIcons.Nodes.Variable;
        public static final @NotNull Icon VariablePublic = row(Variable, Node.PublicItem);
        public static final @NotNull Icon VariablePrivate = row(Variable, Node.PrivateItem);
        public static final @NotNull Icon Parameter = AllIcons.Nodes.Parameter;
        public static final @NotNull Icon Function = AllIcons.Nodes.Function;
        public static final @NotNull Icon Keyword = AllIcons.Nodes.Constant;
        public static final @NotNull Icon Namespace = AllIcons.Nodes.Package;

        public static final @NotNull Icon Table = AllIcons.Nodes.DataTables;
        public static final @NotNull Icon TablePublic = row(Table, Node.PublicItem);
        public static final @NotNull Icon TablePrivate = row(Table, Node.PrivateItem);
        public static final @NotNull Icon TableKeyColumn = load("/org/kdb/inside/brains/icons/nodes/keyColumn.svg");
        public static final @NotNull Icon TableValueColumn = load("/org/kdb/inside/brains/icons/nodes/valueColumn.svg");

        public static final @NotNull Icon ChangeColor = AllIcons.Actions.Colors;
        public static final @NotNull Icon ShowConnectionFilter = AllIcons.Actions.Show;

        public static final @NotNull Icon SystemNamespaces = AllIcons.Nodes.Private;

        public static final @NotNull Icon GroupTables = load("/org/kdb/inside/brains/icons/nodes/groupTables.svg");
        public static final @NotNull Icon GroupFunctions = load("/org/kdb/inside/brains/icons/nodes/groupFunctions.svg");
        public static final @NotNull Icon GroupVariables = load("/org/kdb/inside/brains/icons/nodes/groupVariables.svg");
    }

    public static final class Instance {
        public static final @NotNull Icon Bind = load("/org/kdb/inside/brains/icons/bind.svg");

        public static final @NotNull Icon Execute = AllIcons.Actions.Execute;
        public static final @NotNull Icon ExecuteOn = AllIcons.Actions.Expandall;
        public static final @NotNull Icon ExecuteContext = load("/org/kdb/inside/brains/icons/executeContext.svg");
        public static final @NotNull Icon ExecuteQuick = AllIcons.Debugger.EvaluateExpression;
        public static final @NotNull Icon ExecuteFile = load("/org/kdb/inside/brains/icons/executeFile.svg");

        public static final @NotNull Icon Connected = load("/org/kdb/inside/brains/icons/connected.svg");
        public static final @NotNull Icon Disconnected = load("/org/kdb/inside/brains/icons/disconnected.svg");
    }
}
