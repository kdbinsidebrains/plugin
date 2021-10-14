package org.kdb.inside.brains.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.InstanceState;
import org.kdb.inside.brains.core.KdbConnectionManager;
import org.kdb.inside.brains.core.KdbInstance;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @deprecated replaced by {@link InstancesComboAction}
 */
@Deprecated
public class ButtonInstancesToolbarAction extends ComboBoxAction {
    private ComboBoxButton myButton;
    private final CreateConnectionAction newConnectionAction;

    private static final Key<Boolean> BUTTON_MODE = Key.create("ButtonMode");
    private static final Key<Color> BACKGROUND_COLOR = Key.create("BackgroundColor");

    public ButtonInstancesToolbarAction() {
        newConnectionAction = (CreateConnectionAction) ActionManager.getInstance().getAction("Kdb.NewConnection");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (ActionPlaces.TOUCHBAR_GENERAL.equals(e.getPlace())) {
            final Presentation presentation = e.getPresentation();
            if (Boolean.TRUE.equals(presentation.getClientProperty(BUTTON_MODE))) {
                newConnectionAction.actionPerformed(e);
                return;
            }
        }
        super.actionPerformed(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Project project = e.getData(CommonDataKeys.PROJECT);

        if (ActionPlaces.isMainMenuOrActionSearch(e.getPlace())) {
            presentation.setDescription("Open connected Kdb instances");
        }

        try {
            if (project == null || project.isDisposed() || !project.isOpen()) {
                presentation.setEnabled(false);
                updatePresentation(null, presentation, e.getPlace());
            } else {
                presentation.setEnabled(true);
                updatePresentation(project, presentation, e.getPlace());
            }
        } catch (IndexNotReadyException e1) {
            presentation.setEnabled(false);
        }
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }

    @NotNull
    @Override
    public JComponent createCustomComponent(@NotNull final Presentation presentation, @NotNull String place) {
        myButton = new ComboBoxButton(presentation) {
            @Override
            protected void fireActionPerformed(ActionEvent event) {
                if (isNormalMode(presentation)) {
                    super.fireActionPerformed(event);
                } else {
                    final DataContext dataContext = getDataContext();
                    if (dataContext != null) {
                        // TODO: Commented
//                        newConnectionAction.createNewInstance(dataContext);
                    }
                }
            }

            @Override
            public boolean isOpaque() {
                return true;
            }

            @Override
            public void paint(Graphics g) {
                final Color c = presentation.getClientProperty(BACKGROUND_COLOR);
                if (c != null) {
                    g.setColor(c);
                }

                super.paint(g);
            }

            @Override
            protected boolean isArrowVisible(@NotNull Presentation presentation) {
                return isNormalMode(presentation);
            }


            private boolean isNormalMode(Presentation presentation) {
                return !Boolean.TRUE.equals(presentation.getClientProperty(BUTTON_MODE));
            }
        };

        presentation.addPropertyChangeListener(evt -> {
            if ("BackgroundColor".equals(evt.getPropertyName())) {
                myButton.putClientProperty("JButton.backgroundColor", evt.getNewValue());
            }
        });

        final NonOpaquePanel panel = new NonOpaquePanel(new BorderLayout());
        final Border border = UIUtil.isUnderDefaultMacTheme() ?
                JBUI.Borders.empty(0, 2) : JBUI.Borders.empty(0, 5, 0, 4);


        panel.setBorder(border);
        panel.add(myButton);
        return panel;
    }

    private void updatePresentation(@Nullable Project project, @NotNull Presentation presentation, String actionPlace) {
        presentation.putClientProperty(BUTTON_MODE, null);
        presentation.putClientProperty(BACKGROUND_COLOR, null);

        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);
        if (manager == null) {
            presentation.setEnabled(false);
            return;
        }

        final InstanceConnection conn = manager.getActiveConnection();
        if (conn != null) {
            presentation.setIcon(null);
            presentation.setDescription("");
            presentation.setText(generateName(conn), false);

            presentation.putClientProperty(BACKGROUND_COLOR, conn.getInstance().getInheritedColor());
        } else {
            presentation.putClientProperty(BUTTON_MODE, Boolean.TRUE);

            final Presentation templatePresentation = newConnectionAction.getTemplatePresentation();
            presentation.setText(templatePresentation.getText());
            presentation.setIcon(templatePresentation.getIcon());
            presentation.setDescription(templatePresentation.getDescription());
        }
    }

    public JComponent getNotificationComponent() {
        return myButton;
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(JComponent button, @NotNull DataContext dataContext) {
        final DefaultActionGroup group = new DefaultActionGroup();

        final Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return group;
        }

        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);

        group.add(newConnectionAction);
        final DefaultActionGroup connected = new DefaultActionGroup("Connected Instances", false);
        final DefaultActionGroup disconnected = new DefaultActionGroup("Disconnected Instances", false);

        final List<InstanceConnection> connections = manager.getConnections();
        for (InstanceConnection connection : connections) {
            final KdbInstance instance = connection.getInstance();
            final DumbAwareAction action = new DumbAwareAction(generateName(connection), instance.toString(), null) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e) {
                    manager.activate(instance);
                }
            };
            if (connection.getState() == InstanceState.CONNECTED) {
                connected.add(action);
                action.getTemplatePresentation().setIcon(KdbIcons.Instance.Connected);
            } else {
                disconnected.add(action);
                action.getTemplatePresentation().setIcon(KdbIcons.Instance.Disconnected);
            }
        }

        group.addSeparator("Connected");
        group.add(connected);
        group.addSeparator("Disconnected");
        group.add(disconnected);

        return group;
    }

    @NotNull
    private String generateName(InstanceConnection connection) {
        final KdbInstance instance = connection.getInstance();
        final boolean b = connection.getState() == InstanceState.CONNECTED;
        final String s = instance.getParent() == null ? "" : "<font color=\"gray\">" + instance.getParent().getCanonicalName() + "</font>";
        return "<html>" + (b ? "<strong>" : "<font color=\"dark-grey\">") + instance.getName() + (b ? "</strong>" : "</font>") + " [" + instance.toSymbol() + "] " + s + "</html>";
    }

    @NotNull
    @Override
    protected DefaultActionGroup createPopupActionGroup(JComponent button) {
        throw new UnsupportedOperationException("This function is not in use. See with DataContext");
    }
}
