package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import icons.KdbIcons;

import javax.swing.*;

public enum ElementType {
    TABLES("Tables", KdbIcons.Node.GroupTables, TableElement.class),
    FUNCTIONS("Functions", KdbIcons.Node.GroupFunctions, FunctionElement.class),
    VARIABLES("Variables", KdbIcons.Node.GroupVariables, VariableElement.class);

    private final String text;
    private final Icon groupIcon;
    private final Class<? extends TreeElement> aClass;

    ElementType(String text, Icon groupIcon, Class<? extends TreeElement> aClass) {
        this.text = text;
        this.groupIcon = groupIcon;
        this.aClass = aClass;
    }

    public String getText() {
        return text;
    }

    public Icon getGroupIcon() {
        return groupIcon;
    }

    public boolean isIt(TreeElement c) {
        return aClass.isAssignableFrom(c.getClass());
    }
}
