package org.kdb.inside.brains.action.toolbar;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.action.EdtAction;

import javax.swing.*;
import java.awt.*;

/**
 * The Idea was to add toolbar height, but I couldn't make it work for now - QuickSelection has incorrect background/foreground.
 * <p>
 * In com.intellij.execution.ui.RedesignedRunWidget.kt they have redefined all UI classes to support correct view. Not sure, I'd like to do the same for now.
 */
public class NewUIToolbarComponent extends EdtAction implements CustomComponentAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }

    @Override
    public @NotNull JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
        final ActionManager actionManager = ActionManager.getInstance();

        final ActionGroup group = (ActionGroup) actionManager.getAction("Kdb.NewMainToolbarActionGroup");

        final ActionToolbar toolbar = actionManager.createActionToolbar(ActionPlaces.MAIN_TOOLBAR, group, true);
        toolbar.setReservePlaceAutoPopupIcon(false);
        toolbar.setMinimumButtonSize(JBUI.size(36, 30));
        toolbar.setLayoutPolicy(ActionToolbar.NOWRAP_LAYOUT_POLICY);
        if (toolbar instanceof ActionToolbarImpl t) {
            t.setOpaque(false);
            t.setBorder(JBUI.Borders.empty());
            t.setActionButtonBorder(JBUI.Borders.empty());
//            t.setCustomButtonLook(new RunWidgetButtonLook());
        }
/*
        toolbar.addListener(new ActionToolbarListener() {
            @Override
            public void actionsUpdated() {
//                toolbar.

                final JComponent component = toolbar.getComponent();
                System.out.println(component);
            }
        }, () -> {});
*/
        final JComponent component = toolbar.getComponent();
        component.setBorder(JBUI.Borders.empty(5, 12, 5, 22));
        return component;
    }

    private static class RunWidgetButtonLook extends IdeaActionButtonLook {
        @Override
        protected Color getStateBackground(JComponent component, int state) {
            return JBColor.RED;
//            return JBUI.CurrentTheme.RunWidget.BACKGROUND;
        }
/*
        @Override
        public void paintBackground(Graphics g, JComponent component, int state) {
            super.paintBackground(g, component, state);
        }*/
    }
/*
    private Color getRunWidgetBackgroundColor() {
        return JBUI.CurrentTheme.RunWidget.BACKGROUND;
    }*/

/*
    private val isCurrentConfigurationRunning: () -> Boolean) : () {
        override fun getStateBackground(component: JComponent, state: Int): Color {

            val color = getRunWidgetBackgroundColor(isCurrentConfigurationRunning())

            return when (state) {
                ActionButtonComponent.NORMAL -> color
                ActionButtonComponent.PUSHED -> ColorUtil.alphaBlending(JBUI.CurrentTheme.RunWidget.PRESSED_BACKGROUND, color)
      else -> ColorUtil.alphaBlending(JBUI.CurrentTheme.RunWidget.HOVER_BACKGROUND, color)
            }
        }

        override fun paintBackground(g: Graphics, component: JComponent, @ActionButtonComponent.ButtonState state: Int) {
            val rect = Rectangle(component.size)
            val color = getStateBackground(component, state)

            val g2 = g.create() as Graphics2D

            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
                g2.color = color
                val arc = buttonArc.float
                val width = rect.width
                val height = rect.height

                val shape = when (component) {
                    component.parent?.components?.lastOrNull() -> {
                        val shape1 = RoundRectangle2D.Float(rect.x.toFloat(), rect.y.toFloat(), width.toFloat(), height.toFloat(), arc, arc)
                        val shape2 = Rectangle2D.Float(rect.x.toFloat() - 1, rect.y.toFloat(), arc, height.toFloat())
                        Area(shape1).also { it.add(Area(shape2)) }
                    }
                    component.parent?.components?.get(0) -> {
                        val shape1 = RoundRectangle2D.Float(rect.x.toFloat(), rect.y.toFloat(), width.toFloat(), height.toFloat(), arc, arc)
                        val shape2 = Rectangle2D.Float((rect.x + width).toFloat() - arc, rect.y.toFloat(), arc, height.toFloat())
                        Area(shape1).also { it.add(Area(shape2)) }
                    }
        else -> {
                        Rectangle2D.Float(rect.x.toFloat() - 1, rect.y.toFloat(), width.toFloat() + 2, height.toFloat())
                    }
                }

                g2.fill(shape)
            }
            finally {
                g2.dispose()
            }
        }


        override fun paintIcon(g: Graphics, actionButton: ActionButtonComponent, icon: Icon, x: Int, y: Int) {
            if (icon.iconWidth == 0 || icon.iconHeight == 0) {
                return
            }
            super.paintIcon(g, actionButton, IconUtil.toStrokeIcon(icon, JBUI.CurrentTheme.RunWidget.FOREGROUND), x, y)
        }

        override fun paintLookBorder(g: Graphics, rect: Rectangle, color: Color) {}
        override fun getButtonArc(): JBValue = JBValue.Float(8f)
    }
*/
}
