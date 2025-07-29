package org.kdb.inside.brains.view.console.table;

import com.intellij.find.FindModel;
import com.intellij.find.SearchReplaceComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.action.BgtToggleAction;
import org.kdb.inside.brains.view.QSearchSession;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

class TableResultSearchSession extends QSearchSession {
    private Timer searchTime;
    private DelayText delayText;
    private boolean delaySearchEnabled = false;

    private static final int DELAY_TIMEOUT = 300;
    private final Lock delayLock = new ReentrantLock();
    private QSearchSession.RangeExtractor[] rangeExtractors = NO_EXTRACTORS;

    public TableResultSearchSession(@Nullable Project project, @NotNull JComponent component) {
        super(project, component);
    }

    @Override
    protected SearchReplaceComponent.Builder initializeComponent(SearchReplaceComponent.Builder builder) {
        return builder.addExtraSearchActions(
                        new CommaSeparatorToggleAction(),
                        new ToggleMatchCase(),
                        new ToggleWholeWordsOnlyAction(),
                        new ToggleRegex()
                )
                .addPrimarySearchActions(new DelaySearchAction());
    }

    public void setDelaySearchEnabled(boolean delaySearchEnabled) {
        this.delaySearchEnabled = delaySearchEnabled;
    }

    public List<TextRange> extract(String text) {
        if (rangeExtractors.length == 0) {
            return List.of();
        }
        if (rangeExtractors.length == 1) {
            return rangeExtractors[0].extract(text);
        }

        final List<TextRange> res = new ArrayList<>(rangeExtractors.length);
        for (QSearchSession.RangeExtractor extractor : rangeExtractors) {
            res.addAll(extractor.extract(text));
        }
        return res;
    }

    public RowFilter<TableModel, Integer> createTableFilter() {
        if (rangeExtractors.length == 0) {
            return null;
        }
        if (rangeExtractors.length == 1) {
            return new TableRowFilter(rangeExtractors[0]::matches);
        }
        return RowFilter.andFilter(Stream.of(rangeExtractors).map(e -> new TableRowFilter(e::matches)).toList());
    }

    @Override
    protected void processModelChanged(FindModel model) {
        final String text = model.getStringToFind();
        if (model.isFindAll()) {
            final String[] split = text.split(",");
            if (split.length == 0) {
                rangeExtractors = NO_EXTRACTORS;
            } else if (split.length == 1) {
                final QSearchSession.RangeExtractor extractor = buildExtractor(model, split[0]);
                rangeExtractors = extractor == null ? NO_EXTRACTORS : new QSearchSession.RangeExtractor[]{extractor};
            } else {
                rangeExtractors = Stream.of(split).map(v -> buildExtractor(model, v)).filter(Objects::nonNull).toArray(QSearchSession.RangeExtractor[]::new);
            }
        } else {
            final QSearchSession.RangeExtractor extractor = buildExtractor(model, text);
            rangeExtractors = extractor == null ? NO_EXTRACTORS : new QSearchSession.RangeExtractor[]{extractor};
        }
    }

    @Override
    protected void processSearchChanged(FindModel model, String text) {
        if (!delaySearchEnabled) {
            model.setStringToFind(text);
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
                    model.setStringToFind(delayText.text);
                }
            });
            searchTime.start();
        } finally {
            delayLock.unlock();
        }
    }

    private static class DelayText {
        private final long time = System.currentTimeMillis();
        private final String text;

        public DelayText(String text) {
            this.text = text;
        }
    }

    @FunctionalInterface
    private interface ValueFilter {
        boolean check(String value);
    }

    private static class TableRowFilter extends RowFilter<TableModel, Integer> {
        private final ValueFilter filter;

        public TableRowFilter(ValueFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean include(Entry<? extends TableModel, ? extends Integer> value) {
            int index = value.getValueCount();
            while (--index >= 0) {
                final String stringValue = value.getStringValue(index);
                if (filter.check(stringValue)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class DelaySearchAction extends BgtToggleAction {
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
