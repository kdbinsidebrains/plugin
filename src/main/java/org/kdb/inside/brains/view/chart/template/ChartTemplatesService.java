package org.kdb.inside.brains.view.chart.template;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.view.chart.ChartConfig;
import org.kdb.inside.brains.view.chart.types.ChartType;

import java.util.ArrayList;
import java.util.List;

@State(name = "KdbChartingTemplates", storages = {@Storage("kdb-templates.xml")})
public class ChartTemplatesService implements PersistentStateComponent<Element>, DumbAware {
    private static final Logger log = Logger.getInstance(ChartTemplatesService.class);
    private final List<ChartTemplate> templates = new ArrayList<>();

    public ChartTemplatesService() {
    }

    public static ChartTemplatesService getService(@NotNull Project project) {
        return project.getService(ChartTemplatesService.class);
    }

    public void insertTemplate(ChartTemplate template) {
        templates.add(template);
    }

    public List<ChartTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<ChartTemplate> templates) {
        this.templates.clear();
        this.templates.addAll(templates);
    }

    public boolean containsName(String text) {
        return templates.stream().map(ChartTemplate::getName).anyMatch(n -> n.equals(text));
    }

    @Override
    public void loadState(@NotNull Element state) {
        templates.clear();

        final List<Element> template = state.getChildren("template");
        for (Element element : template) {
            final ChartConfig config = loadChartConfig(element);
            final ChartTemplate t = new ChartTemplate(config);
            t.setName(element.getAttributeValue("name"));
            final Element description = element.getChild("description");
            if (description != null) {
                t.setDescription(description.getText());
            }
            t.setQuickAction(Boolean.parseBoolean(element.getAttributeValue("quickAction")));
            templates.add(t);
        }
    }

    private ChartConfig loadChartConfig(Element element) {
        for (ChartType value : ChartType.values()) {
            final Element child = element.getChild(value.getTagName());
            if (child != null) {
                return value.restore(child);
            }
        }
        return null;
    }

    @Override
    public @NotNull Element getState() {
        final Element element = new Element("templates");
        for (ChartTemplate template : templates) {
            try {
                final Element t = new Element("template");
                final ChartConfig config = template.getConfig();
                t.setAttribute("name", template.getName());
                t.setAttribute("quickAction", String.valueOf(template.isQuickAction()));
                if (template.getDescription() != null) {
                    t.addContent(new Element("description").setText(template.getDescription()));
                }
                t.addContent(config.store());
                element.addContent(t);
            } catch (Exception ex) {
                log.warn("Template can't be stored and was ignored: " + template.getName(), ex);
            }
        }
        return element;
    }
}