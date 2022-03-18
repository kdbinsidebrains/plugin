package org.kdb.inside.brains.lang.docs;

import com.intellij.lang.documentation.DocumentationMarkup;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.kdb.inside.brains.psi.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QVariableDoc {
    private final String definition;
    private final String description;
    private final List<Parameter> parameters;

    public QVariableDoc(String definition, String description, List<Parameter> parameters) {
        this.definition = definition;
        this.description = description;
        this.parameters = parameters;
    }

    public String getDefinition() {
        return definition;
    }

    public String getDescription() {
        return description;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public String toHtml() {
        if (definition == null && description == null && (parameters == null || parameters.isEmpty())) {
            return null;
        }

        final HtmlBuilder builder = new HtmlBuilder();

        if (definition != null && !definition.isBlank()) {
            final HtmlBuilder definitionBuilder = new HtmlBuilder();
            definitionBuilder.append(HtmlChunk.text(definition).bold());
            builder.append(definitionBuilder.wrapWith(DocumentationMarkup.DEFINITION_ELEMENT));
        }

        if (description != null && !description.isBlank()) {
            final HtmlBuilder contentBuilder = new HtmlBuilder();
            contentBuilder.append(HtmlChunk.text(description));
            contentBuilder.append(HtmlChunk.br()).append(HtmlChunk.br());
            builder.append(contentBuilder.wrapWith(DocumentationMarkup.CONTENT_ELEMENT));
        }

        final HtmlBuilder sectionsBuilder = new HtmlBuilder();
        for (Parameter parameter : parameters) {
            appendSection(sectionsBuilder, parameter.getName(), parameter.getDescription());
        }
        builder.append(sectionsBuilder.wrapWith(DocumentationMarkup.SECTIONS_TABLE));
        return builder.toString();
    }

    private static void appendSection(HtmlBuilder builder, String sectionName, String sectionContent) {
        HtmlChunk headerCell = DocumentationMarkup.SECTION_HEADER_CELL.child(HtmlChunk.text(sectionName).wrapWith("p"));
        HtmlChunk contentCell = DocumentationMarkup.SECTION_CONTENT_CELL.addText(sectionContent);
        builder.append(HtmlChunk.tag("tr").children(headerCell, contentCell));
    }

    public static QVariableDoc from(QAssignmentExpr variable) {
        return from(getDefinition(variable), extractDocs(variable));
    }

    static String getDefinition(QAssignmentExpr assignment) {
        final QVarDeclaration variable = assignment.getVarDeclaration();
        if (variable == null) {
            return null;
        }

        final QExpression expression = assignment.getExpression();
        if (!(expression instanceof QLambdaExpr)) {
            return null;
        }

        final QLambdaExpr lambda = (QLambdaExpr) expression;
        final QParameters parameters = lambda.getParameters();
        if (parameters == null) {
            return variable.getQualifiedName() + "[]";
        }
        final String params = parameters.getVariables().stream().map(QVariable::getName).collect(Collectors.joining(";"));
        return variable.getQualifiedName() + "[" + params + "]";
    }

    public static QVariableDoc from(String definition, String documentations) {
        if ((definition == null || definition.isBlank()) && (documentations == null || documentations.isBlank())) {
            return null;
        }

        String mainText = null;
        List<Parameter> parameters = new ArrayList<>();

        final List<String> strings = splitDocs(documentations);
        for (String string : strings) {
            if (!string.startsWith("@")) {
                if (mainText == null) {
                    mainText = string;
                } else {
                    mainText += " " + string;
                }
            } else {
                final int i = string.indexOf(' ');
                if (i < 0) {
                    parameters.add(new Parameter(string.substring(1), ""));
                } else {
                    parameters.add(new Parameter(string.substring(1, i), string.substring(i + 1)));
                }
            }
        }
        return new QVariableDoc(definition, mainText, parameters);
    }

    static List<String> splitDocs(String docString) {
        List<String> res = new ArrayList<>();

        String line = "";

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
                res.add(line.strip());
                line = s;
            } else {
                line += " " + s;
            }
        }
        res.add(line.strip());
        return res;
    }

    static String extractDocs(QAssignmentExpr element) {
        StringBuilder b = new StringBuilder();
        PsiElement e = element.getPrevSibling();
        while (e instanceof PsiComment || e instanceof PsiWhiteSpace) {
            if (e instanceof PsiComment) {
                b.insert(0, e.getText());
            } else {
                b.insert(0, System.lineSeparator());
            }
            e = e.getPrevSibling();
        }
        return b.toString();
    }

    public static final class Parameter {
        private final String name;
        private final String description;

        public Parameter(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
