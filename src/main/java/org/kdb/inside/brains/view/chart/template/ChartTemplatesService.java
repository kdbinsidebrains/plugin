package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.jgoodies.common.base.Strings;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.ArrayList;
import java.util.List;

@State(name = "KdbChartingTemplates", storages = {@Storage("kdb-templates.xml")})
public class ChartTemplatesService implements PersistentStateComponent<Element>, DumbAware {
    private static final Logger log = Logger.getInstance(ChartTemplatesService.class);
    private final List<ChartTemplate> templates = new ArrayList<>();
    private long lastUpdateNanos = 0;

    public ChartTemplatesService() {
    }

    public static ChartTemplatesService getService(@NotNull Project project) {
        return project.getService(ChartTemplatesService.class);
    }

    public long getLastUpdateNanos() {
        return lastUpdateNanos;
    }

    public void addTemplate(ChartTemplate template) {
        templates.add(template);
        lastUpdateNanos = System.nanoTime();
    }

    public List<ChartTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<ChartTemplate> templates) {
        this.templates.clear();
        this.templates.addAll(templates);
        lastUpdateNanos = System.nanoTime();
    }

    public boolean containsName(String text) {
        return templates.stream().map(ChartTemplate::getName).anyMatch(n -> n.equals(text));
    }

    @Override
    public void loadState(@NotNull Element state) {
        templates.clear();

        final List<Element> template = state.getChildren();
        for (Element element : template) {
            if ("template".equals(element.getName())) {
                element = fixOldFormat(element);
            }

            final ChartTemplate t = loadChartTemplate(element);
            if (t != null) {
                templates.add(t);
            }
        }

        lastUpdateNanos = System.nanoTime();
    }

    private Element fixOldFormat(Element element) {
        final Element desc = element.getChild("description");
        if (desc != null) {
            element.removeContent(desc);
        }
        final Element e = element.getChildren().get(0);
        e.setAttribute("name", element.getAttributeValue("name", ""));
        e.setAttribute("quickAction", element.getAttributeValue("quickAction", "false"));
        if (desc != null) {
            e.addContent(desc);
        }
        return e;
    }

    private @Nullable ChartTemplate loadChartTemplate(@Nullable Element element) {
        if (element == null) {
            return null;
        }

        final String name = element.getAttributeValue("name");
        if (Strings.isEmpty(name)) {
            return null;
        }

        try {
            final Element child = element.getChild("description");
            final String description = child != null ? child.getText().trim() : null;
            final boolean quickAction = Boolean.parseBoolean(element.getAttributeValue("quickAction", "false"));
            final ChartType type = ChartType.byName(element.getName());
            if (type == null) {
                return null;
            }

            final ChartConfig config = type.restore(element);
            return new ChartTemplate(name, description, config, quickAction);
        } catch (Exception ex) {
            log.warn("Template can't be restored and was ignored: " + name, ex);
            return null;
        }
    }

    @Override
    public @NotNull Element getState() {
        final Element element = new Element("templates");
        for (ChartTemplate template : templates) {
            try {
                final Element t = template.getConfig().store();
                t.setAttribute("name", template.getName());
                t.setAttribute("quickAction", String.valueOf(template.isQuickAction()));
                final String description = template.getDescription();
                if (Strings.isNotEmpty(description)) {
                    t.addContent(new Element("description").setText(description));
                }
                element.addContent(t);
            } catch (Exception ex) {
                log.warn("Template can't be stored and was ignored: " + template.getName(), ex);
            }
        }
        return element;
    }
}