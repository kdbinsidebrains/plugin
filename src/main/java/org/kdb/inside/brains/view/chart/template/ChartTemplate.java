package org.kdb.inside.brains.view.chart.template;

import org.kdb.inside.brains.view.chart.ChartConfig;

import javax.swing.*;
import java.util.Objects;

public class ChartTemplate {
    private String name;
    private String description;
    private boolean quickAction;

    private ChartConfig config;

    public ChartTemplate(ChartConfig config) {
        this("", null, config);
    }

    public ChartTemplate(String name, String description, ChartConfig config) {
        this(name, description, config, false);
    }

    public ChartTemplate(String name, String description, ChartConfig config, boolean quickAction) {
        this.name = name;
        this.description = description;
        this.config = config.copy();
        this.quickAction = quickAction;
    }

    public Icon getIcon() {
        return config.getType().getIcon();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChartConfig getConfig() {
        return config;
    }

    public void setConfig(ChartConfig config) {
        this.config = config.copy();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isQuickAction() {
        return quickAction;
    }

    public void setQuickAction(boolean quickAction) {
        this.quickAction = quickAction;
    }

    public ChartTemplate copy() {
        final ChartTemplate template = new ChartTemplate(config);
        template.name = name;
        template.description = description;
        template.quickAction = quickAction;
        return template;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChartTemplate template = (ChartTemplate) o;
        return quickAction == template.quickAction && Objects.equals(config, template.config) && Objects.equals(name, template.name) && Objects.equals(description, template.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, quickAction, config);
    }
}