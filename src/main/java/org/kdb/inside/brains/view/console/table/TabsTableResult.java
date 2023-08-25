package org.kdb.inside.brains.view.console.table;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.AbstractPainter;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeGlassPaneUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockableContent;
import com.intellij.ui.docking.DragSession;
import com.intellij.ui.tabs.*;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.GraphicsUtil;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.action.EdtAction;
import org.kdb.inside.brains.core.KdbQuery;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TabsTableResult extends NonOpaquePanel implements DockContainer, Disposable {
    private TabInfo tableTab;
    private TabInfo consoleTab;

    private final JBTabsEx tabs;
    private Image myCurrentOverImg;
    private TabInfo myCurrentOverInfo;
    private MyDropAreaPainter myCurrentPainter;
    private Disposable myGlassPaneListenersDisposable = Disposer.newDisposable();
    private final Project project;
    private final AnAction renameAction;
    private final CopyOnWriteArraySet<Listener> listeners = new CopyOnWriteArraySet<>();
    // Docking
    private JBTabs myCurrentOver;

    public TabsTableResult(Project project, Disposable parent) {
        this.project = project;

        tabs = (JBTabsEx) JBTabsFactory.createTabs(project, this);
        // We can't use Supplier here as it's been Getter before and some versions are not compatiable anymore.
        tabs.setPopupGroup(new ActionGroup() {
            @Override
            public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
                return buildTabsPopupGroup().getChildren(e);
            }
        }, "KdbConsoleTabsMenu", true);

        renameAction = new RenameTabAction();

        tabs.addTabMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
                    IdeEventQueue.getInstance().blockNextEvents(e);
                    closeTab(tabs.findInfo(e));
                } else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    ActionManager.getInstance().tryToExecute(renameAction, e, e.getComponent(), "", true);
                }
            }
        });

        tabs.getPresentation().setTabDraggingEnabled(true);

        tabs.addListener(new TabsListener() {
            @Override
            public void selectionChanged(TabInfo oldSelection, TabInfo newSelection) {
                TabsListener.super.selectionChanged(oldSelection, newSelection);
            }

            @Override
            public void tabRemoved(@NotNull TabInfo tabToRemove) {
                TabsListener.super.tabRemoved(tabToRemove);
            }
        });

        TabsDockingManager.getInstance(project).register(this, parent);

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, tabs.getComponent());

        Disposer.register(parent, this);
    }

    public void showConsole(TabInfo consoleTab) {
        if (this.consoleTab == null) {
            this.consoleTab = consoleTab;
            insertNewTab(consoleTab, 0);
        }
    }

    public void hideConsole() {
        if (this.consoleTab != null) {
            tabs.removeTab(consoleTab);
            this.consoleTab = null;
        }
    }

    public void selectConsole() {
        if (consoleTab != null && tabs.getSelectedInfo() != consoleTab) {
            tabs.select(consoleTab, false);
        }
    }

    @Override
    public void addListener(@NotNull Listener listener, Disposable parent) {
        listeners.add(listener);
        Disposer.register(parent, () -> listeners.remove(listener));
    }

    private @NotNull ActionGroup buildTabsPopupGroup() {
        final TabInfo info = tabs.getTargetInfo();
        if (info == null || info == consoleTab) {
            return ActionGroup.EMPTY_GROUP;
        }

        return new DefaultActionGroup(renameAction);
    }

    public int getTabCount() {
        return tabs.getTabCount();
    }

    public TabInfo getSelectedInfo() {
        return tabs.getSelectedInfo();
    }

    @Override
    public void dispose() {
        closeAll();
    }

    @Override
    public void closeAll() {
        for (TabInfo tab : tabs.getTabs()) {
            tabs.removeTab(tab);
            fireContentClosed(tab);
        }
    }

    @Override
    public @NotNull RelativeRectangle getAcceptArea() {
        return new RelativeRectangle(tabs.getComponent());
    }

    @Override
    public @NotNull ContentResponse getContentResponse(@NotNull DockableContent<?> content, RelativePoint point) {
        JBTabsEx tabs = (JBTabsEx) getTabsAt(content, point);
        if (tabs == null || tabs.getPresentation().isHideTabs()) {
            return ContentResponse.DENY;
        }
        return ContentResponse.ACCEPT_MOVE;
    }

    public static TabsTableResult findParentTabs(Component component) {
        Component p = component;
        while (p != null && !(p instanceof TabsTableResult)) {
            p = p.getParent();
        }
        return (TabsTableResult) p;
    }

    @Override
    public void add(@NotNull DockableContent<?> content, RelativePoint dropTarget) {
        if (!(content instanceof TableResultContent trc)) {
            return;
        }

        final TabInfo info = trc.getKey();
        final TableResultView view = (TableResultView) info.getObject();

        final int dropInfoIndex = tabs.getDropInfoIndex();

        insertNewTab(createResultTabInfo(info.getText(), view), dropInfoIndex);
    }

    @Override
    public boolean isEmpty() {
        return tabs.getTabCount() == 0;
    }

    @Override
    public boolean isDisposeWhenEmpty() {
        return true;
    }

    @Override
    public @Nullable Image processDropOver(@NotNull DockableContent<?> content, RelativePoint point) {
        JBTabs current = getTabsAt(content, point);

        if (myCurrentOver != null && myCurrentOver != current) {
            resetDropOver(content);
        }

        if (myCurrentOver == null && current != null) {
            myCurrentOver = current;
            Presentation presentation = content.getPresentation();
            myCurrentOverInfo = new TabInfo(new JLabel("")).setText(presentation.getText()).setIcon(presentation.getIcon());
            myCurrentOverImg = myCurrentOver.startDropOver(myCurrentOverInfo, point);
        }

        if (myCurrentOver != null) {
            myCurrentOver.processDropOver(myCurrentOverInfo, point);
        }
        if (myCurrentPainter == null) {
            myCurrentPainter = new MyDropAreaPainter();
            myGlassPaneListenersDisposable = Disposer.newDisposable("GlassPaneListeners");
            Disposer.register(this, myGlassPaneListenersDisposable);
            IdeGlassPaneUtil.find(myCurrentOver.getComponent()).addPainter(myCurrentOver.getComponent(), myCurrentPainter, myGlassPaneListenersDisposable);
        }
        myCurrentPainter.processDropOver();
        return myCurrentOverImg;
    }

    @Override
    public void resetDropOver(@NotNull DockableContent<?> content) {
        if (myCurrentOver != null) {
            myCurrentOver.resetDropOver(myCurrentOverInfo);
            myCurrentOver = null;
            myCurrentOverInfo = null;
            myCurrentOverImg = null;

            Disposer.dispose(myGlassPaneListenersDisposable);
            myGlassPaneListenersDisposable = Disposer.newDisposable();
            myCurrentPainter = null;
        }
    }

    @Override
    public @NotNull JComponent getContainerComponent() {
        return this;
    }

    private @Nullable JBTabs getTabsAt(DockableContent<?> content, RelativePoint point) {
        if (content instanceof TableResultContent) {
            final Point p = point.getPoint(tabs.getComponent());
            Component c = SwingUtilities.getDeepestComponentAt(tabs.getComponent(), p.x, p.y);
            while (c != null) {
                if (c instanceof JBTabs) {
                    return (JBTabs) c;
                }
                c = c.getParent();
            }
        }
        return null;
    }

    public void showTab(String name, TableResult tableResult) {
        showTab(name, tableResult, -1);
    }

    public void showTab(String name, TableResult result, int index) {
        showTab(name, result, TableMode.NORMAL, index);
    }

    public void showTab(String name, TableResult result, TableMode mode, int index) {
        insertNewTab(createResultTabInfo(name, result, mode, null), index);
    }

    public void showTabAfter(String name, TableResult result) {
        showTabAfter(name, result, TableMode.NORMAL);
    }

    public void showTabAfter(String name, TableResult result, TableMode mode) {
        int index = -1;
        final TabInfo selectedInfo = tabs.getSelectedInfo();
        if (selectedInfo != null) {
            index = tabs.getIndexOf(selectedInfo) + 1;
        }
        showTab(name, result, mode, index);
    }

    private void insertNewTab(TabInfo info, int index) {
        tabs.addTab(info, index);
        fireContentOpen(info);
        tabs.select(info, false);
    }

    @NotNull
    private TabInfo createResultTabInfo(String name, TableResultView tableResultView) {
        final TabInfo info = new TabInfo(tableResultView);
        info.setText(name);
        info.setObject(tableResultView);

        info.setIcon(KdbIcons.Console.Table);
        info.setPreferredFocusableComponent(tableResultView.getFocusableComponent());

        info.setDragOutDelegate(new MyDragOutDelegate());

        final AnAction closeAction = new EdtAction("Close", "Close current result tab", AllIcons.Actions.Close) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                closeTab(info);
            }
        };
        closeAction.getTemplatePresentation().setHoveredIcon(AllIcons.Actions.CloseHovered);
        info.setTabLabelActions(new DefaultActionGroup(closeAction), "KdbConsolePanel");

        return info;
    }

    public void clearTableResult() {
        updateTableResult(null, null, null);
    }

    public void updateTableResult(TableResult tableResult, TableResultView resultView, BiConsumer<KdbQuery, TableResultView> repeater) {
        if (resultView != null) {
            resultView.showResult(tableResult);

            final TabInfo info = tabs.findInfo(resultView);
            if (info != null) {
                tabs.select(info, false);
            }
        } else {
            if (tableTab != null) {
                ((TableResultView) tableTab.getObject()).showResult(tableResult);
                tabs.select(tableTab, false);
            } else if (tableResult != null) {
                tableTab = createResultTabInfo("Table Result", tableResult, TableMode.NORMAL, repeater);
                insertNewTab(tableTab, consoleTab != null ? 1 : 0);
            }
        }
    }

    private void closeTab(TabInfo info) {
        if (info == null || !(info.getObject() instanceof TableResultView)) {
            return;
        }
        if (tableTab == info) {
            tableTab = null;
        }

        tabs.removeTab(info);
        fireContentClosed(info);
    }

    private void fireContentOpen(@NotNull TabInfo info) {
        for (Listener each : listeners) {
            each.contentAdded(info.getObject());
        }
    }

    protected void fireContentClosed(@NotNull TabInfo info) {
        for (Listener each : listeners) {
            each.contentRemoved(info.getObject());
        }
    }

    @NotNull
    private TabInfo createResultTabInfo(String name, TableResult tableResult, TableMode mode, BiConsumer<KdbQuery, TableResultView> repeater) {
        final TableResultView tableResultView = new TableResultView(project, mode, repeater);
        tableResultView.showResult(tableResult);

        return createResultTabInfo(name, tableResultView);
    }

    public static class TableResultContent implements DockableContent<TabInfo> {
        private final Image myImg;
        private final TabInfo tabInfo;
        private final Presentation myPresentation;
        private final Dimension myPreferredSize;

        TableResultContent(TabInfo info) {
            myImg = JBTabsImpl.getComponentImage(info);
            tabInfo = info;

            myPresentation = new Presentation(info.getText());
            myPresentation.setIcon(info.getIcon());

            myPreferredSize = info.getComponent().getSize();
        }

        @NotNull
        @Override
        public TabInfo getKey() {
            return tabInfo;
        }

        @Override
        public Image getPreviewImage() {
            return myImg;
        }

        @Override
        public Dimension getPreferredSize() {
            return myPreferredSize;
        }

        @Override
        public String getDockContainerType() {
            return null;
        }

        @Override
        public Presentation getPresentation() {
            return myPresentation;
        }

        @Override
        public void close() {
        }
    }

    private class RenameTabAction extends AnAction {
        public RenameTabAction() {
            super("Rename/Pin", "Rename the result set to keep it in memory", null);
            registerCustomShortcutSet(KeyEvent.VK_R, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, tabs.getComponent());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            final TabInfo info = tabs.getTargetInfo();
            if (info == null) {
                return;
            }

            final Set<String> collect = tabs.getTabs().stream().map(TabInfo::getText).collect(Collectors.toSet());
            UIUtils.createNameDialog(project, "New name", info.getText(), e.getDataContext(), name -> !collect.contains(name), name -> {
                info.setText(name);
                if (info == tableTab) {
                    tableTab = null;
                }
            });
        }
    }

    private class MyDropAreaPainter extends AbstractPainter {
        private Shape myBoundingBox;

        @Override
        public boolean needsRepaint() {
            return myBoundingBox != null;
        }

        @Override
        public void executePaint(Component component, Graphics2D g) {
            if (myBoundingBox == null) {
                return;
            }
            GraphicsUtil.setupAAPainting(g);
            // TODO: MIGRATION - replace in older versions
            // g.setColor(JBUI.CurrentTheme.DragAndDrop.Area.BACKGROUND);
            g.setColor(JBColor.namedColor("DragAndDrop.areaBackground", 0x3d7dcc, 0x404a57));
            g.fill(myBoundingBox);
        }

        private void processDropOver() {
            myBoundingBox = null;
            setNeedsRepaint(true);

            Rectangle r = new Rectangle(tabs.getComponent().getSize());
            JBInsets.removeFrom(r, getTabsInsets());
            myBoundingBox = new Rectangle2D.Double(r.x, r.y, r.width, r.height);
        }

        private TabInfo getFirstVisibleTab() {
            final int tabCount = myCurrentOver.getTabCount();
            for (int i = 0; i < tabCount; i++) {
                final TabInfo tabAt = myCurrentOver.getTabAt(i);
                if (!tabAt.isHidden()) {
                    return tabAt;
                }
            }
            return null;
        }

        private Insets getTabsInsets() {
            final TabInfo tab = getFirstVisibleTab();
            if (tab == null) {
                return null;
            }

            final Rectangle bounds = myCurrentOver.getTabLabel(tab).getBounds();
            return switch (myCurrentOver.getPresentation().getTabsPosition()) {
                case top -> JBUI.insetsTop(bounds.height);
                case left -> JBUI.insetsLeft(bounds.width);
                case bottom -> JBUI.insetsBottom(bounds.height);
                case right -> JBUI.insetsRight(bounds.width);
            };
        }
    }

    private class MyDragOutDelegate implements TabInfo.DragOutDelegate {
        private DragSession mySession;

        @Override
        public void dragOutStarted(@NotNull MouseEvent mouseEvent, @NotNull TabInfo info) {
            TabInfo previousSelection = info.getPreviousSelection();
            if (previousSelection == null && tabs != null) {
                previousSelection = tabs.getToSelectOnRemoveOf(info);
            }
//                int dragStartIndex = resultTabs.getIndexOf(info);
//                boolean isPinnedAtStart = info.isPinned();
            info.setHidden(true);

            if (previousSelection != null) {
                tabs.select(previousSelection, true);
            }

            final TableResultContent view = new TableResultContent(info);
            mySession = TabsDockingManager.getInstance(project).createDragSession(mouseEvent, view);
        }

        @Override
        public void processDragOut(@NotNull MouseEvent event, @NotNull TabInfo source) {
            mySession.process(event);
        }

        @Override
        public void dragOutFinished(@NotNull MouseEvent event, TabInfo source) {
            boolean copy = UIUtil.isControlKeyDown(event) || mySession.getResponse(event) == DockContainer.ContentResponse.ACCEPT_COPY;
            if (!copy) {
                closeTab(source);
            } else {
                source.setHidden(false);
            }
            mySession.process(event);
            mySession = null;
        }

        @Override
        public void dragOutCancelled(TabInfo source) {
            source.setHidden(false);

            if (mySession != null) {
                mySession.cancel();
                mySession = null;
            }
        }
    }
}