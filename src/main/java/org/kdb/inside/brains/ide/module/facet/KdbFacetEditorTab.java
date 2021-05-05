package org.kdb.inside.brains.ide.module.facet;

import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QVersion;

import javax.swing.*;
import java.awt.*;

public class KdbFacetEditorTab extends FacetEditorTab {
    private final KdbModuleSettings mySettings;

    private ComboBox<QVersion> versionEditor = null;

    public KdbFacetEditorTab(KdbModuleSettings mySettings) {
        this.mySettings = mySettings;
    }

    @Override
    public @NotNull JComponent createComponent() {
        versionEditor = new ComboBox<>(QVersion.values());
        versionEditor.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends QVersion> list, QVersion value, int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    return;
                }
                append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                append(" ");
                append(value.getDescription(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        });

        versionEditor.setEnabled(false);

        final var builder = FormBuilder.createFormBuilder();
        builder.setVerticalGap(10);
        builder.addComponent(new JLabel("<html><b>KDB Language configuration for this module</b></html>"));
        builder.addLabeledComponent("KDB language version: ", versionEditor);

        builder.addComponent(new JBLabel("* this functionality doesn't work at this moment and default language level is used."));

        final JPanel p = new JPanel(new BorderLayout());
        p.add(builder.getPanel(), BorderLayout.NORTH);
        return p;
    }

    @Override
    public String getDisplayName() {
        return "Q Language Settings";
    }

    @Override
    public boolean isModified() {
        return versionEditor == null || versionEditor.getSelectedItem() != mySettings.getLanguageVersion();
    }

    @Override
    public void reset() {
        versionEditor.setItem(mySettings.getLanguageVersion());
    }

    @Override
    public void apply() {
        if (versionEditor != null) {
            mySettings.setLanguageVersion(versionEditor.getItem());
        }
    }
}
