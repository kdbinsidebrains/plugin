package org.kdb.inside.brains.lang.inspection.exclusions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@com.intellij.openapi.components.State(name = "UndefinedExclusions", storages = {@Storage("kdb-settings.xml")})
public class UndefinedExclusionsService implements PersistentStateComponent<Element> {
    private static UndefinedExclusionsService instance = null;
    private final Set<UndefinedExclusion> exclusions = new HashSet<>();

    public static UndefinedExclusionsService getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getService(UndefinedExclusionsService.class);
        }
        return instance;
    }

    public void addExclusion(UndefinedExclusion exclusion) {
        if (exclusion != null) {
            exclusions.add(exclusion);
        }
    }

    public void removeExclusion(UndefinedExclusion exclusion) {
        if (exclusion != null) {
            exclusions.remove(exclusion);
        }
    }

    public void clear() {
        exclusions.clear();
    }

    public Set<UndefinedExclusion> getExclusions() {
        return exclusions;
    }

    public void setExclusions(Collection<UndefinedExclusion> exclusions) {
        this.exclusions.clear();
        this.exclusions.addAll(exclusions);
    }

    @Override
    public @Nullable Element getState() {
        if (exclusions.isEmpty()) {
            return null;
        }

        final Element element = new Element("exclusions");
        for (UndefinedExclusion exclusion : exclusions) {
            element.addContent(new Element("pattern")
                    .setText(exclusion.name())
                    .setAttribute("regex", String.valueOf(exclusion.regex()))
            );
        }
        return element;
    }

    @Override
    public void loadState(@NotNull Element state) {
        exclusions.clear();
        final List<Element> pattern = state.getChildren("pattern");
        for (Element element : pattern) {
            final boolean regex = Boolean.parseBoolean(element.getAttributeValue("regex", "false"));
            exclusions.add(new UndefinedExclusion(element.getText(), regex));
        }
    }
}
