package org.kdb.inside.brains.settings;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.intellij.openapi.options.colors.RainbowColorSettingsPage;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.lang.QSyntaxHighlighter;

import javax.swing.*;
import java.util.Map;

public final class KdbColorSettingsPage implements RainbowColorSettingsPage, ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Primitive", QSyntaxHighlighter.ATOM),
            new AttributesDescriptor("List", QSyntaxHighlighter.VECTOR),
            new AttributesDescriptor("Char", QSyntaxHighlighter.CHAR),
            new AttributesDescriptor("String", QSyntaxHighlighter.STRING),
            new AttributesDescriptor("Operator", QSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Iterator", QSyntaxHighlighter.ITERATOR),
            new AttributesDescriptor("Symbol", QSyntaxHighlighter.SYMBOL),
            new AttributesDescriptor("Variable", QSyntaxHighlighter.VARIABLE),
            new AttributesDescriptor("Keyword", QSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Command", QSyntaxHighlighter.COMMAND),
            new AttributesDescriptor("Comment", QSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Import", QSyntaxHighlighter.IMPORT),
            new AttributesDescriptor("Namespace", QSyntaxHighlighter.CONTEXT),
            new AttributesDescriptor("Type cast", QSyntaxHighlighter.TYPECAST),
    };

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.File;
    }

    @Override
    public @NotNull String getDisplayName() {
        return QLanguage.INSTANCE.getDisplayName();
    }

    @Override
    public Language getLanguage() {
        return QLanguage.INSTANCE;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new QSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        // @formatter:off
        return "\\c 100 200\n" +
                "\\l file.q\n" +
                "3.5\n" +
                "1 2 2.4 0N 3 43 0i / numbers\n" +
                "\"c\"\n" +
                "\"string\"\n" +
                "`symbol\n" +
                "`a`b`c\n" +
                "variable\n" +
                "(+;*;<;>;~=)\n" +
                "count;\n" +
                "`int$()\n" +
                "a ,/:\\\\: b\n" +
                "(if[];?[];$[])\n" +
                "\\d .some.dir\n" +
                "{[param1;param2;param2] localV:10; :param1+param2+param3+localV+globalE; }\n";
        // @formatter:on
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public boolean isRainbowType(TextAttributesKey type) {
        return QSyntaxHighlighter.VARIABLE.equals(type);
    }

    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

}
