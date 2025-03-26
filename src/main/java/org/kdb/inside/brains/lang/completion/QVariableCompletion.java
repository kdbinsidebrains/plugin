package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.KdbType;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.QWord;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.IdentifierType;
import org.kdb.inside.brains.psi.index.QIndexService;
import org.kdb.inside.brains.view.inspector.InspectorToolWindow;
import org.kdb.inside.brains.view.inspector.model.ExecutableElement;
import org.kdb.inside.brains.view.inspector.model.InspectorElement;
import org.kdb.inside.brains.view.inspector.model.TableElement;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class QVariableCompletion extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        PsiElement el = parameters.getOriginalPosition();
        if (el == null) {
            el = parameters.getPosition();
        }

        final Project project = el.getProject();
        final PsiElement parent = el.getParent();
        if (!(parent instanceof QPsiElement)) {
            return;
        }

        final ElementContext ctx = ElementContext.of(el);
        if (ctx.is(ElementScope.QUERY)) {
            completeQuery(el, ctx.query(), project, result);
        }


        if (parent instanceof QVariable) {
            completeVariable((QVariable) parent, project, result);
        }
    }

    private void completeQuery(PsiElement el, QQueryExpr query, Project project, CompletionResultSet result) {
        final LeafPsiElement pos = PsiTreeUtil.getPrevSiblingOfType(el, LeafPsiElement.class);
        if (pos != null && "from".equals(pos.getText())) {
            addGlobal("", project, IdentifierType.TABLE, result);
            addLambda("", ElementContext.of(query), result);
            addInspector("", TableElement.class, project, result);
        } else {
            final QExpression source = query.getSource();
            if (source instanceof QVarReference) {
                final PsiReference reference = source.getReference();
                if (reference != null) {
                    final PsiElement resolve = reference.resolve();
                    if (resolve instanceof QVarDeclaration) {
                        addSourceTableColumns((QVarDeclaration) resolve, result);
                    } else {
                        addInspectorTableColumns(((QVarReference) source).getQualifiedName(), project, result);
                    }
                }
            }
        }
    }

    private void addInspectorTableColumns(String tableName, Project project, CompletionResultSet result) {
        final InspectorToolWindow inspector = InspectorToolWindow.getExist(project);
        if (inspector == null) {
            return;
        }

        final InspectorElement element = inspector.getElement(tableName);
        if (!(element instanceof TableElement table)) {
            return;
        }

        for (TableElement.Column column : table.getColumns()) {
            LookupElementBuilder b = LookupElementBuilder.create(column.getName()).withIcon(column.getIcon()).withTailText(" " + KdbType.typeOf(column.getType()).getTypeName()).withTypeText(tableName + "@inspector", true);
            result.addElement(b);
        }
    }

    private void addSourceTableColumns(QVarDeclaration tableName, CompletionResultSet result) {
        final PsiElement parent = tableName.getParent();
        if (!(parent instanceof QAssignmentExpr assignmentExpr)) {
            return;
        }

        final QExpression expression = assignmentExpr.getExpression();
        if (!(expression instanceof QTableExpr)) {
            return;
        }

        PsiTreeUtil.getChildrenOfTypeAsList(expression, QTableColumns.class).stream().flatMap(c -> c.getColumns().stream()).forEach(c -> {
            final QVarDeclaration declaration = c.getVarDeclaration();
            if (declaration == null) {
                return;
            }

            final String name = declaration.getQualifiedName();
            String type = "expression";
            final QExpression varExpr = c.getExpression();
            if (varExpr instanceof QTypeCastExpr) {
                type = QPsiUtil.getTypeCast((QTypeCastExpr) varExpr);
            } else if (varExpr instanceof QLiteralExpr) {
                type = "()".equals(varExpr.getText()) ? "vector" : "literal";
            }

            LookupElementBuilder b = LookupElementBuilder.create(name).withIcon(QIconProvider.getColumnIcon(c)).withTailText(" " + type).withTypeText(tableName.getQualifiedName() + "@" + tableName.getContainingFile().getName(), true);
            result.addElement(b);
        });
    }

    private void completeVariable(QVariable variable, Project project, CompletionResultSet result) {
        // No suggestion in parameters - it's free-form names
        if (variable.getParent() instanceof QParameters) {
            return;
        }

        // ignore empty values
        final String qualifiedName = variable.getQualifiedName();
        if (qualifiedName.isBlank()) {
            return;
        }

        addGlobal(qualifiedName, project, null, result);
        addLambda(qualifiedName, ElementContext.of(variable), result);
        addFunctions(qualifiedName, result);
        addKeywords(qualifiedName, result);
        addInspector(qualifiedName, ExecutableElement.class, project, result);
    }


    private void addInspector(String qualifiedName, Class<? extends ExecutableElement> type, Project project, CompletionResultSet result) {
        final InspectorToolWindow inspector = InspectorToolWindow.getExist(project);
        if (inspector == null) {
            return;
        }

        final String instance = "inspector";
        final List<ExecutableElement> suggestions = inspector.getSuggestions(qualifiedName, type);
        for (ExecutableElement element : suggestions) {
            result.addElement(LookupElementBuilder.create(element.getCanonicalName()).withIcon(element.getIcon(false)).withTailText(" " + element.getLocationString()).withTypeText(instance, true));
        }
    }

    private void addKeywords(String qualifiedName, CompletionResultSet result) {
        addEntities(QLanguage.getKeywords(), qualifiedName, KdbIcons.Node.Keyword, result);
    }

    private void addFunctions(String qualifiedName, CompletionResultSet result) {
        QLanguage.getSystemNamespaces().stream().filter(qualifiedName::startsWith).forEach(s -> addEntities(QLanguage.getSystemFunctions(s), qualifiedName, KdbIcons.Node.Function, result));
    }

    private void addEntities(Collection<QWord> entities, String qualifiedName, Icon icon, CompletionResultSet result) {
        for (QWord function : entities) {
            final String name = function.getName();
            if (name.startsWith(qualifiedName)) {
                LookupElementBuilder b = LookupElementBuilder.create(name).withIcon(icon).withTypeText(function.getDescription(), true);
                if (function.getArguments() != null) {
                    b = b.withTailText(function.getArguments());
                }
                result.addElement(b);
            }
        }
    }

    private void addGlobal(String qualifiedName, Project project, IdentifierType identifierType, CompletionResultSet result) {
        final QIndexService index = QIndexService.getInstance(project);
        index.processValues(s -> s.startsWith(qualifiedName), GlobalSearchScope.allScope(project), (key, file, descriptor) -> {
            // No symbols for suggestions in a variable
            if (descriptor.isSymbol()) {
                return true;
            }

            final IdentifierType type = descriptor.type();
            if (identifierType != null && identifierType != type) {
                return true;
            }

            LookupElementBuilder b = LookupElementBuilder.create(key).withIcon(type.getIcon()).withTypeText(file.getName(), true);

            final List<String> params = descriptor.params();
            if (type == IdentifierType.LAMBDA) {
                final String join = params == null ? "" : String.join(";", params);
                b = b.withTailText("[" + join + "]");
            }
            if (type == IdentifierType.DICT) {
                final String size = params == null ? "unknown" : String.valueOf(params.size());
                b = b.withTailText(" " + size + " fields");
            }
            if (type == IdentifierType.TABLE) {
                final String size = params == null ? "unknown" : String.valueOf(params.size());
                b = b.withTailText(" " + size + " columns");
            }
            result.addElement(b);
            return true;
        });
    }

    private void addLambda(String qualifiedName, ElementContext context, CompletionResultSet result) {
        final QLambdaExpr lambda = context.lambda();
        if (lambda == null) {
            return;
        }

        final Set<String> names = new HashSet<>();
        addLambdaParams(lambda, qualifiedName, names, result);
        addLambdaVariables(lambda, qualifiedName, names, result);
    }

    private void addLambdaParams(QLambdaExpr lambda, String qualifiedName, Set<String> names, CompletionResultSet result) {
        final QParameters parameters = lambda.getParameters();
        if (parameters == null) {
            return;
        }

        for (QVarDeclaration variable : parameters.getVariables()) {
            final String varName = variable.getQualifiedName();
            if (!varName.startsWith(qualifiedName) || !names.add(varName)) {
                continue;
            }

            final LookupElementBuilder b = LookupElementBuilder.create(variable).withIcon(IdentifierType.ARGUMENT.getIcon()).withTypeText("Function argument", true);
            result.addElement(b);
        }
    }

    private void addLambdaVariables(QLambdaExpr lambda, String qualifiedName, Set<String> names, CompletionResultSet result) {
        final Collection<QAssignmentExpr> childrenOfType = PsiTreeUtil.findChildrenOfType(lambda, QAssignmentExpr.class);
        for (QAssignmentExpr assignment : childrenOfType) {
            final QVarDeclaration variable = assignment.getVarDeclaration();
            if (variable == null) {
                continue;
            }

            final String varName = variable.getQualifiedName();
            if (!varName.startsWith(qualifiedName) || !names.add(varName)) {
                continue;
            }

            final IdentifierType type = IdentifierType.getType(assignment);
            if (type == null) {
                continue;
            }

            final LookupElementBuilder b = LookupElementBuilder.create(variable).withIcon(type.getIcon()).withTypeText("Local " + type.name().toLowerCase(), true);
            result.addElement(b);
        }
    }
}
