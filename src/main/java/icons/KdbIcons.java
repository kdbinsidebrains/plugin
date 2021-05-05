package icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import com.intellij.ui.LayeredIcon;
import com.intellij.ui.RowIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class KdbIcons {
    public static final class Main {
        public static final Icon File = IconLoader.getIcon("/org/kdb/inside/brains/icons/q.svg", KdbIcons.class);
        public static final Icon Import = AllIcons.Nodes.Interface;
        public static final Icon Module = IconLoader.getIcon("/org/kdb/inside/brains/icons/q.svg", KdbIcons.class);
        public static final Icon Application = IconLoader.getIcon("/org/kdb/inside/brains/icons/q.svg", KdbIcons.class);
        public static final Icon Notification = IconLoader.getIcon("/org/kdb/inside/brains/icons/q.svg", KdbIcons.class);

        public static final Icon ToolWindow = IconLoader.getIcon("/org/kdb/inside/brains/icons/kdbInstancesTool.svg", KdbIcons.class);
    }

    public static final class Scope {
        public static final Icon Icon = AllIcons.Ide.LocalScope;
        public static final Icon Local = AllIcons.Ide.LocalScope;
        public static final Icon Shared = IconManager.getInstance().createLayered(AllIcons.Ide.LocalScope, AllIcons.Nodes.Shared);
    }

    public static final class Console {
        public static final Icon Kill = AllIcons.Debugger.KillProcess;

        public static final Icon Table = AllIcons.Nodes.DataTables;
        public static final Icon Console = AllIcons.Debugger.Console;

        public static final Icon ShowOnlyLast = AllIcons.Debugger.AddToWatch;

        public static final Icon SaveCSV = IconLoader.getIcon("/org/kdb/inside/brains/icons/exportCSV.svg", KdbIcons.class);
        public static final Icon SaveBinary = IconLoader.getIcon("/org/kdb/inside/brains/icons/exportBinary.svg", KdbIcons.class);

        public static final Icon ExportExcel = IconLoader.getIcon("/org/kdb/inside/brains/icons/exportExcel.svg", KdbIcons.class);
    }

    public static final class Node {
        public static final Icon Package = AllIcons.Nodes.Folder;
        public static final Icon Instance = IconLoader.getIcon("/org/kdb/inside/brains/icons/instance.svg", KdbIcons.class);

        public static final Icon InstanceQueryRunning = lineIcons(Instance, AllIcons.Actions.Execute);
        public static final Icon InstanceQueryCancelled = lineIcons(Instance, AllIcons.Actions.Suspend);

        public static final Icon NewPackage = AllIcons.Actions.NewFolder;
        public static final Icon NewInstance = IconLoader.getIcon("/org/kdb/inside/brains/icons/newInstance.svg", KdbIcons.class);

        public static final Icon keyword = AllIcons.Nodes.Constant;

        public static final Icon tablePublic = new RowIcon(AllIcons.Nodes.DataTables, AllIcons.Nodes.C_public);
        public static final Icon tablePrivate = new RowIcon(AllIcons.Nodes.DataTables, AllIcons.Nodes.C_private);

        public static final Icon functionPublic = new RowIcon(AllIcons.Nodes.Function, AllIcons.Nodes.C_public);
        public static final Icon functionPrivate = new RowIcon(AllIcons.Nodes.Function, AllIcons.Nodes.C_private);

        public static final Icon variablePublic = new RowIcon(AllIcons.Nodes.Variable, AllIcons.Nodes.C_public);
        public static final Icon variablePrivate = new RowIcon(AllIcons.Nodes.Variable, AllIcons.Nodes.C_private);

        public static final Icon ChangeColor = AllIcons.Actions.Colors;
        public static final Icon ShowConnectionFilter = AllIcons.Actions.Show;
    }

    public static final class Instance {
        public static final Icon Bind = IconLoader.getIcon("/org/kdb/inside/brains/icons/bind.svg", KdbIcons.class);

        public static final Icon Execute = AllIcons.Actions.Execute;
        public static final Icon ExecuteOn = AllIcons.Actions.Expandall;
        public static final Icon QuickExecute = AllIcons.Debugger.EvaluateExpression;

        public static final Icon Connected = IconLoader.getIcon("/org/kdb/inside/brains/icons/connected.svg", KdbIcons.class);
        public static final Icon Disconnected = IconLoader.getIcon("/org/kdb/inside/brains/icons/disconnected.svg", KdbIcons.class);
    }

    @NotNull
    private static LayeredIcon lineIcons(Icon icon1, Icon icon2) {
        LayeredIcon icon = new LayeredIcon(2);
        icon.setIcon(icon1, 0, 0, 0);
        icon.setIcon(icon2, 1, icon1.getIconWidth(), 0);
        return icon;
    }
}
