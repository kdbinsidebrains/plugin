package icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class KdbIcons {
    private static Icon load(String path) {
        return IconLoader.getIcon(path, KdbIcons.class);
    }

    public static final class Main {
        public static final Icon File = load("/org/kdb/inside/brains/icons/qFile.svg");
        public static final Icon Module = load("/org/kdb/inside/brains/icons/q.svg");
        public static final Icon Application = load("/org/kdb/inside/brains/icons/q.svg");
        public static final Icon Notification = load("/org/kdb/inside/brains/icons/q.svg");

        public static final Icon ToolWindow = load("/org/kdb/inside/brains/icons/windows/instances.svg");
        public static final Icon ConsoleWindow = load("/org/kdb/inside/brains/icons/windows/console.svg");
    }

    public static final class Scope {
        public static final Icon Icon = AllIcons.Ide.LocalScope;
        public static final Icon Local = AllIcons.Ide.LocalScope;
        public static final Icon Shared = IconManager.getInstance().createLayered(AllIcons.Ide.LocalScope, AllIcons.Nodes.Shared);
    }

    public static final class Chart {
        public static final Icon Icon = load("/org/kdb/inside/brains/icons/charting.svg");
        public static final Icon Line = load("/org/kdb/inside/brains/icons/chart/line.svg");
        public static final Icon Candlestick = load("/org/kdb/inside/brains/icons/chart/candlestick.svg");

        public static final Icon TypeLine = load("/org/kdb/inside/brains/icons/chart/type_line.svg");
        public static final Icon TypeSpline = load("/org/kdb/inside/brains/icons/chart/type_spline.svg");
        public static final Icon TypeSteps = load("/org/kdb/inside/brains/icons/chart/type_steps.svg");
        public static final Icon TypeArea = load("/org/kdb/inside/brains/icons/chart/type_area.svg");
        public static final Icon TypeScatter = load("/org/kdb/inside/brains/icons/chart/type_scatter.svg");
        public static final Icon TypeBar = load("/org/kdb/inside/brains/icons/chart/type_bar.svg");
        public static final Icon TypeDiff = load("/org/kdb/inside/brains/icons/chart/type_diff.svg");

        public static final Icon ToolCrosshair = load("/org/kdb/inside/brains/icons/chart/tool_crosshair.svg");
        public static final Icon ToolPoints = load("/org/kdb/inside/brains/icons/chart/tool_points.svg");
        public static final Icon ToolMeasure = load("/org/kdb/inside/brains/icons/chart/tool_measure.svg");
    }

    public static final class Console {
        public static final Icon Kill = AllIcons.Debugger.KillProcess;

        public static final Icon Table = AllIcons.Nodes.DataTables;
        public static final Icon Console = AllIcons.Debugger.Console;

        public static final Icon ShowOnlyLast = AllIcons.Debugger.AddToWatch;

        public static final Icon CopyTable = load("/org/kdb/inside/brains/icons/copyTable.svg");
        public static final Icon CopyValues = load("/org/kdb/inside/brains/icons/copyValues.svg");
        public static final Icon CopySpecial = load("/org/kdb/inside/brains/icons/copySpecial.svg");

        public static final Icon SendInto = AllIcons.Actions.Upload;
        public static final Icon Export = AllIcons.ToolbarDecorator.Export;
        public static final Icon ImportBinary = load("/org/kdb/inside/brains/icons/importBinary.svg");
        public static final Icon ExportExcel = load("/org/kdb/inside/brains/icons/exportExcel.svg");
    }

    public static final class Node {
        public static final Icon Package = AllIcons.Nodes.Folder;
        public static final Icon Instance = load("/org/kdb/inside/brains/icons/instance.svg");

        public static final Icon InstanceQueryRunning = lineIcons(Instance, AllIcons.Actions.Execute);
        public static final Icon InstanceQueryCancelled = lineIcons(Instance, AllIcons.Actions.Suspend);

        public static final Icon NewPackage = AllIcons.Actions.NewFolder;
        public static final Icon NewInstance = load("/org/kdb/inside/brains/icons/newInstance.svg");


        public static final Icon File = Main.File;
        public static final Icon Import = load("/org/kdb/inside/brains/icons/nodes/import.svg");
        public static final Icon Command = load("/org/kdb/inside/brains/icons/nodes/command.svg");
        public static final Icon Context = load("/org/kdb/inside/brains/icons/nodes/context.svg");
        public static final Icon Symbol = AllIcons.Nodes.Static;
        public static final Icon Lambda = AllIcons.Nodes.Lambda;
        public static final Icon Variable = AllIcons.Nodes.Variable;
        public static final Icon Function = AllIcons.Nodes.Function;
        public static final Icon Keyword = AllIcons.Nodes.Constant;

        public static final Icon Table = AllIcons.Nodes.DataTables;
        public static final Icon TableKeyColumn = load("/org/kdb/inside/brains/icons/nodes/keyColumn.svg");
        public static final Icon TableValueColumn = load("/org/kdb/inside/brains/icons/nodes/valueColumn.svg");

        public static final Icon ChangeColor = AllIcons.Actions.Colors;
        public static final Icon ShowConnectionFilter = AllIcons.Actions.Show;

    }

    public static final class Instance {
        public static final Icon Bind = load("/org/kdb/inside/brains/icons/bind.svg");

        public static final Icon Execute = AllIcons.Actions.Execute;
        public static final Icon ExecuteOn = AllIcons.Actions.Expandall;
        public static final Icon QuickExecute = AllIcons.Debugger.EvaluateExpression;

        public static final Icon Connected = load("/org/kdb/inside/brains/icons/connected.svg");
        public static final Icon Disconnected = load("/org/kdb/inside/brains/icons/disconnected.svg");
    }

    @NotNull
    private static LayeredIcon lineIcons(Icon icon1, Icon icon2) {
        LayeredIcon icon = new LayeredIcon(2);
        icon.setIcon(icon1, 0, 0, 0);
        icon.setIcon(icon2, 1, icon1.getIconWidth(), 0);
        return icon;
    }
}
