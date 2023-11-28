package org.kdb.inside.brains.view;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import icons.KdbIcons;
import kotlin.Unit;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.ide.sdk.KdbSdkType;

import java.util.stream.Stream;

public class KdbToolWindowManager {
    private boolean enabled;

    public KdbToolWindowManager() {
        try {
            // If there is no KdbSdkType - it's not IDEA and the plugin is enabled by default.
            KdbSdkType.getInstance();
        } catch (Exception ex) {
            enabled = true;
        }
    }

    public static void enable(Project project) {
        project.getService(KdbToolWindowManager.class).enabled = true;

        final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);
        manager.invokeLater(() -> getKdbWindows().forEach(w -> {
            final ToolWindow window = manager.getToolWindow(w.id);
            if (window == null) {
                manager.registerToolWindow(w.id, b -> {
                    b.icon = IconLoader.findIcon(w.icon, KdbIcons.class);
                    b.anchor = ToolWindowAnchor.fromText(w.anchor);
                    b.canCloseContent = w.canCloseContents;
                    b.hideOnEmptyContent = false;
                    b.contentFactory = w.getToolWindowFactory(w.getPluginDescriptor());
                    b.shouldBeAvailable = b.contentFactory.shouldBeAvailable(project);
                    return Unit.INSTANCE;
                });
            }
        }));
    }

    public static void disable(Project project) {
        project.getService(KdbToolWindowManager.class).enabled = false;

        final ToolWindowManagerEx manager = ToolWindowManagerEx.getInstanceEx(project);
        manager.invokeLater(() -> getKdbWindows().forEach(w -> {
            final ToolWindow window = manager.getToolWindow(w.id);
            if (window != null) {
                window.remove();
            }
        }));
    }

    public static boolean isPluginEnabled(@Nullable Project project) {
        if (project == null) {
            return false;
        }
        final KdbToolWindowManager service = project.getService(KdbToolWindowManager.class);
        return service != null && service.enabled;
    }

    private static Stream<ToolWindowEP> getKdbWindows() {
        return Stream.of(ToolWindowEP.EP_NAME.getExtensions()).filter(e -> e.id.startsWith("KDB"));
    }
}
