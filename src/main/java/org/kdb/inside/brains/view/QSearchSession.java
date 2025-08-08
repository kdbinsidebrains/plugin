package org.kdb.inside.brains.view;

import com.intellij.find.*;
import com.intellij.find.editorHeaderActions.EditorHeaderToggleAction;
import com.intellij.find.editorHeaderActions.Embeddable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.ComponentWithEmptyText;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QSearchSession implements SearchSession {
    private final FindModel findModel;
    private final SearchReplaceComponent searchComponent;

    private boolean internalChangeFlag;

    protected static final RangeExtractor[] NO_EXTRACTORS = new RangeExtractor[0];

    public QSearchSession(@Nullable Project project, @NotNull JComponent component) {
        this.findModel = new FindModel();
        findModel.setFindAll(true);

        searchComponent = initializeComponent(SearchReplaceComponent.buildFor(project, component))
                .withDataProvider(dataId -> SearchSession.KEY.is(dataId) ? QSearchSession.this : null)
                .withMultilineEnabled(false)
                .withCloseAction(this::close)
                .withReplaceAction(this::replaceAll)
                .build();
        searchComponent.setVisible(false);
        searchComponent.update("", "", false, false);

        searchComponent.addListener(new SearchReplaceComponent.Listener() {
            @Override
            public void searchFieldDocumentChanged() {
                processSearchChanged(findModel, searchComponent.getSearchTextComponent().getText());
            }

            @Override
            public void replaceFieldDocumentChanged() {
                processReplaceChanged(findModel, searchComponent.getReplaceTextComponent().getText());
            }

            @Override
            public void multilineStateChanged() {
            }
        });

        // We don't need to replace with multi-lines here
        final SearchTextArea textArea = (SearchTextArea) SwingUtilities.getAncestorOfClass(SearchTextArea.class, searchComponent.getReplaceTextComponent());
        if (textArea != null) {
            textArea.setMultilineEnabled(false);
        }
        findModel.addObserver(this::fireModelChanged);
    }

    protected static @Nullable RangeExtractor buildExtractor(@NotNull FindModel model) {
        final String what = model.getStringToFind();
        if (what.isBlank()) {
            return null;
        }
        return buildExtractor(model, what);
    }

    protected static @Nullable RangeExtractor buildExtractor(@NotNull FindModel model, String what) {
        if (what.isBlank()) {
            return null;
        }

        final boolean regex = model.isRegularExpressions();
        final int regExFlags = model.isCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        try {
            if (model.isWholeWordsOnly()) {
                final Pattern compile = Pattern.compile(
                        ".*\\b" + (regex ? what : "\\Q" + what + "\\E") + "\\b.*",
                        regExFlags
                );
                final Matcher matcher = compile.matcher("");
                return RangeExtractor.regex(matcher);
            }
            if (regex) {
                final Matcher matcher = Pattern.compile(what, regExFlags).matcher("");
                return RangeExtractor.regex(matcher);
            }
        } catch (Exception ex) {
            return null;
        }
        return RangeExtractor.contains(what, model.isCaseSensitive());
    }

    protected void processModelChanged(FindModel model) {
    }

    protected void processSearchChanged(FindModel model, String text) {
        model.setStringToFind(text);
    }

    protected void processReplaceChanged(FindModel model, String text) {
        model.setStringToReplace(text);
    }

    protected SearchReplaceComponent.Builder initializeComponent(SearchReplaceComponent.Builder builder) {
        return builder;
    }

    public void open(boolean replace) {
        if (!isOpened()) {
            searchComponent.update("", "", replace, false);
            fireModelChanged(findModel);
            searchComponent.setVisible(true);
        } else {
            searchComponent.update(searchComponent.getSearchTextComponent().getText(), searchComponent.getReplaceTextComponent().getText(), replace, false);
        }
        searchComponent.getSearchTextComponent().requestFocusInWindow();
    }

    public boolean isOpened() {
        return searchComponent.isVisible();
    }

    @Override
    public void close() {
        searchComponent.setVisible(false);
        searchComponent.getSearchTextComponent().setText("");
        searchComponent.getReplaceTextComponent().setText("");
    }

    @Override
    public @NotNull FindModel getFindModel() {
        return findModel;
    }

    @Override
    public @NotNull SearchReplaceComponent getComponent() {
        return searchComponent;
    }

    @Override
    public boolean hasMatches() {
        return false;
    }

    @Override
    public void searchForward() {
    }

    @Override
    public void searchBackward() {
    }

    protected void replaceAll() {
    }

    protected void clearStatus() {
        searchComponent.setStatusText("0 results");
        searchComponent.setRegularBackground();
    }

    protected void updateStatus(@Nullable String message) {
        if (message == null) {
            clearStatus();
            searchComponent.setNotFoundBackground();
        } else {
            searchComponent.setStatusText(message);
            searchComponent.setRegularBackground();
        }
    }

    private void fireModelChanged(FindModel model) {
        if (internalChangeFlag) {
            return;
        }

        try {
            internalChangeFlag = true;

            updateEmptyText();
            processModelChanged(model);
            searchComponent.updateActions();
        } finally {
            internalChangeFlag = false;
        }
    }

    private void updateEmptyText() {
        if (searchComponent.getSearchTextComponent() instanceof ComponentWithEmptyText c) {
            c.getEmptyText().setText(buildEmptyText(findModel));
        }
    }

    private String buildEmptyText(FindModel findModel) {
        if (!findModel.getStringToFind().isEmpty()) {
            return "";
        }

        String res = "";
        if (findModel.isCaseSensitive()) {
            res += "match case";
        }
        if (findModel.isWholeWordsOnly()) {
            if (!res.isEmpty()) {
                res += " and ";
            }
            res += "words";
        }
        if (findModel.isRegularExpressions()) {
            if (!res.isEmpty()) {
                res += " and ";
            }
            res += "regex";
        }
        if (res.isEmpty()) {
            return "";
        }
        return StringUtil.capitalize(res);
    }

    public abstract static class RangeExtractor {
        private RangeExtractor() {
        }

        private static RangeExtractor contains(String what, boolean caseSensitive) {
            final SubstringSearch search = caseSensitive ? String::indexOf : UIUtils::indexOfIgnoreCase;
            return new RangeExtractor() {
                @Override
                public @NotNull List<TextRange> extract(String where) {
                    int s = search.search(where, what, 0);
                    if (s < 0) {
                        return List.of();
                    }
                    final List<TextRange> r = new ArrayList<>(2);
                    while (s >= 0) {
                        final int e = s + what.length();
                        r.add(new TextRange(s, e));
                        s = search.search(where, what, e + 1);
                    }
                    return r;
                }
            };
        }

        private static RangeExtractor regex(Matcher matcher) {
            return new RangeExtractor() {
                @Override
                public @NotNull List<TextRange> extract(String where) {
                    final Matcher reset = matcher.reset(where);
                    if (!reset.find()) {
                        return List.of();
                    }
                    final List<TextRange> r = new ArrayList<>();
                    do {
                        final int start = reset.start();
                        final int end = reset.end();
                        r.add(new TextRange(start, end));
                    } while (reset.find());
                    return r;
                }
            };
        }

        public boolean matches(String where) {
            return !extract(where).isEmpty();
        }

        @NotNull
        public abstract List<TextRange> extract(String where);

        @FunctionalInterface
        private interface SubstringSearch {
            int search(String where, String what, int from);

        }
    }

    // I had to redefine all these classes because they use FindSettings which are not required for the plugin
    protected static class ToggleMatchCase extends EditorHeaderToggleAction implements Embeddable {
        public ToggleMatchCase() {
            super(FindBundle.message("find.case.sensitive"),
                    AllIcons.Actions.MatchCase,
                    AllIcons.Actions.MatchCaseHovered,
                    AllIcons.Actions.MatchCaseSelected);
        }

        @Override
        protected boolean isSelected(@NotNull SearchSession session) {
            return session.getFindModel().isCaseSensitive();
        }

        @Override
        protected void setSelected(@NotNull SearchSession session, boolean selected) {
            session.getFindModel().setCaseSensitive(selected);
        }
    }

    protected static class ToggleWholeWordsOnlyAction extends EditorHeaderToggleAction implements Embeddable {
        public ToggleWholeWordsOnlyAction() {
            super(FindBundle.message("find.whole.words"),
                    AllIcons.Actions.Words,
                    AllIcons.Actions.WordsHovered,
                    AllIcons.Actions.WordsSelected);
        }

        @Override
        protected boolean isSelected(@NotNull SearchSession session) {
            return session.getFindModel().isWholeWordsOnly();
        }

        @Override
        protected void setSelected(@NotNull SearchSession session, boolean selected) {
            session.getFindModel().setWholeWordsOnly(selected);
        }
    }

    protected static class ToggleRegex extends EditorHeaderToggleAction implements Embeddable {
        public ToggleRegex() {
            super(FindBundle.message("find.regex"),
                    AllIcons.Actions.Regex,
                    AllIcons.Actions.RegexHovered,
                    AllIcons.Actions.RegexSelected);
        }

        @Override
        protected boolean isSelected(@NotNull SearchSession session) {
            return session.getFindModel().isRegularExpressions();
        }

        @Override
        protected void setSelected(@NotNull SearchSession session, boolean selected) {
            session.getFindModel().setRegularExpressions(selected);
        }
    }

    protected static class CommaSeparatorToggleAction extends EditorHeaderToggleAction implements Embeddable {
        public CommaSeparatorToggleAction() {
            super("&Separate by Comma",
                    KdbIcons.Console.SearchCommaSeparator,
                    KdbIcons.Console.SearchCommaSeparatorHovered,
                    KdbIcons.Console.SearchCommaSeparatorSelected
            );
            getTemplatePresentation().setDescription("Split search by commas and find all tokens");
        }

        @Override
        protected boolean isSelected(@NotNull SearchSession session) {
            return session.getFindModel().isFindAll();
        }

        @Override
        protected void setSelected(@NotNull SearchSession session, boolean selected) {
            session.getFindModel().setFindAll(selected);
        }
    }
}