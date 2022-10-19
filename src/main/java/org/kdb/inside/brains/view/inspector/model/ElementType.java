package org.kdb.inside.brains.view.inspector.model;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import icons.KdbIcons;

import javax.swing.*;

public enum ElementType {
    TABLES("Tables", KdbIcons.Node.GroupTables, TableElement.class),
    FUNCTIONS("Functions", KdbIcons.Node.GroupFunctions, FunctionElement.class),
    VARIABLES("Variables", KdbIcons.Node.GroupVariables, VariableElement.class);

    private final String text;
    private final Icon icon;
    private final Class<? extends TreeElement> aClass;

    ElementType(String text, Icon icon, Class<? extends TreeElement> aClass) {
        this.text = text;
        this.icon = icon;
        this.aClass = aClass;
    }

    public String getText() {
        return text;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isIt(TreeElement c) {
        return aClass.isAssignableFrom(c.getClass());
    }
}
