package org.kdb.inside.brains.lang.docs;

import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QAssignmentExpr;
import org.kdb.inside.brains.psi.QExpression;
import org.kdb.inside.brains.psi.QLambdaExpr;
import org.kdb.inside.brains.psi.QVarDeclaration;

import java.util.ArrayList;
import java.util.List;

public record QVariableDoc(@NotNull String definition, @Nullable String description, List<Parameter> parameters) {
    public static QVariableDoc withParsedComment(QAssignmentExpr assign) {
        if (assign == null) {
            return null;
        }

        final String definition = parseDefinition(assign);
        if (definition == null) {
            return null;
        }

        final String comment = collectComment(assign);
        if (StringUtil.isEmpty(comment)) {
            return new QVariableDoc(definition, null, List.of());
        }
        return withParsedComment(definition, comment);
    }

    static QVariableDoc withParsedComment(@NotNull String definition, @NotNull String comment) {
        StringBuilder description = null;

        final List<Parameter> parameters = new ArrayList<>();
        final List<String> sentences = parseCommentary(comment);
        for (String sentence : sentences) {
            if (!sentence.startsWith("@")) {
                if (description == null) {
                    description = new StringBuilder(sentence);
                } else {
                    description.append(" ").append(sentence);
                }
            } else {
                final int i = sentence.indexOf(' ');
                if (i < 0) {
                    parameters.add(new Parameter(sentence.substring(1), ""));
                } else {
                    parameters.add(new Parameter(sentence.substring(1, i), sentence.substring(i + 1)));
                }
            }
        }
        return new QVariableDoc(definition, description == null ? null : description.toString(), parameters);
    }

    private static String parseDefinition(QAssignmentExpr assignment) {
        final QVarDeclaration variable = assignment.getVarDeclaration();
        if (variable == null) {
            return null;
        }

        final QExpression expression = assignment.getExpression();
        if (!(expression instanceof QLambdaExpr lambda)) {
            return null;
        }
        return variable.getQualifiedName() + lambda.getParametersInfo();
    }

    private static String collectComment(QAssignmentExpr element) {
        PsiElement e = element.getPrevSibling();
        // comment must have no spaces to be a description
        if (e instanceof PsiWhiteSpace s && !singleNewLine(s)) {
            return null;
        }

        final StringBuilder b = new StringBuilder();
        do {
            if (e instanceof PsiComment) {
                b.insert(0, e.getText());
            } else if (e instanceof PsiWhiteSpace s && singleNewLine(s)) {
                b.insert(0, e.getText());
            } else {
                break;
            }
            e = e.getPrevSibling();
        } while (true);
        return b.toString();
    }

    private static boolean singleNewLine(PsiWhiteSpace space) {
        return "\n".equals(space.getText());
    }

    private static List<String> parseCommentary(String docString) {
        StringBuilder line = new StringBuilder();
        final List<String> res = new ArrayList<>();

        final String[] split = docString.split("\\r?\\n");
        for (String s : split) {
            s = s.strip();
            if (s.isEmpty()) {
                continue;
            }

            while (!s.isEmpty()) {
                final char c = s.charAt(0);
                if (c != '/' && c != '\\') {
                    break;
                }
                s = s.substring(1);
            }

            s = s.strip();
            if (s.isEmpty()) {
                continue;
            }

            if (s.charAt(0) == '@') {
                res.add(line.toString().strip());
                line = new StringBuilder(s);
            } else {
                line.append(" ").append(s);
            }
        }
        res.add(line.toString().strip());
        return res;
    }

    private static void appendSection(HtmlBuilder builder, String sectionName, String sectionContent) {
        HtmlChunk headerCell = DocumentationMarkup.SECTION_HEADER_CELL.child(HtmlChunk.text(sectionName).wrapWith("p"));
        HtmlChunk contentCell = DocumentationMarkup.SECTION_CONTENT_CELL.addRaw(sectionContent);
        builder.append(HtmlChunk.tag("tr").children(headerCell, contentCell));
    }

    public String toHtml() {
        final HtmlBuilder builder = new HtmlBuilder();

        if (StringUtil.isNotEmpty(definition)) {
            final HtmlBuilder definitionBuilder = new HtmlBuilder();
            definitionBuilder.append(HtmlChunk.text(definition).bold());
            builder.append(definitionBuilder.wrapWith(DocumentationMarkup.DEFINITION_ELEMENT));
        }

        if (StringUtil.isNotEmpty(description)) {
            final HtmlBuilder contentBuilder = new HtmlBuilder();
            contentBuilder.appendRaw(description);
            contentBuilder.append(HtmlChunk.br()).append(HtmlChunk.br());
            builder.append(contentBuilder.wrapWith(DocumentationMarkup.CONTENT_ELEMENT));
        }

        if (!parameters.isEmpty()) {
            final HtmlBuilder sectionsBuilder = new HtmlBuilder();
            for (Parameter parameter : parameters) {
                appendSection(sectionsBuilder, parameter.name(), parameter.description());
            }
            builder.append(sectionsBuilder.wrapWith(DocumentationMarkup.SECTIONS_TABLE));
        }
        return builder.toString();
    }

    public record Parameter(String name, String description) {
    }
}
