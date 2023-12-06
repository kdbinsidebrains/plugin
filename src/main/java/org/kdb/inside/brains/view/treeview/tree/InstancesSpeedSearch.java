package org.kdb.inside.brains.view.treeview.tree;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TreeSpeedSearch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.core.KdbInstance;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

public class InstancesSpeedSearch extends TreeSpeedSearch {
    private final TheSpeedSearchComparator comparator = new TheSpeedSearchComparator();

    InstancesSpeedSearch(JTree tree) {
        super(tree, true, InstancesSpeedSearch::pathToString);
        setComparator(comparator);
    }

    private static String pathToString(TreePath path) {
        return itemToString(path.getLastPathComponent());
    }

    private static String itemToString(Object c) {
        if (c instanceof KdbInstance i) {
            return i.getCanonicalName() + " " + i.toSymbol();
        }
        return c.toString();
    }

    @Override
    protected boolean compare(@NotNull String text, @Nullable String pattern) {
        if (pattern == null) {
            return false;
        }
        return comparator.matchingFragmentsStrict(pattern, text, true) != null;
    }

    public boolean isObjectFilteredOut(Object o) {
        final String str = itemToString(o);
        return str == null || !compare(str, comparator.getRecentSearchText());
    }

    private static class TheSpeedSearchComparator extends SpeedSearchComparator {
        @Override
        public int matchingDegree(String pattern, String text) {
            return matchingFragments(pattern, text) != null ? 1 : 0;
        }

        @Override
        public Iterable<TextRange> matchingFragments(@NotNull String pattern, @NotNull String text) {
            return matchingFragmentsStrict(pattern, text, false);
        }

        private List<TextRange> matchingFragmentsStrict(@NotNull String pattern, @NotNull String text, boolean strict) {
            myRecentSearchText = pattern;

            final String[] strings = pattern.split(" ");
            if (strings.length == 0) {
                return List.of();
            }

            final ArrayList<TextRange> r = new ArrayList<>(strings.length);
            for (String token : strings) {
                int index = StringUtil.indexOfIgnoreCase(text, token, 0);
                if (index < 0) {
                    if (strict) {
                        return null;
                    }
                } else {
                    r.add(TextRange.from(index, token.length()));
                }
            }
            return r;
        }
    }
}
