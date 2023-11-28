package org.kdb.inside.brains.view;

import com.intellij.openapi.ui.SimpleToolWindowPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class KdbToolWindowPanel extends SimpleToolWindowPanel {
    public KdbToolWindowPanel(boolean vertical) {
        super(vertical);
    }

    @Override
    public void setToolbar(@Nullable JComponent c) {
        // See https://youtrack.jetbrains.com/issue/IDEA-339144 about the issue
        final JComponent toolbar = getToolbar();
        if (toolbar != null) {
            remove(toolbar);
        }
        super.setToolbar(c);
    }
}