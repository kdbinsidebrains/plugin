package org.kdb.inside.brains.ide.sdk;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.ui.components.labels.ActionLink;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class KdbSdkPanel extends JPanel {
    private KdbSdkComboBox mySdkComboBox;

    private final boolean projectCreation;

    public KdbSdkPanel(boolean projectCreation) {
        super(new BorderLayout());
        this.projectCreation = projectCreation;
        initPanel();
    }

    private void initPanel() {
        mySdkComboBox = new KdbSdkComboBox(projectCreation);

        ActionLink myDownloadLink = new ActionLink("Download Kdb binaries", new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("https://kx.com/download/"));
                    } catch (Exception ignore) {
                        //
                    }
                }
            }
        });
        myDownloadLink.setBorder(JBUI.Borders.empty());

        final String label;
        if (projectCreation) {
            label = "Project SDK: ";
        } else {
            label = "Module SDK: ";
        }

        final var builder = FormBuilder.createFormBuilder();
        builder.setVerticalGap(10);
        builder.setHorizontalGap(0);

        final var component = new JTextPane();
        component.setText("" +
                "To be able to run Q code locally, you have to setup KDB SDK on the local PC. If it's not required, you can continue without SDK setup.\n\r" +
                "To setup new KDB SKD, select home folder of KDB binaries that usually is located at C:\\q for Windows or ~/q for Linux");
        component.setBorder(JBUI.Borders.empty());

        builder.addComponent(component);

        builder.addLabeledComponent(label, mySdkComboBox);
        builder.addComponent(myDownloadLink);

        add(builder.getPanel(), BorderLayout.NORTH);
    }

    public Sdk getSdk() {
        return mySdkComboBox.getSelectedSdk();
    }

    public JComponent getPreferredFocusedComponent() {
        return mySdkComboBox;
    }
}
