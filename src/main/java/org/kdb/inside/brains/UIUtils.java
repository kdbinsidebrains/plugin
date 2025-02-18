package org.kdb.inside.brains;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Experiments;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.ImageUtil;
import org.apache.commons.lang.text.StrSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class UIUtils {
    public static final String KEY_COLUMN_PREFIX = "\u00A1";

    public static final String KEY_COLUMN_PREFIX_XMAS = "\uD83C\uDF84";

    private UIUtils() {
    }

    public static String replaceSystemProperties(String string) {
        return StrSubstitutor.replaceSystemProperties(string);
    }

    public static JComponent wrapWithHelpLabel(JComponent component, String text) {
        final ContextHelpLabel infoLabel = ContextHelpLabel.create(text);
        final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(component);
        p.add(Box.createHorizontalStrut(5));
        p.add(infoLabel);
        return p;
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

    // Taken from https://stackoverflow.com/questions/1126227/indexof-case-sensitive
    public static int indexOfIgnoreCase(final String where, final String what) {
        return indexOfIgnoreCase(where, what, 0);
    }

    public static int indexOfIgnoreCase(final String where, final String what, int startFrom) {
        if (what.isEmpty() || where.isEmpty()) {
            // Fallback to legacy behavior.
            return where.indexOf(what);
        }

        final int whatLength = what.length();
        final int whereLength = where.length();
        for (int i = startFrom; i < whereLength; ++i) {
            // Early out, if possible.
            if (i + whatLength > whereLength) {
                return -1;
            }

            // Attempt to match substring starting at position i of where.
            int j = 0;
            int ii = i;
            while (ii < whereLength && j < whatLength) {
                char c = Character.toLowerCase(where.charAt(ii));
                char c2 = Character.toLowerCase(what.charAt(j));
                if (c != c2) {
                    break;
                }
                j++;
                ii++;
            }
            // Walked all the way to the end of the what, return the start
            // position that this was found.
            if (j == whatLength) {
                return i;
            }
        }
        return -1;
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

    public static Image getTabInfoImage(TabInfo info) {
        final JComponent component = info.getComponent();
        if (component.isShowing()) {
            var width = component.getWidth();
            var height = component.getHeight();
            var image = ImageUtil.createImage(component.getGraphicsConfiguration(), width > 0 ? width : 500, height > 0 ? height : 500, BufferedImage.TYPE_INT_ARGB);
            component.paint(image.createGraphics());
            return image;
        } else {
            return ImageUtil.createImage(component.getGraphicsConfiguration(), 500, 500, BufferedImage.TYPE_INT_ARGB);
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

    public static ComponentValidator initializerTextBrowseValidator(@NotNull TextFieldWithBrowseButton field, @NotNull Supplier<String> emptySupplier, @Nullable Supplier<String> nonExistSupplier) {
        return initializerTextBrowseValidator(field, emptySupplier, nonExistSupplier, (Function<String, String>[]) null);
    }

    @SafeVarargs
    public static ComponentValidator initializerTextBrowseValidator(@NotNull TextFieldWithBrowseButton field, @NotNull Supplier<String> emptySupplier, @Nullable Supplier<String> nonExistSupplier, Function<String, String>... customValidator) {
        final JTextField textField = field.getTextField();

        final ComponentValidator componentValidator = new ComponentValidator(field);
        componentValidator.withValidator(() -> {
            final String text = textField.getText().trim();
            if (text.isEmpty()) {
                return new ValidationInfo(emptySupplier.get(), textField);
            }
            if (nonExistSupplier != null && !Files.exists(Path.of(text))) {
                return new ValidationInfo(nonExistSupplier.get(), textField);
            }
            if (customValidator != null) {
                for (Function<String, String> validator : customValidator) {
                    final String apply = validator.apply(text);
                    if (apply != null) {
                        return new ValidationInfo(apply, textField);
                    }
                }
            }
            return null;
        }).andRegisterOnDocumentListener(textField).installOn(textField);
        return componentValidator;
    }

    public static void initializeFileChooser(@Nullable Project project, @NotNull ComponentWithBrowseButton<?> field, @NotNull FileChooserDescriptor descriptor) {
        if (descriptor.getRoots().isEmpty() && project != null) {
            descriptor.withRoots(project.getBaseDir());
        }

        if (field instanceof TextFieldWithBrowseButton button) {
            button.addBrowseFolderListener(new TextBrowseFolderListener(descriptor, project));
        } else if (field instanceof TextFieldWithHistoryWithBrowseButton hist) {
            hist.addBrowseFolderListener(project, descriptor, TextComponentAccessors.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
        } else {
            throw new UnsupportedOperationException("Unsupported field type: " + field.getClass());
        }
    }
}
