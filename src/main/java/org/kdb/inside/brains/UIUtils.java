package org.kdb.inside.brains;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class UIUtils {
    private UIUtils() {
    }

    public static Content createContent(JComponent component, String displayName, boolean isLockable) {
        return getContentFactory().createContent(component, displayName, isLockable);
    }

    public static ContentFactory getContentFactory() {
        return ApplicationManager.getApplication().getService(ContentFactory.class);
    }

    public static String encodeColor(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static Color decodeColor(String hexColor) {
        return Color.decode(hexColor);
    }

    public static Color getKeyColumnColor(Color c) {
        if (c == null) {
            return null;
        }
        return new Color(Math.max((int) (c.getRed() * 0.85), 0), Math.max((int) (c.getGreen() * 0.9), 0), Math.max((int) (c.getBlue() * 0.85), 0), c.getAlpha());
    }

    public static void createNameDialog(Project project, String title, String currentName, DataContext dataContext, Predicate<String> validName, Consumer<String> action) {
        final InputValidator validator = new InputValidator() {
            @Override
            public boolean checkInput(String name) {
                return !name.isBlank() && validName.test(name);
            }

            @Override
            public boolean canClose(String inputString) {
                return checkInput(inputString);
            }
        };
        createNameDialog(project, title, currentName, dataContext, validator, action);
    }

    public static void createNameDialog(Project project, String title, String currentName, DataContext dataContext, InputValidator validator, Consumer<String> action) {
        if (Experiments.getInstance().isFeatureEnabled("show.create.new.element.in.popup")) {
            createPopupNameDialog(project, title, currentName, validator, action).showInBestPositionFor(dataContext);
        } else {
            final String new_package = Messages.showInputDialog(project, null, title, null, currentName, validator, new TextRange(0, currentName.length()));
            if (new_package != null) {
                action.accept(new_package);
            }
        }
    }

    private static JBPopup createPopupNameDialog(Project project, String title, String currentName, InputValidator validator, Consumer<String> action) {
        final JBTextField textField = new JBTextField(20);
        textField.setText(currentName);
        textField.setSelectionStart(0);
        textField.setSelectionEnd(currentName.length());

        textField.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                final boolean b = validator.checkInput(textField.getText());
                textField.putClientProperty("JComponent.outline", !b ? "error" : null);
            }
        });
        textField.setTextToTriggerEmptyTextStatus("Name");

        final ComponentPopupBuilder componentPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(textField, textField)
                .setTitle(title)
                .setProject(project)
                .setMovable(false)
                .setResizable(true)
                .setRequestFocus(true)
                .setCancelKeyEnabled(true);

        final JBPopup popup = componentPopupBuilder.createPopup();

        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                final String text = textField.getText();
                if (!validator.canClose(text)) {
                    return;
                }
                popup.cancel();
                action.accept(text);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), textField);
        return popup;
    }
}
