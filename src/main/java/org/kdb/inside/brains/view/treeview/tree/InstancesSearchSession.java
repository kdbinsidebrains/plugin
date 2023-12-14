package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.find.FindBundle;
import com.intellij.find.FindModel;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.find.editorHeaderActions.*;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbInstance;
import org.kdb.inside.brains.view.QSearchSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public class InstancesSearchSession extends QSearchSession {
    private final InstancesTree instancesTree;
    private final List<KdbInstance> foundInstances = new ArrayList<>();
    private final Set<KdbInstance> excludedInstances = new HashSet<>();
    private final Map<KdbInstance, EnumMap<FilterContext, List<TextRange>>> instanceDetails = new LinkedHashMap<>();
    private int selectedIndex = -1;

    public InstancesSearchSession(@Nullable Project project, @NotNull InstancesTree instancesTree, @NotNull FindModel findModel) {
        super(project, instancesTree, findModel);
        this.instancesTree = instancesTree;
    }

    // Copy of SpeedSearchUtil#applySpeedSearchHighlighting but adds strikeout
    private static void applySpeedSearchHighlighting(@NotNull SimpleColoredComponent coloredComponent,
                                                     @Nullable Iterable<? extends TextRange> ranges,
                                                     boolean selected, boolean excluded) {
        Iterator<? extends TextRange> rangesIterator = ranges != null ? ranges.iterator() : null;
        if (rangesIterator == null || !rangesIterator.hasNext()) return;
        Color bg = UIUtil.getTreeBackground(selected, true);

        SimpleColoredComponent.ColoredIterator coloredIterator = coloredComponent.iterator();
        TextRange range = rangesIterator.next();
        main:
        while (coloredIterator.hasNext()) {
            coloredIterator.next();
            int offset = coloredIterator.getOffset();
            int endOffset = coloredIterator.getEndOffset();
            if (!range.intersectsStrict(offset, endOffset)) continue;
            SimpleTextAttributes attributes = coloredIterator.getTextAttributes();
            // this part was added
            int i = attributes.getStyle() | SimpleTextAttributes.STYLE_SEARCH_MATCH;
            if (excluded) {
                i |= SimpleTextAttributes.STYLE_STRIKEOUT;
            }
            // the end of the part
            SimpleTextAttributes highlighted = new SimpleTextAttributes(bg, attributes.getFgColor(), null, i);
            do {
                if (range.getStartOffset() > offset) {
                    offset = coloredIterator.split(range.getStartOffset() - offset, attributes);
                }
                if (range.getEndOffset() <= endOffset) {
                    offset = coloredIterator.split(range.getEndOffset() - offset, highlighted);
                    if (rangesIterator.hasNext()) {
                        range = rangesIterator.next();
                    } else {
                        break main;
                    }
                } else {
                    coloredIterator.split(endOffset - offset, highlighted);
                    continue main;
                }
            }
            while (range.intersectsStrict(offset, endOffset));
        }
    }

    @Override
    protected SearchReplaceComponent.Builder initializeComponent(SearchReplaceComponent.Builder builder) {
        return builder.addExtraSearchActions(
                        new ToggleMatchCase(),
                        new ToggleWholeWordsOnlyAction(),
                        new ToggleRegex()
                )
                .addPrimarySearchActions(
                        new StatusTextAction(),
                        new PrevOccurrenceAction(),
                        new NextOccurrenceAction(),
                        new Separator(),
                        createFilterGroup()
                )
                .addPrimaryReplaceActions(
                        new ReplaceAction(),
                        new ReplaceAllAction(),
                        new ExcludeAction()
                );
    }

    @Override
    public boolean hasMatches() {
        return !foundInstances.isEmpty();
    }

    @Override
    public void searchBackward() {
        if (selectedIndex > 0) {
            updateSelection(selectedIndex - 1);
        } else if (selectedIndex == 0) {
            updateSelection(foundInstances.size() - 1);
        }
    }

    @Override
    public void searchForward() {
        final int size = foundInstances.size() - 1;
        if (selectedIndex < size) {
            updateSelection(selectedIndex + 1);
        } else if (selectedIndex == size) {
            updateSelection(0);
        }
    }

    private void updateSelection(int index) {
        if (index < 0 || index > foundInstances.size() - 1) {
            selectedIndex = -1;
            updateStatus(null);
        } else {
            selectedIndex = index;
            updateStatus((selectedIndex + 1) + "/" + foundInstances.size());
            TreeUtil.selectPath(instancesTree, foundInstances.get(selectedIndex).getTreePath());
        }
    }

    @Override
    public void close() {
        super.close();
        clearCurrentState();
    }

    public void customizeSearchItem(KdbInstance instance, SimpleColoredComponent coloredComponent, boolean selected) {
        final Map<FilterContext, List<TextRange>> details = instanceDetails.get(instance);
        if (details == null || details.isEmpty()) {
            return;
        }

        final List<TextRange> ranges = new ArrayList<>(details.getOrDefault(FilterContext.IN_NAME, List.of()));
        final List<TextRange> symbols = details.getOrDefault(FilterContext.IN_ADDRESS, List.of());
        if (!symbols.isEmpty()) {
            final int nameShift = instance.getName().length() + 2;
            for (TextRange range : symbols) {
                ranges.add(range.shiftRight(nameShift));
            }
        }

        if (!ranges.isEmpty()) {
            applySpeedSearchHighlighting(coloredComponent, ranges, selected, excludedInstances.contains(instance));
        }
    }

    @Override
    protected void processModelChanged(FindModel model) {
        final KdbInstance oldSelection = getSelectedInstance();

        clearCurrentState();

        final String text = model.getStringToFind();
        if (text.isEmpty()) {
            clearStatus();
        } else {
            final RangeExtractor extractor = buildExtractor(model);
            if (extractor != null) {
                final List<FilterContext> contexts = FilterContext.valueOf(model.getSearchContext());

                TreeUtil.treeTraverser(instancesTree)
                        .filter(KdbInstance.class)
                        .forEach(i -> {
                            for (FilterContext context : contexts) {
                                final List<TextRange> ranges = context.extract(i, extractor);
                                if (!ranges.isEmpty()) {
                                    instanceDetails.computeIfAbsent(i, k -> new EnumMap<>(FilterContext.class)).put(context, ranges);
                                }
                            }
                            if (instanceDetails.containsKey(i)) {
                                foundInstances.add(i);
                            }
                        });

                // Restore active instance, if possible
                final int i = oldSelection != null ? foundInstances.indexOf(oldSelection) : -1;
                updateSelection(Math.max(i, 0));
            }
        }
        instancesTree.repaint();
    }

    private void clearCurrentState() {
        selectedIndex = -1;
        foundInstances.clear();
        instanceDetails.clear();
        excludedInstances.clear();
    }

    private KdbInstance getSelectedInstance() {
        if (selectedIndex == -1) {
            return null;
        }
        return foundInstances.get(selectedIndex);
    }

    @Override
    protected void replaceAll() {
        foundInstances.stream().filter(i -> !excludedInstances.contains(i)).forEach(this::doReplace);
        processModelChanged(getFindModel());
    }

    private void replaceCurrent() {
        doReplace(foundInstances.get(selectedIndex));
        processModelChanged(getFindModel());
    }

    private void doReplace(KdbInstance instance) {
        final String replacement = getFindModel().getStringToReplace();
        if (replacement.isBlank() || selectedIndex == -1) {
            return;
        }

        final EnumMap<FilterContext, List<TextRange>> details = instanceDetails.get(instance);
        for (Map.Entry<FilterContext, List<TextRange>> entry : details.entrySet()) {
            entry.getKey().replace(instance, replacement, entry.getValue());
        }
    }

    @NotNull
    private ShowFilterPopupGroup createFilterGroup() {
        final ShowFilterPopupGroup g = new ShowFilterPopupGroup();
        g.removeAll();
        g.add(new ToggleAnywhereAction());
        for (FilterContext value : FilterContext.VALUES) {
            g.add(new ToggleCustomContextAction(value));
        }
        return g;
    }

    private enum FilterContext {
        IN_NAME("In Name", FindModel.SearchContext.IN_STRING_LITERALS, InstanceItem::getName) {
            @Override
            void doReplace(KdbInstance instance, String value) {
                instance.setName(value);
            }
        },
        IN_ADDRESS("In Address", FindModel.SearchContext.EXCEPT_STRING_LITERALS, KdbInstance::toAddress) {
            @Override
            void doReplace(KdbInstance instance, String value) {
                instance.updateAddress(value);
            }
        },
        ;

        private static final FilterContext[] VALUES = FilterContext.values();
        private final String text;
        private final FindModel.SearchContext searchContext;
        private final Function<KdbInstance, String> fieldSupplier;

        FilterContext(String text, FindModel.SearchContext searchContext, Function<KdbInstance, String> fieldSupplier) {
            this.text = text;
            this.searchContext = searchContext;
            this.fieldSupplier = fieldSupplier;
        }

        static List<FilterContext> valueOf(FindModel.SearchContext searchContext) {
            if (searchContext == FindModel.SearchContext.IN_STRING_LITERALS) {
                return List.of(IN_NAME);
            }
            if (searchContext == FindModel.SearchContext.EXCEPT_STRING_LITERALS) {
                return List.of(IN_ADDRESS);
            }
            return List.of(IN_NAME, IN_ADDRESS);
        }

        public List<TextRange> extract(KdbInstance instance, QSearchSession.RangeExtractor extractor) {
            return extractor.extract(fieldSupplier.apply(instance));
        }

        public void replace(KdbInstance instance, String replace, List<TextRange> ranges) {
            final ArrayList<TextRange> textRanges = new ArrayList<>(ranges);
            textRanges.sort((o1, o2) -> o2.getStartOffset() - o1.getStartOffset());

            String apply = fieldSupplier.apply(instance);
            for (TextRange range : textRanges) {
                apply = range.replace(apply, replace);
            }
            doReplace(instance, apply);
        }

        abstract void doReplace(KdbInstance instance, String value);
    }

    private static abstract class ButtonAction extends DumbAwareAction implements CustomComponentAction, ActionListener {
        private final @NlsActions.ActionText String myTitle;

        ButtonAction(@NotNull @NlsActions.ActionText String title) {
            myTitle = title;
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }

        @NotNull
        @Override
        public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
            JButton button = new JButton(myTitle);
            button.setEnabled(false);
            button.setFocusable(false);
            button.addActionListener(this);
            return button;
        }

        @Override
        public final void update(@NotNull AnActionEvent e) {
            JButton button = (JButton) e.getPresentation().getClientProperty(COMPONENT_KEY);
            if (button != null) {
                update(button);
            }
        }

        @Override
        public final void actionPerformed(@NotNull AnActionEvent e) {
            onClick();
        }

        @Override
        public final void actionPerformed(ActionEvent e) {
            onClick();
        }

        protected abstract void update(@NotNull JButton button);

        protected abstract void onClick();
    }

    private static class ToggleCustomContextAction extends EditorHeaderSetSearchContextAction {
        protected ToggleCustomContextAction(FilterContext context) {
            super(context.text, context.searchContext);
        }
    }

    private class ReplaceAction extends ButtonAction {
        ReplaceAction() {
            super(ApplicationBundle.message("editorsearch.replace.action.text"));
        }

        @Override
        protected void update(@NotNull JButton button) {
            button.setEnabled(!foundInstances.isEmpty() && !getFindModel().getStringToReplace().isBlank() && selectedIndex != -1);
        }

        @Override
        protected void onClick() {
            replaceCurrent();
        }
    }

    private class ReplaceAllAction extends ButtonAction {
        ReplaceAllAction() {
            super(ApplicationBundle.message("editorsearch.replace.all.action.text"));
        }

        @Override
        protected void update(@NotNull JButton button) {
            button.setEnabled(!foundInstances.isEmpty() && !getFindModel().getStringToReplace().isBlank() && selectedIndex != -1);
        }

        @Override
        protected void onClick() {
            replaceAll();
        }
    }

    private class ExcludeAction extends ButtonAction {
        ExcludeAction() {
            super(FindBundle.message("button.exclude"));
        }

        @Override
        protected void update(@NotNull JButton button) {
            button.setEnabled(!foundInstances.isEmpty());
            button.setText(selectedIndex != -1 && excludedInstances.contains(foundInstances.get(selectedIndex))
                    ? FindBundle.message("button.include")
                    : FindBundle.message("button.exclude"));
        }

        @Override
        protected void onClick() {
            if (selectedIndex != -1) {
                final KdbInstance e = foundInstances.get(selectedIndex);
                if (excludedInstances.contains(e)) {
                    excludedInstances.remove(e);
                } else {
                    excludedInstances.add(e);
                }
            }
            searchForward();
        }
    }
}
