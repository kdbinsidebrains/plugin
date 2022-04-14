package org.kdb.inside.brains.view.console;

import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.SearchSession;
import com.intellij.find.editorHeaderActions.Embeddable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.ui.ComponentWithEmptyText;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TableResultSearchSession implements SearchSession {
    private static final int DELAY_TIMEOUT = 300;
    private final Lock delayLock = new ReentrantLock();
    private Timer searchTime;
    private DelayText delayText;
    private boolean delaySearchEnabled = false;

    private final FindModel findModel;
    private final SearchReplaceComponent searchComponent;

    public TableResultSearchSession(JComponent component, Project project, @NotNull FindModel findModel) {
        this.findModel = findModel;
        searchComponent = SearchReplaceComponent
                .buildFor(project, component)
                .addExtraSearchActions(
                        SearchTypeAction.matchCase(findModel),
                        SearchTypeAction.wholeWords(findModel),
                        SearchTypeAction.regex(findModel)
                )
                .addPrimarySearchActions(new DelaySearchAction())
                .withDataProvider(dataId -> SearchSession.KEY.is(dataId) ? TableResultSearchSession.this : null)
                .withMultilineEnabled(false)
                .withCloseAction(this::close)
                .build();
        searchComponent.setVisible(false);

        searchComponent.update("", "", false, false);

        searchComponent.addListener(new SearchReplaceComponent.Listener() {
            @Override
            public void searchFieldDocumentChanged() {
                fireTextChanged(searchComponent.getSearchTextComponent().getText());
            }

            @Override
            public void replaceFieldDocumentChanged() {
            }

            @Override
            public void multilineStateChanged() {
            }
        });

        fireModelChanged();
        findModel.addObserver(m -> fireModelChanged());
    }

    @Override
    public @NotNull FindModel getFindModel() {
        return findModel;
    }

    @Override
    public @NotNull SearchReplaceComponent getComponent() {
        return searchComponent;
    }

    public boolean isOpened() {
        return searchComponent.isVisible();
    }

    public boolean isDelaySearchEnabled() {
        return delaySearchEnabled;
    }

    public void setDelaySearchEnabled(boolean delaySearchEnabled) {
        this.delaySearchEnabled = delaySearchEnabled;
    }

    public void open() {
        searchComponent.setVisible(true);
        searchComponent.getSearchTextComponent().requestFocusInWindow();
    }

    @Override
    public void close() {
        searchComponent.setVisible(false);
        searchComponent.getSearchTextComponent().setText("");
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

    private void fireModelChanged() {
        if (searchComponent.getSearchTextComponent() instanceof ComponentWithEmptyText) {
            String emptyText = getEmptyText();
            ((ComponentWithEmptyText) searchComponent.getSearchTextComponent()).getEmptyText().setText(StringUtil.capitalize(emptyText));
        }
        searchComponent.updateActions();
    }

    private void fireTextChanged(String text) {
        if (!delaySearchEnabled) {
            findModel.setStringToFind(text);
            return;
        }

        delayLock.lock();
        try {
            delayText = new DelayText(text);
            if (searchTime != null) {
                return;
            }

            searchTime = new Timer(DELAY_TIMEOUT, e -> {
                boolean update = false;
                delayLock.lock();
                try {
                    if (searchTime == null) {
                        return;
                    }
                    if (System.currentTimeMillis() - delayText.time > DELAY_TIMEOUT) {
                        update = true;
                        searchTime.stop();
                        searchTime = null;
                    }
                } finally {
                    delayLock.unlock();
                }
                if (update) {
                    findModel.setStringToFind(delayText.text);
                }
            });
            searchTime.start();
        } finally {
            delayLock.unlock();
        }
    }


    private String getEmptyText() {
        if (!findModel.getStringToFind().isEmpty()) {
            return "";
        }

        final SmartList<String> chosenOptions = new SmartList<>();
        if (findModel.isCaseSensitive()) {
            chosenOptions.add("match case");
        }
        if (findModel.isWholeWordsOnly()) {
            chosenOptions.add("words");
        }
        if (findModel.isRegularExpressions()) {
            chosenOptions.add("regex");
        }
        if (chosenOptions.isEmpty()) {
            return "";
        }
        if (chosenOptions.size() == 1) {
            return FindBundle.message("emptyText.used.option", chosenOptions.get(0));
        }
        return FindBundle.message("emptyText.used.options", chosenOptions.get(0), chosenOptions.get(1));
    }

    private static class DelayText {
        private final long time = System.currentTimeMillis();
        private final String text;

        public DelayText(String text) {
            this.text = text;
        }
    }

    public static class SearchTypeAction extends CheckboxAction implements DumbAware, Embeddable, TooltipDescriptionProvider {
        private final Consumer<Boolean> consumer;
        private final Supplier<Boolean> supplier;

        public SearchTypeAction(String text, Icon icon, Icon hoveredIcon, Icon selectedIcon, Supplier<Boolean> supplier, Consumer<Boolean> consumer) {
            super(text);
            this.supplier = supplier;
            this.consumer = consumer;
            getTemplatePresentation().setIcon(icon);
            getTemplatePresentation().setHoveredIcon(hoveredIcon);
            getTemplatePresentation().setSelectedIcon(selectedIcon);
        }

        public static SearchTypeAction matchCase(FindModel findModel) {
            return new SearchTypeAction(
                    FindBundle.message("find.case.sensitive"),
                    AllIcons.Actions.MatchCase,
                    AllIcons.Actions.MatchCaseHovered,
                    AllIcons.Actions.MatchCaseSelected,
                    findModel::isCaseSensitive, findModel::setCaseSensitive
            );
        }

        public static SearchTypeAction regex(FindModel findModel) {
            return new SearchTypeAction(
                    FindBundle.message("find.regex"),
                    AllIcons.Actions.Regex,
                    AllIcons.Actions.RegexHovered,
                    AllIcons.Actions.RegexSelected,
                    findModel::isRegularExpressions, findModel::setRegularExpressions
            ) {
                @Override
                public void setSelected(@NotNull AnActionEvent e, boolean state) {
                    super.setSelected(e, state);
                    findModel.setWholeWordsOnly(false);
                }
            };
        }

        public static SearchTypeAction wholeWords(FindModel findModel) {
            return new SearchTypeAction(
                    FindBundle.message("find.whole.words"),
                    AllIcons.Actions.Words,
                    AllIcons.Actions.WordsHovered,
                    AllIcons.Actions.WordsSelected,
                    findModel::isWholeWordsOnly, findModel::setWholeWordsOnly
            ) {
                @Override
                public void setSelected(@NotNull AnActionEvent e, boolean state) {
                    super.setSelected(e, state);
                    findModel.setRegularExpressions(false);
                }
            };
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }

        @NotNull
        @Override
        public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
            JComponent customComponent = super.createCustomComponent(presentation, place);
            customComponent.setFocusable(false);
            customComponent.setOpaque(false);
            return customComponent;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return supplier.get();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            consumer.accept(state);
        }
    }

    private class DelaySearchAction extends ToggleAction {
        public DelaySearchAction() {
            super(null, "<html><strong>Delay Search Update</strong><br>Wait " + DELAY_TIMEOUT + "ms before search update after the text change to reduce UI freezes for huge table result.</html>", KdbIcons.Console.DelaySearchUpdate);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return delaySearchEnabled;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            delaySearchEnabled = state;
        }

        @Override
        public boolean displayTextInToolbar() {
            return true;
        }
    }
}
