package org.kdb.inside.brains.view.console.table;

import com.intellij.ui.docking.DockContainer;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;

public interface TabsDragSession {
    @NotNull
    DockContainer.ContentResponse getResponse(MouseEvent e);

    void process(MouseEvent e);

    void cancel();
}
