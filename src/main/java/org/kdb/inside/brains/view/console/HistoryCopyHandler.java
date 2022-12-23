package org.kdb.inside.brains.view.console;

import com.intellij.codeInsight.editorActions.TextBlockTransferable;
import com.intellij.codeInsight.editorActions.TextBlockTransferableData;
import com.intellij.execution.actions.ClearConsoleAction;
import com.intellij.execution.console.LanguageConsoleView;
import com.intellij.ide.actions.CopyUrlAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.editor.richcopy.model.SyntaxInfo;
import com.intellij.openapi.editor.richcopy.settings.RichCopySettings;
import com.intellij.openapi.editor.richcopy.view.HtmlTransferableData;
import com.intellij.openapi.editor.richcopy.view.RtfTransferableData;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.PopupHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * This code is based on getSelectionAction of {@link com.intellij.codeInsight.editorActions.CopyHandler} and
 * {@link com.intellij.openapi.editor.richcopy.TextWithMarkupProcessor}.
 */
class HistoryCopyHandler {
    private static final Logger LOG = Logger.getInstance(HistoryCopyHandler.class);

    public static void redefineHistoricalViewer(LanguageConsoleView console) {
        final EditorEx editor = console.getHistoryViewer();
        final JComponent content = editor.getContentComponent();

        final DefaultActionGroup group = new DefaultActionGroup();

        group.add(HistoryCopyHandler.createActions(console.getProject(), editor));
        group.addSeparator();
        group.add(ActionManager.getInstance().getAction("CompareClipboardWithSelection"));
        group.add(ActionManager.getInstance().getAction("$SearchWeb"));
        group.add(new CopyUrlAction());
        group.addSeparator();
        group.add(new ClearConsoleAction());

        PopupHandler.installPopupHandler(content, group, "Kdb.ConsoleHistoryView");
    }

    private static ActionGroup createActions(Project project, EditorEx editor) {
        final AnAction action = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyHistoryEditorToClipboard(project, editor, false);
            }
        };
        action.copyFrom(ActionManager.getInstance().getAction("$Copy"));
        action.registerCustomShortcutSet(action.getShortcutSet(), editor.getContentComponent());

        final AnAction copyAsPlain = new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                copyHistoryEditorToClipboard(project, editor, true);
            }
        };
        copyAsPlain.copyFrom(ActionManager.getInstance().getAction("CopyAsPlainText"));

        return new DefaultActionGroup(action, copyAsPlain);
    }

    public static void copyHistoryEditorToClipboard(Project project, EditorEx editor, boolean plain) {
        final List<TextBlockTransferableData> transferableDataList = new ArrayList<>();

        final String rawText = collectTransferableData(project, editor, transferableDataList, plain);
        if (rawText == null) {
            return;
        }

        final TextBlockTransferable transferable = new TextBlockTransferable(rawText, transferableDataList, new RawText(rawText));

        CopyPasteManager.getInstance().setContents(transferable);
    }

    private static String collectTransferableData(@NotNull Project project, @NotNull Editor editor, List<TextBlockTransferableData> transferable, boolean plain) {
        if (plain || !RichCopySettings.getInstance().isEnabled()) {
            return collectPlainText(editor, transferable);
        }
        return collectRichText(project, editor, transferable);
    }

    private static String collectRichText(@NotNull Project project, @NotNull Editor editor, List<TextBlockTransferableData> transferable) {
        final EditorColorsScheme scheme = editor.getColorsScheme();
        try {
            final MarkupModel model = DocumentMarkupModel.forDocument(editor.getDocument(), project, false);

            final String text = String.valueOf(model.getDocument().getCharsSequence());
            final RangeHighlighter[] highlighters = model.getAllHighlighters();
            // They are unsorted by default
            Arrays.parallelSort(highlighters, Comparator.comparingInt(RangeMarker::getStartOffset));

            final StringBuilder buf = new StringBuilder();

            float fontSize = getFontSize(scheme);

            final SyntaxInfo.Builder builder = new SyntaxInfo.Builder(scheme.getDefaultForeground(), scheme.getDefaultBackground(), fontSize);
            builder.addFontFamilyName(scheme.getFontPreferences().getFontFamily());

            String separator = "";
            final List<Caret> allCarets = editor.getCaretModel().getAllCarets();
            for (Caret caret : allCarets) {
                if (!caret.hasSelection()) {
                    continue;
                }
                if (!separator.isEmpty()) {
                    buf.append(separator);
                    builder.addText(buf.length() - 1, buf.length());
                }

                collectCaretData(caret, text, scheme, highlighters, builder, buf);
                separator = "\n";
            }

            final String rawText = buf.toString();
            final SyntaxInfo syntax = builder.build();

            Stream.of(new HtmlTransferableData(syntax, EditorUtil.getTabSize(editor)), new RtfTransferableData(syntax)).peek(r -> r.setRawText(rawText)).forEach(transferable::add);

            return rawText;
        } catch (Throwable t) {
            LOG.error("Error generating text with markup", t);
            return null;
        }
    }

    private static float getFontSize(EditorColorsScheme scheme) {
        // The code taken from SyntaxInfoBuilder#Context#Context
        float javaFontSize = scheme.getEditorFontSize();
        return SystemInfo.isMac || ApplicationManager.getApplication().isHeadlessEnvironment() ? javaFontSize : javaFontSize * 0.75f / UISettings.getDefFontScale(); // matching font size in external apps
    }

    private static void collectCaretData(Caret caret, String text, EditorColorsScheme scheme, RangeHighlighter[] highlighters, SyntaxInfo.Builder builder, StringBuilder buf) {
        final int selectionStart = caret.getSelectionStart();
        final int selectionEnd = caret.getSelectionEnd();
        if (selectionStart == selectionEnd) {
            return;
        }

        for (final RangeHighlighter highlighter : highlighters) {
            final int hs = highlighter.getStartOffset();
            final int he = highlighter.getEndOffset();

            if (he < selectionStart) {
                continue;
            }

            if (hs > selectionEnd) {
                break;
            }

            final TextAttributes attrs = highlighter.getTextAttributes(scheme);
            if (attrs != null) {
                final Color foregroundColor = attrs.getForegroundColor();
                if (foregroundColor != null) {
                    builder.addForeground(foregroundColor);
                }

                final Color backgroundColor = attrs.getBackgroundColor();
                if (backgroundColor != null) {
                    builder.addBackground(backgroundColor);
                }

                builder.addFontStyle(attrs.getFontType());
            }

            final int start = Math.max(selectionStart, hs);
            final int end = Math.min(selectionEnd, he);
            final int s = buf.length();
            buf.append(StringUtil.convertLineSeparators(text.substring(start, end), "\n"));
            final int e = buf.length();
            builder.addText(s, e);
        }
    }

    @Nullable
    private static String collectPlainText(@NotNull Editor editor, List<TextBlockTransferableData> transferable) {
        final SelectionModel selectionModel = editor.getSelectionModel();
        final boolean multipleCarets = editor.getCaretModel().supportsMultipleCarets();

        String s = multipleCarets ? EditorCopyPasteHelperImpl.getSelectedTextForClipboard(editor, transferable) : selectionModel.getSelectedText();
        if (StringUtil.isEmpty(s)) {
            return null;
        }
        return TextBlockTransferable.convertLineSeparators(s, "\n", transferable);
    }
}