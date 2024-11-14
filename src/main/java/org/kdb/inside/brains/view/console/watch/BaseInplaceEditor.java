package org.kdb.inside.brains.view.console.watch;

import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.execution.Executor;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.execution.ui.RunContentWithExecutorListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ClientProperty;
import com.intellij.ui.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a copy of com.intellij.xdebugger.impl.ui.InplaceEditor declared Internal now.
 * <p>
 * Another option is to use CustomFieldInplaceEditor, but it's designed for Java use only.
 */
abstract class BaseInplaceEditor implements AWTEventListener {
    public static final Key<Boolean> IGNORE_MOUSE_EVENT = Key.create("KIB.BaseInplaceEditor.IGNORE_MOUSE_EVENT");
    public static final Key<JComponent> PARENT_COMPONENT = Key.create("KIB.BaseInplaceEditor.PARENT_COMPONENT");
    protected final Disposable myDisposable = Disposer.newDisposable();
    private final List<Runnable> myRemoveActions = new ArrayList<>();
    private JComponent myInplaceEditorComponent;

    private static void setInplaceEditorBounds(JComponent component, int x, int y, int width, int height) {
        int h = Math.max(height, component.getPreferredSize().height);
        component.setBounds(x, y - (h - height) / 2, width, h);
    }

    protected abstract JComponent createInplaceEditorComponent();

    protected abstract JComponent getPreferredFocusedComponent();

    public abstract Editor getEditor();

    public abstract JComponent getEditorComponent();

    protected void doPopupOKAction() {
        this.doOKAction();
    }

    public void doOKAction() {
        this.hide();
    }

    public void cancelEditing() {
        this.hide();
    }

    protected abstract JComponent getHostComponent();

    private void hide() {
        if (this.isShown()) {
            this.myInplaceEditorComponent = null;
            this.onHidden();
            this.myRemoveActions.forEach(Runnable::run);
            this.myRemoveActions.clear();
            Disposer.dispose(this.myDisposable);
            JComponent hostComponent = this.getHostComponent();
            hostComponent.repaint();
            IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(hostComponent, true));
        }
    }

    protected void onHidden() {
    }

    protected abstract Project getProject();

    public final void show() {
        this.beforeShow();

        JComponent hostComponent = this.getHostComponent();
        JRootPane rootPane = this.getHostComponent().getRootPane();
        if (rootPane != null) {
            JLayeredPane layeredPane = rootPane.getLayeredPane();
            Rectangle bounds = this.getEditorBounds();
            if (bounds != null) {
                Point layeredPanePoint = SwingUtilities.convertPoint(hostComponent, bounds.x, bounds.y, layeredPane);
                final JComponent inplaceEditorComponent = this.createInplaceEditorComponent();
                this.myInplaceEditorComponent = inplaceEditorComponent;
                setInplaceEditorBounds(inplaceEditorComponent, layeredPanePoint.x, layeredPanePoint.y, bounds.width, bounds.height);
                layeredPane.add(inplaceEditorComponent);
                ClientProperty.put(inplaceEditorComponent, PARENT_COMPONENT, hostComponent);
                this.myRemoveActions.add(() -> {
                    layeredPane.remove(inplaceEditorComponent);
                    ClientProperty.remove(inplaceEditorComponent, PARENT_COMPONENT);
                });
                inplaceEditorComponent.validate();
                inplaceEditorComponent.paintImmediately(0, 0, inplaceEditorComponent.getWidth(), inplaceEditorComponent.getHeight());
                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(this.getPreferredFocusedComponent(), true));
                ComponentAdapter componentListener = new ComponentAdapter() {
                    public void componentMoved(ComponentEvent e) {
                        this.resetBounds();
                    }

                    public void componentResized(ComponentEvent e) {
                        this.resetBounds();
                    }

                    private void resetBounds() {
                        Project project = getProject();
                        ApplicationManager.getApplication().invokeLater(() -> {
                            if (isShown() && project != null && !project.isDisposed()) {
                                JComponent hostComponent = getHostComponent();
                                JLayeredPane layeredPane1 = hostComponent.getRootPane().getLayeredPane();
                                Rectangle bounds1 = getEditorBounds();
                                if (bounds1 == null) {
                                    doOKAction();
                                } else {
                                    Point layeredPanePoint1 = SwingUtilities.convertPoint(hostComponent, bounds1.x, bounds1.y, layeredPane1);
                                    setInplaceEditorBounds(inplaceEditorComponent, layeredPanePoint1.x, layeredPanePoint1.y, bounds1.width, bounds1.height);
                                    inplaceEditorComponent.revalidate();
                                }
                            }
                        });
                    }

                    public void componentHidden(ComponentEvent e) {
                        cancelEditing();
                    }
                };
                HierarchyListener hierarchyListener = (HierarchyEvent e) -> {
                    if (!hostComponent.isShowing()) {
                        this.cancelEditing();
                    }

                };
                hostComponent.addHierarchyListener(hierarchyListener);
                hostComponent.addComponentListener(componentListener);
                rootPane.addComponentListener(componentListener);
                this.myRemoveActions.add(() -> {
                    hostComponent.removeHierarchyListener(hierarchyListener);
                    hostComponent.removeComponentListener(componentListener);
                    rootPane.removeComponentListener(componentListener);
                });
                this.getProject().getMessageBus().connect(this.myDisposable).subscribe(RunContentManager.TOPIC, new RunContentWithExecutorListener() {
                    public void contentSelected(@Nullable RunContentDescriptor descriptor, @NotNull Executor executor) {
                        cancelEditing();
                    }

                    public void contentRemoved(@Nullable RunContentDescriptor descriptor, @NotNull Executor executor) {
                        cancelEditing();
                    }
                });
                JComponent editorComponent = this.getEditorComponent();
                editorComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(10, 0), "enterStroke");
                editorComponent.getActionMap().put("enterStroke", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        doOKAction();
                    }
                });
                editorComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(27, 0), "escapeStroke");
                editorComponent.getActionMap().put("escapeStroke", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        cancelEditing();
                    }
                });
                Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                SwingUtilities.invokeLater(() -> {
                    if (this.isShown()) {
                        defaultToolkit.addAWTEventListener(this, 16L);
                    }
                });
                this.myRemoveActions.add(() -> defaultToolkit.removeAWTEventListener(this));
                this.onShown();
            }
        }
    }

    protected abstract void beforeShow();

    protected void onShown() {
    }

    public void eventDispatched(AWTEvent event) {
        if (this.isShown()) {
            MouseEvent mouseEvent = (MouseEvent) event;
            if (mouseEvent.getClickCount() != 0) {
                int id = mouseEvent.getID();
                if (id == 501 || id == 502 || id == 500) {
                    Component sourceComponent = mouseEvent.getComponent();
                    Boolean property = ClientProperty.get(sourceComponent, IGNORE_MOUSE_EVENT);
                    if (property == null || !property.equals(true)) {
                        Point originalPoint = mouseEvent.getPoint();
                        Editor editor = this.getEditor();
                        if (editor != null) {
                            for (JBPopup popup : JBPopupFactory.getInstance().getChildPopups(this.myInplaceEditorComponent)) {
                                if (!popup.isDisposed() && SwingUtilities.isDescendingFrom(sourceComponent, ComponentUtil.getWindow(popup.getContent()))) {
                                    return;
                                }
                            }

                            Project project = editor.getProject();
                            LookupImpl activeLookup = project != null ? (LookupImpl) LookupManager.getInstance(project).getActiveLookup() : null;
                            if (activeLookup != null) {
                                Point lookupPoint = SwingUtilities.convertPoint(sourceComponent, originalPoint, activeLookup.getComponent());
                                if (activeLookup.getComponent().getBounds().contains(lookupPoint)) {
                                    return;
                                }

                                activeLookup.hide();
                            }

                            Point point = SwingUtilities.convertPoint(sourceComponent, originalPoint, this.myInplaceEditorComponent);
                            if (!this.myInplaceEditorComponent.contains(point)) {
                                Component componentAtPoint = SwingUtilities.getDeepestComponentAt(sourceComponent, originalPoint.x, originalPoint.y);

                                for (Component comp = componentAtPoint; comp != null; comp = comp.getParent()) {
                                    if (comp instanceof ComboPopup) {
                                        this.doPopupOKAction();
                                        return;
                                    }
                                }

                                if (ComponentUtil.getWindow(sourceComponent) == ComponentUtil.getWindow(this.myInplaceEditorComponent) && id == 501) {
                                    this.doOKAction();
                                }

                            }
                        }
                    }
                }
            }
        }
    }

    protected abstract @Nullable Rectangle getEditorBounds();

    public boolean isShown() {
        return this.myInplaceEditorComponent != null;
    }
}
