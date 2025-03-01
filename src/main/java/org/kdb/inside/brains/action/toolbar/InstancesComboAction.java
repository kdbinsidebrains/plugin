package org.kdb.inside.brains.action.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListItemDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.SingleSelectionModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.action.connection.CreateConnectionAction;
import org.kdb.inside.brains.core.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstancesComboAction extends ComboBoxAction implements CustomComponentAction {
    private JBPopup popup;
    private JComponent notificationComponent;
    private final CreateConnectionAction newConnectionAction;

    private static final String PANEL_EDITOR = "EDITOR";
    private static final String PANEL_BUTTON = "BUTTON";
    private static final Key<KdbInstance> ACTIVE_INSTANCE = Key.create("ActiveKdbInstance");

    public InstancesComboAction() {
        newConnectionAction = (CreateConnectionAction) ActionManager.getInstance().getAction("Kdb.NewConnection");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Presentation presentation = e.getPresentation();
        final Project project = e.getData(CommonDataKeys.PROJECT);

        try {
            if (project == null || project.isDisposed() || !project.isOpen()) {
                presentation.setEnabled(false);
                updatePresentation(null, presentation);
            } else {
                presentation.setEnabled(true);
                updatePresentation(project, presentation);
            }
        } catch (IndexNotReadyException e1) {
            presentation.setEnabled(false);
        }
    }

    private void updatePresentation(@Nullable Project project, @NotNull Presentation presentation) {
        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);
        if (manager == null) {
            presentation.setEnabled(false);
            return;
        }

        final InstanceConnection conn = manager.getActiveConnection();
        if (conn != null) {
            presentation.putClientProperty(ACTIVE_INSTANCE, conn.getInstance());

            presentation.setIcon(null);
            presentation.setDescription("");
            presentation.setText(generateName(conn.getInstance(), conn.getState()), false);
        } else {
            presentation.putClientProperty(ACTIVE_INSTANCE, null);

            final Presentation templatePresentation = newConnectionAction.getTemplatePresentation();
            presentation.setText("<html>" + templatePresentation.getText() + "</html>");
            presentation.setIcon(templatePresentation.getIcon());
            presentation.setDescription(templatePresentation.getDescription());
        }
    }

    @Override
    public @NotNull JComponent createCustomComponent(@NotNull final Presentation presentation, @NotNull String place) {
        final CardLayout layout = new CardLayout();
        final NonOpaquePanel panel = new NonOpaquePanel(layout);

        final JBTextField editor = new JBTextField("", 20) {
            @Override
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                Insets m = getMargin();
                dim.width = Math.max(dim.width, getFontMetrics(getFont()).stringWidth(getText()) + 10 + m.left + m.right);
                return dim;
            }
        };
        editor.setDragEnabled(true);

        editor.putClientProperty(DarculaUIUtil.COMPACT_PROPERTY, Boolean.TRUE);

        final Consumer<DataContext> selectionCallback = dataContext -> {
            updatePresentation(CommonDataKeys.PROJECT.getData(dataContext), presentation);
            layout.show(panel, PANEL_BUTTON);
        };

        final InstancesList list = new InstancesList(editor, selectionCallback);

        final ComboBoxButton button = new ComboBoxButton(presentation) {
            @Override
            public boolean isOpaque() {
                return false;
            }

            @Override
            protected boolean isArrowVisible(@NotNull Presentation presentation) {
                return list.getModel().getSize() != 0;
            }

            @Override
            public void showPopup() {
                final JBScrollPane content = new JBScrollPane(list);
                content.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                content.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

                popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, editor)
                        .setFocusable(false)
                        .setRequestFocus(false)
                        .setCancelKeyEnabled(false)
                        .setCancelOnClickOutside(false)
                        .setMayBeParent(false)
                        .setResizable(false)
                        .createPopup();

                updateEditorText(editor, presentation);
                updatePopupActions(this, list, "");

                layout.show(panel, PANEL_EDITOR);

                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
                    IdeFocusManager.getGlobalInstance().requestFocus(editor, true);
                    popup.showUnderneathOf(editor);
                });
            }
        };
//        button.setUI(new MainToolbarComboBoxButtonUI());

        // See https://youtrack.jetbrains.com/issue/IDEA-307709
        button.addPropertyChangeListener(evt -> {
            final String n = evt.getPropertyName();
            if ("UI".equals(n) || AbstractButton.TEXT_CHANGED_PROPERTY.equals(n)) {
                BasicHTML.updateRenderer(button, button.getText());
            }
        });

        final ComponentValidator componentValidator = new ComponentValidator(() -> {
        });

        // Move focus away from the editor
        editor.registerKeyboardAction(e -> layout.show(panel, PANEL_BUTTON), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_FOCUSED);
        editor.registerKeyboardAction(e -> {
            if (list.handleSelection()) {
                return;
            }

            final KdbInstance instance = KdbInstance.parseInstance(editor.getText().strip());
            if (instance == null) {
                componentValidator.updateInfo(new ValidationInfo("Please enter connection uri in format [`:]host:port[:credentials]", editor));
            } else {
                final DataContext dataContext = DataManager.getInstance().getDataContext(button);
                final Project project = CommonDataKeys.PROJECT.getData(dataContext);
                if (project != null) {
                    final KdbConnectionManager manager = KdbConnectionManager.getManager(project);
                    new CreateTemporalAction(instance, manager).performSelection();
                }
                selectionCallback.accept(dataContext);
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);

        editor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                layout.show(panel, PANEL_BUTTON);
                updateEditorText(editor, presentation);

                if (popup != null) {
                    popup.dispose();
                    popup = null;
                }
            }
        });
        editor.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                if (popup == null) {
                    return;
                }
                componentValidator.updateInfo(null);
                updatePopupActions(button, list, editor.getText().strip());

                panel.revalidate();
            }
        });

        presentation.addPropertyChangeListener(evt -> {
            if (ACTIVE_INSTANCE.toString().equals(evt.getPropertyName())) {
                updateEditorText(editor, presentation);

                final KdbInstance instance = (KdbInstance) evt.getNewValue();
                button.putClientProperty("JButton.backgroundColor", instance != null ? instance.getInheritedColor() : null);
            }
        });

        panel.add(button, PANEL_BUTTON);
        panel.add(editor, PANEL_EDITOR);

        panel.setBorder(JBUI.Borders.empty(0, 5, 0, 4));

        notificationComponent = panel;
        return panel;
    }

    private void updateEditorText(JBTextField editor, @NotNull Presentation presentation) {
        final KdbInstance instance = presentation.getClientProperty(ACTIVE_INSTANCE);
        editor.setText(instance == null ? "" : instance.toSymbol());
        editor.selectAll();
    }

    private void updatePopupActions(@NotNull ComboBoxButton button, @NotNull InstancesList list, @NotNull String filter) {
        list.setActionGroup(createPopupActionGroup(filter, DataManager.getInstance().getDataContext(button)));

        final int size = Math.max(2, list.getItemsCount());
        list.setVisibleRowCount(Math.min(size, 30));
        popup.pack(true, size < 30);
    }

    protected @NotNull DefaultActionGroup createPopupActionGroup(@NotNull String filter, DataContext dataContext) {
        final Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return new DefaultActionGroup();
        }

        final DefaultActionGroup group = new DefaultActionGroup();
        final KdbConnectionManager manager = KdbConnectionManager.getManager(project);

        final String[] tokens = Stream.of(filter.split(" ")).map(String::strip).filter(s -> !s.isBlank()).map(String::toLowerCase).toArray(String[]::new);

        final DefaultActionGroup connected = new DefaultActionGroup("Connected", false);
        final DefaultActionGroup disconnected = new DefaultActionGroup("Disconnected", false);

        final List<InstanceConnection> connections = manager.getConnections();
        for (InstanceConnection connection : connections) {
            if (tokens.length != 0 && !isValidInstance(connection.getInstance(), tokens)) {
                continue;
            }
            (connection.isConnected() ? connected : disconnected).add(new SelectInstanceAction(connection, manager));
        }
        group.add(connected);
        group.add(disconnected);

        if (tokens.length != 0) {
            final List<KdbScope> scopes = KdbScopesManager.getManager(project).getScopes();
            for (KdbScope scope : scopes) {
                final DefaultActionGroup g = new DefaultActionGroup("Scope " + scope.getName(), false);

                final List<SelectInstanceAction> collect = collectAllInstances(scope, new ArrayList<>()).stream()
                        .filter(i -> isValidInstance(i, tokens))
                        .map(i -> new SelectInstanceAction(i, manager))
                        .collect(Collectors.toList());

                if (collect.isEmpty()) {
                    final KdbInstance instance = KdbInstance.parseInstance(filter);
                    if (instance != null) {
                        g.add(new CreateTemporalAction(instance, scope, manager));
                    }
                } else {
                    g.addAll(collect);
                }
                group.add(g);
            }
        }
        return group;
    }

    private List<KdbInstance> collectAllInstances(StructuralItem folder, List<KdbInstance> instances) {
        for (InstanceItem instanceItem : folder) {
            if (instanceItem instanceof KdbInstance) {
                instances.add((KdbInstance) instanceItem);
            } else if (instanceItem instanceof StructuralItem) {
                collectAllInstances((StructuralItem) instanceItem, instances);
            }
        }
        return instances;
    }

    private boolean isValidInstance(KdbInstance instance, String[] tokens) {
        final String details = instance.toSymbol().toLowerCase();
        final String canonicalName = instance.getCanonicalName().toLowerCase();
        for (String token : tokens) {
            if (!canonicalName.contains(token) && !details.contains(token)) {
                return false;
            }
        }
        return true;
    }

    public JComponent getNotificationComponent() {
        return notificationComponent;
    }

    public static InstancesComboAction getInstance() {
        return (InstancesComboAction) ActionManager.getInstance().getAction("Kdb.Instances.QuickSelection");
    }

    @NotNull
    private static String generateName(KdbInstance instance, InstanceState state) {
        final KdbScope scope = instance.getScope();
        final boolean b = state == InstanceState.CONNECTED;
        final StructuralItem parent = instance.getParent();
        final String path = scope == null && parent == null ? "" : "<font color=\"gray\">" + (parent != null ? parent.getCanonicalName() : scope.getName()) + "</font>";
        return "<html>" + (b ? "<b>" : "<font color=\"dark-grey\">") + instance.getName() + (b ? "</b>" : "</font>") + " [" + instance.toSymbol() + "] " + path + "</html>";
    }

    private interface ItemSelectionAction {
        void performSelection();
    }

    private static class CreateTemporalAction extends EdtAction implements ItemSelectionAction {
        private final KdbScope scope;
        private final KdbInstance instance;
        private final KdbConnectionManager manager;

        private CreateTemporalAction(KdbInstance instance, KdbConnectionManager manager) {
            this.instance = instance;
            this.manager = manager;
            this.scope = null;
        }

        private CreateTemporalAction(KdbInstance instance, KdbScope scope, KdbConnectionManager manager) {
            super("Create in the '" + scope.getName() + "' scope");
            this.instance = instance;
            this.scope = scope;
            this.manager = manager;
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(true);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            performSelection();
        }

        @Override
        public void performSelection() {
            manager.activate(manager.createTempInstance(instance, scope));
        }
    }

    private static class SelectInstanceAction extends EdtAction implements ItemSelectionAction {
        private final KdbInstance instance;
        private final KdbConnectionManager manager;

        private SelectInstanceAction(InstanceConnection connection, KdbConnectionManager manager) {
            this(connection.getInstance(), manager);
        }

        private SelectInstanceAction(KdbInstance instance, KdbConnectionManager manager) {
            this.instance = instance;
            this.manager = manager;

            final InstanceState state = manager.getInstanceState(instance);

            final Presentation p = getTemplatePresentation();
            p.setText(generateName(instance, state));
            p.setDescription(instance.toSymbol());
            if (state == InstanceState.CONNECTED) {
                p.setIcon(KdbIcons.Instance.Connected);
            }
        }

        @Override
        public void performSelection() {
            manager.activate(instance);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            performSelection();
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    private static class InstancesList extends JBList<AnAction> implements ListItemDescriptor<AnAction> {
        private int myLastSelectedIndex = -2;
        private Point myLastMouseLocation;

        private final Consumer<DataContext> selectionCallback;
        private final Map<AnAction, String> captionItems = new HashMap<>();

        public InstancesList(JBTextField editor, Consumer<DataContext> selectionCallback) {
            this.selectionCallback = selectionCallback;

            setBorder(new EmptyBorder(UIUtil.getListViewportPadding()));
            setAutoscrolls(true);
            ScrollingUtil.installActions(this, editor);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setCellRenderer(new GroupedItemsListRenderer<>(this));
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            setEmptyText("Type an instance URI or search mask");
        }

        void setActionGroup(DefaultActionGroup group) {
            captionItems.clear();
            myLastSelectedIndex = -2;
            myLastMouseLocation = null;
            setSelectionModel(new SingleSelectionModel()); // clear selection model here

            final List<AnAction> items = new ArrayList<>();
            for (AnAction action : group.getChildActionsOrStubs()) {
                if (action instanceof DefaultActionGroup actionGroup) {
                    final AnAction[] children = actionGroup.getChildActionsOrStubs();
                    if (children.length != 0) {
                        captionItems.put(children[0], actionGroup.getTemplatePresentation().getText());
                        for (int i = 0; i < children.length; i++) {
                            AnAction child = children[i];
                            if (child instanceof Separator) {
                                if (i < children.length - 1) {
                                    captionItems.put(children[i + 1], null);
                                }
                            } else {
                                items.add(child);
                            }
                        }
                    }
                } else {
                    items.add(action);
                }
            }

            setModel(new AbstractListModel<>() {
                @Override
                public int getSize() {
                    return items.size();
                }

                @Override
                public AnAction getElementAt(int index) {
                    return items.get(index);
                }
            });
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            Insets insets = getInsets();
            int dx = insets.left + insets.right;
            int dy = insets.top + insets.bottom;

            final int rows = getModel().getSize();
            if (rows == 0) {
                final Dimension ps = getEmptyText().getPreferredSize();
                return new Dimension(ps.width + dx + 20, ps.height + dy + 20);
            } else {
                int visibleRowCount = getVisibleRowCount();
                Rectangle r = getCellBounds(0, Math.min(visibleRowCount, rows) - 1);
                int width = getPreferredSize().width;
                int height = r != null ? r.height + dy : 1;
                return new Dimension(width, height);
            }
        }

        @Override
        public @Nullable String getTextFor(AnAction value) {
            return value.getTemplatePresentation().getText();
        }

        @Override
        public @Nullable String getTooltipFor(AnAction value) {
            return value.getTemplatePresentation().getDescription();
        }

        @Override
        public @Nullable Icon getIconFor(AnAction value) {
            final Icon icon = value.getTemplatePresentation().getIcon();
            return icon == null ? AllIcons.Nodes.EmptyNode : icon;
        }

        @Override
        public Icon getSelectedIconFor(AnAction value) {
            return getIconFor(value);
        }

        @Override
        public boolean hasSeparatorAboveOf(AnAction value) {
            return captionItems.containsKey(value);
        }

        @Override
        public @Nullable String getCaptionAboveOf(AnAction value) {
            return captionItems.get(value);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_RELEASED && isActionClick(e)) {
                IdeEventQueue.getInstance().blockNextEvents(e);
                if (handleSelection()) {
                    e.consume();
                } else {
                    super.processMouseEvent(e);
                }
            } else {
                super.processMouseEvent(e);
            }
        }

        @Override
        protected void processMouseMotionEvent(MouseEvent e) {
            if (e.getID() == MouseEvent.MOUSE_MOVED && isMouseMoved(e.getLocationOnScreen())) {
                Point point = e.getPoint();
                int index = locationToIndex(point);

                if (index != myLastSelectedIndex) {
                    if (!UIUtil.isSelectionButtonDown(e) && getSelectedIndices().length <= 1) {
                        setSelectedIndex(index);
                    }
                    myLastSelectedIndex = index;
                }
            } else {
                super.processMouseMotionEvent(e);
            }
        }

        private boolean isMouseMoved(Point location) {
            if (myLastMouseLocation == null) {
                myLastMouseLocation = location;
                return false;
            }
            Point prev = myLastMouseLocation;
            myLastMouseLocation = location;
            return !prev.equals(location);
        }

        private boolean handleSelection() {
            final AnAction selectedValue = getSelectedValue();
            if (selectedValue instanceof ItemSelectionAction action) {
                action.performSelection();
                selectionCallback.accept(DataManager.getInstance().getDataContext(this));
                return true;
            }
            return false;
        }

        protected boolean isActionClick(MouseEvent e) {
            return UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED, true);
        }
    }
}