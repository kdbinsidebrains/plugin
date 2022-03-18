package org.kdb.inside.brains.view.chart;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class DynamicToggleAction extends ToggleAction {
    private final IsSelected isSelected;
    private final SetSelected setSelected;

    public DynamicToggleAction(String text, Icon icon, IsSelected isSelected, SetSelected setSelected) {
        this(text, null, icon, isSelected, setSelected);
    }

    public DynamicToggleAction(String text, String description, Icon icon, IsSelected isSelected, SetSelected setSelected) {
        super(text, description, icon);
        this.isSelected = isSelected;
        this.setSelected = setSelected;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        return isSelected.isSelected();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
        setSelected.setSelected(state);
    }

    @FunctionalInterface
    public interface IsSelected {
        boolean isSelected();
    }

    @FunctionalInterface
    public interface SetSelected {
        void setSelected(boolean selected);
    }
}
