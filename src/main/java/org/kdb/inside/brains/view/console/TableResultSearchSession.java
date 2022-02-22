package org.kdb.inside.brains.view.console;

import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.SearchSession;
import com.intellij.find.editorHeaderActions.Embeddable;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.ex.CheckboxAction;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.ui.ComponentWithEmptyText;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TableResultSearchSession implements SearchSession {
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
                .withDataProvider(new DataProvider() {
                    @Override
                    public @Nullable Object getData(@NotNull @NonNls String dataId) {
                        return SearchSession.KEY.is(dataId) ? this : null;
                    }
                })
                .withCloseAction(this::close)
                .build();
        searchComponent.setVisible(false);

        searchComponent.update("", "", false, false);

        searchComponent.addListener(new SearchReplaceComponent.Listener() {
            @Override
            public void searchFieldDocumentChanged() {
                findModel.setStringToFind(searchComponent.getSearchTextComponent().getText());
            }

            @Override
            public void replaceFieldDocumentChanged() {
            }

            @Override
            public void multilineStateChanged() {
            }
        });

        findModelChanged();
        findModel.addObserver(m -> findModelChanged());
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

    private void findModelChanged() {
        if (searchComponent.getSearchTextComponent() instanceof ComponentWithEmptyText) {
            String emptyText = getEmptyText();
            ((ComponentWithEmptyText) searchComponent.getSearchTextComponent()).getEmptyText().setText(StringUtil.capitalize(emptyText));
        }
        searchComponent.updateActions();
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
}
