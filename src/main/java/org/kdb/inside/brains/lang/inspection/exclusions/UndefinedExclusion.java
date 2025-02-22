package org.kdb.inside.brains.lang.inspection.exclusions;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;

public record UndefinedExclusion(String name,
                                 boolean regex) implements Comparator<UndefinedExclusion>, Comparable<UndefinedExclusion> {
    public static boolean isExcluded(String qualifiedName) {
        return isExcluded(qualifiedName, UndefinedExclusionsService.getInstance().getExclusions());
    }

    public static boolean isExcluded(String qualifiedName, Collection<UndefinedExclusion> exclusions) {
        return exclusions.stream().anyMatch(e -> e.regex() ? qualifiedName.matches(e.name()) : qualifiedName.equals(e.name()));
    }

    @Override
    public int compareTo(@NotNull UndefinedExclusion o) {
        return name.compareTo(o.name);
    }

    @Override
    public int compare(UndefinedExclusion o1, UndefinedExclusion o2) {
        return o1.name.compareTo(o2.name);
    }
}