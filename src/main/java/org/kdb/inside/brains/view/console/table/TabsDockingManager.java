package org.kdb.inside.brains.view.console.table;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.BusyObject;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.DevicePoint;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockableContent;
import com.intellij.ui.docking.DragSession;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.ImageUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.Activatable;
import com.intellij.util.ui.update.UiNotifyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * It's based on DockManagerImpl but all not required staff has been removed.
 * <p>
 * Main reason to create the class - DockManager in dialog mode looks bad but in frame mode adds north panel and status bar.
 */
@Service(value = Service.Level.PROJECT)
final class TabsDockingManager implements Disposable {
    private final Project myProject;
    private final Set<DockContainer> myContainers = new HashSet<>();
    private final Map<DockContainer, DockWindow> containerToWindow = new HashMap<>();
    private int myWindowIdCounter = 1;
    private MyDragSession myCurrentDragSession;
    private final BusyObject.Impl myBusyObject = new BusyObject.Impl() {
        @Override
        public boolean isReady() {
            return myCurrentDragSession == null;
        }
    };

    public TabsDockingManager(@NotNull Project project) {
        myProject = project;
    }

    public static TabsDockingManager getInstance(Project project) {
        return project.getService(TabsDockingManager.class);
    }

    public void register(@NotNull DockContainer container, @NotNull Disposable parentDisposable) {
        myContainers.add(container);
        Disposer.register(parentDisposable, () -> myContainers.remove(container));
    }

    public DragSession createDragSession(MouseEvent mouseEvent, @NotNull DockableContent<?> content) {
        stopCurrentDragSession();

        for (DockContainer each : getContainers()) {
            if (each.isEmpty() && each.isDisposeWhenEmpty()) {
                DockWindow window = containerToWindow.get(each);
                if (window != null) {
                    window.setTransparent(true);
                }
            }
        }

        myCurrentDragSession = new MyDragSession(mouseEvent, content);
        return myCurrentDragSession;
    }

    public void stopCurrentDragSession() {
        if (myCurrentDragSession == null) {
            return;
        }

        myCurrentDragSession.cancelSession();
        myCurrentDragSession = null;
        myBusyObject.onReady();

        for (DockContainer each : getContainers()) {
            if (each.isEmpty()) {
                continue;
            }

            final DockWindow window = containerToWindow.get(each);
            if (window != null) {
                window.setTransparent(false);
            }
        }
    }

    private void createNewDockContainerFor(@NotNull DockableContent<?> content, @NotNull RelativePoint point) {
        final DockContainer container = new TabsTableResult(myProject, this);

        final String windowId = Integer.toString(myWindowIdCounter++);
        final DockWindow window = new DockWindow("KdbConsole.TableResult.Window", windowId, myProject, container);
        window.setTitle("KDB Table Result");
        containerToWindow.put(container, window);

        Dimension size = content.getPreferredSize();
        Point showPoint = point.getScreenPoint();
        showPoint.x -= size.width / 2;
        showPoint.y -= size.height / 2;

        Rectangle target = new Rectangle(showPoint, size);
        ScreenUtil.moveRectangleToFitTheScreen(target);
        ScreenUtil.cropRectangleToFitTheScreen(target);

        window.setLocation(target.getLocation());
        window.myDockContentUiContainer.setPreferredSize(target.getSize());

        window.show(false);
        window.getFrame().pack();

        container.add(content, new RelativePoint(target.getLocation()));

        ApplicationManager.getApplication().invokeLater(() -> window.myUiContainer.setPreferredSize(null));
    }

    private ActionCallback getReady() {
        return myBusyObject.getReady(this);
    }

    private DockContainer getContainerFor(Component c) {
        return getContainerFor(c, dockContainer -> true);
    }

    private List<DockContainer> getContainers() {
        List<DockContainer> result = new ArrayList<>(myContainers.size() + containerToWindow.size());
        result.addAll(myContainers);
        result.addAll(containerToWindow.keySet());
        return result;
    }

    private @Nullable DockContainer getContainerFor(@Nullable Component c, @NotNull Predicate<DockContainer> filter) {
        if (c == null) {
            return null;
        }

        for (DockContainer eachContainer : getContainers()) {
            if (SwingUtilities.isDescendingFrom(c, eachContainer.getContainerComponent()) && filter.test(eachContainer)) {
                return eachContainer;
            }
        }

        Component parent = UIUtil.findUltimateParent(c);
        for (DockContainer eachContainer : getContainers()) {
            if (parent == UIUtil.findUltimateParent(eachContainer.getContainerComponent()) && filter.test(eachContainer)) {
                return eachContainer;
            }
        }

        return null;
    }

    private @Nullable DockContainer findContainerFor(RelativePoint point, @NotNull DockableContent<?> content) {
        List<DockContainer> containers = getContainers();
        containers.remove(myCurrentDragSession.myStartDragContainer);
        containers.add(0, myCurrentDragSession.myStartDragContainer);

        final DevicePoint dp = new DevicePoint(point);
        for (DockContainer each : containers) {
            RelativeRectangle rec = each.getAcceptArea();
            if (rec.contains(dp) && each.getContentResponse(content, point).canAccept()) {
                return each;
            }
        }

        for (DockContainer each : containers) {
            RelativeRectangle rec = each.getAcceptAreaFallback();
            if (rec.contains(dp) && each.getContentResponse(content, point).canAccept()) {
                return each;
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        if (myCurrentDragSession != null) {
            myCurrentDragSession.cancel();
        }

        myContainers.forEach(DockContainer::closeAll);
        myContainers.clear();

        containerToWindow.values().forEach(DockWindow::dispose);
        containerToWindow.keySet().forEach(DockContainer::closeAll);
        containerToWindow.clear();
    }

    private final class MyDragSession implements DragSession {
        private final JDialog myWindow;
        private final Image myDefaultDragImage;
        private final DockableContent<?> myContent;
        private final JLabel myImageContainer;
        private final DockContainer myStartDragContainer;
        private Image myDragImage;
        private DockContainer myCurrentOverContainer;

        private MyDragSession(MouseEvent me, @NotNull DockableContent<?> content) {
            myWindow = new JDialog(UIUtil.getWindow(me.getComponent()));
            myWindow.setUndecorated(true);
            myContent = content;
            myStartDragContainer = getContainerFor(me.getComponent());

            BufferedImage buffer = ImageUtil.toBufferedImage(content.getPreviewImage());

            double requiredSize = 220;

            double width = buffer.getWidth(null);
            double height = buffer.getHeight(null);

            double ratio;
            if (width > height) {
                ratio = requiredSize / width;
            } else {
                ratio = requiredSize / height;
            }

            myDefaultDragImage = buffer.getScaledInstance((int) (width * ratio), (int) (height * ratio), Image.SCALE_SMOOTH);
            myDragImage = myDefaultDragImage;

            myImageContainer = new JLabel();
            myImageContainer.setIcon(IconUtil.createImageIcon(myDragImage));
            myWindow.setContentPane(myImageContainer);
            myWindow.pack();

            setLocationFrom(me);

            myWindow.setVisible(true);

            WindowManagerEx windowManager = WindowManagerEx.getInstanceEx();
            windowManager.setAlphaModeEnabled(myWindow, true);
            windowManager.setAlphaModeRatio(myWindow, 0.1f);
        }

        private void setLocationFrom(MouseEvent me) {
            Point showPoint = me.getPoint();
            SwingUtilities.convertPointToScreen(showPoint, me.getComponent());

            Dimension size = myImageContainer.getSize();
            showPoint.x -= size.width / 2;
            showPoint.y -= size.height / 2;
            myWindow.setBounds(new Rectangle(showPoint, size));
        }

        @Override
        public @NotNull DockContainer.ContentResponse getResponse(MouseEvent e) {
            final DevicePoint dp = new DevicePoint(e);
            final RelativePoint rp = new RelativePoint(e);
            for (DockContainer each : getContainers()) {
                final RelativeRectangle rec = each.getAcceptArea();
                if (rec.contains(dp)) {
                    DockContainer.ContentResponse response = each.getContentResponse(myContent, rp);
                    if (response.canAccept()) {
                        return response;
                    }
                }
            }
            return DockContainer.ContentResponse.DENY;
        }

        @Override
        public void process(MouseEvent e) {
            final RelativePoint point = new RelativePoint(e);

            Image img = null;
            if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
                DockContainer over = findContainerFor(point, myContent);
                if (myCurrentOverContainer != null && myCurrentOverContainer != over) {
                    myCurrentOverContainer.resetDropOver(myContent);
                    myCurrentOverContainer = null;
                }

                if (myCurrentOverContainer == null && over != null) {
                    myCurrentOverContainer = over;
                    img = myCurrentOverContainer.startDropOver(myContent, point);
                }

                if (myCurrentOverContainer != null) {
                    img = myCurrentOverContainer.processDropOver(myContent, point);
                }

                if (img == null) {
                    img = myDefaultDragImage;
                }

                if (img != myDragImage) {
                    myDragImage = img;
                    myImageContainer.setIcon(IconUtil.createImageIcon(myDragImage));
                    myWindow.pack();
                }

                setLocationFrom(e);
            } else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
                if (myCurrentOverContainer == null) {
                    createNewDockContainerFor(myContent, point);
                    e.consume();//Marker for DragHelper: drag into separate window is not tabs reordering
                } else {
                    myCurrentOverContainer.add(myContent, point);
                }
                stopCurrentDragSession();
            }
        }

        @Override
        public void cancel() {
            stopCurrentDragSession();
        }

        private void cancelSession() {
            myWindow.dispose();

            if (myCurrentOverContainer != null) {
                myCurrentOverContainer.resetDropOver(myContent);
                myCurrentOverContainer = null;
            }
        }
    }

    private final class DockWindow extends FrameWrapper implements IdeEventQueue.EventDispatcher {
        private final DockContainer myContainer;

        private final NonOpaquePanel myUiContainer;
        private final JPanel myDockContentUiContainer;

        private DockWindow(@Nullable String dimensionKey,
                           @Nullable String id,
                           @NotNull Project project,
                           @NotNull DockContainer container) {
            super(project, dimensionKey != null ? dimensionKey : "dock-window-" + id, false);

            installListeners(getFrame());

            myContainer = container;

            JPanel myCenterPanel = new JPanel(new BorderLayout(0, 2));
            myCenterPanel.setOpaque(false);

            myDockContentUiContainer = new JPanel(new BorderLayout());
            myDockContentUiContainer.setOpaque(false);
            myDockContentUiContainer.add(myContainer.getContainerComponent(), BorderLayout.CENTER);
            myCenterPanel.add(myDockContentUiContainer, BorderLayout.CENTER);

            myUiContainer = new NonOpaquePanel(new BorderLayout());
            myUiContainer.add(myCenterPanel, BorderLayout.CENTER);
            StatusBar statusBar = getStatusBar();
            if (statusBar != null) {
                final JComponent component = statusBar.getComponent();
                if (component != null) {
                    myUiContainer.add(component, BorderLayout.SOUTH);
                }
            }

            setComponent(myUiContainer);

            IdeEventQueue.getInstance().addPostprocessor(this, this);

            myContainer.addListener(new DockContainer.Listener() {
                @Override
                public void contentRemoved(@NotNull Object key) {
                    getReady().doWhenDone(() -> {
                        if (myContainer.isEmpty()) {
                            close();
                            myContainers.remove(myContainer);
                        }
                    });
                }
            }, this);
        }

        public void setTransparent(boolean transparent) {
            final WindowManagerEx ex = WindowManagerEx.getInstanceEx();
            ex.setAlphaModeEnabled(getFrame(), true);
            ex.setAlphaModeRatio(getFrame(), transparent ? 0.5f : 0f);
        }

        @Override
        public void dispose() {
            super.dispose();
            containerToWindow.remove(myContainer);
            if (myContainer instanceof Disposable) {
                Disposer.dispose((Disposable) myContainer);
            }
        }

        @Override
        public boolean dispatch(@NotNull AWTEvent e) {
            if (e instanceof KeyEvent) {
                if (myCurrentDragSession != null) {
                    stopCurrentDragSession();
                }
            }
            return false;
        }

        private void installListeners(@NotNull Window frame) {
            UiNotifyConnector uiNotifyConnector = myContainer instanceof Activatable
                    ? new UiNotifyConnector(((RootPaneContainer) frame).getContentPane(), (Activatable) myContainer)
                    : null;
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    myContainer.closeAll();
                    if (uiNotifyConnector != null) {
                        Disposer.dispose(uiNotifyConnector);
                    }
                }
            });
        }
    }
}