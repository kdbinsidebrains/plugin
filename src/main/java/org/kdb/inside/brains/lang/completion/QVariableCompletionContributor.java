package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.QWord;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.IdentifierType;
import org.kdb.inside.brains.psi.index.QIndexService;

import javax.swing.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class QVariableCompletionContributor extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement el = parameters.getOriginalPosition();
        if (el == null || !(el.getParent() instanceof QVariable)) {
            return;
        }

        final QVariable variable = (QVariable) el.getParent();
        // No suggestion in parameters - it's free-form names
        if (variable.getParent() instanceof QParameters) {
            return;
        }

        final String qualifiedName = variable.getQualifiedName();

        addGlobal(qualifiedName, el.getProject(), result);
        addLocal(qualifiedName, ElementContext.of(variable), result);
        addFunctions(qualifiedName, result);
        addKeywords(qualifiedName, result);
    }

    private void addKeywords(String qualifiedName, CompletionResultSet result) {
        addEntities(QLanguage.getKeywords(), qualifiedName, KdbIcons.Node.Keyword, result);
    }

    private void addFunctions(String qualifiedName, CompletionResultSet result) {
        QLanguage.getSystemNamespaces()
                .stream()
                .filter(qualifiedName::startsWith)
                .forEach(s -> addEntities(QLanguage.getSystemFunctions(s), qualifiedName, KdbIcons.Node.Function, result));
    }

    private void addEntities(Collection<QWord> entities, String qualifiedName, Icon icon, CompletionResultSet result) {
        for (QWord function : entities) {
            final String name = function.getName();
            if (name.startsWith(qualifiedName)) {
                LookupElementBuilder b = LookupElementBuilder
                        .create(name)
                        .withIcon(icon)
                        .withTypeText(function.getDescription(), true);
                if (function.getArguments() != null) {
                    b = b.withTailText(function.getArguments());
                }
                result.addElement(b);
            }
        }
    }

    private void addGlobal(String qualifiedName, Project project, CompletionResultSet result) {
        final QIndexService index = QIndexService.getInstance(project);
        index.processValues(
                s -> s.startsWith(qualifiedName),
                GlobalSearchScope.allScope(project),
                (key, file, descriptor) -> {
                    final IdentifierType type = descriptor.getType();
                    LookupElementBuilder b = LookupElementBuilder
                            .create(key)
                            .withIcon(type.getIcon())
                            .withTypeText(file.getName(), true);

                    if (type == IdentifierType.LAMBDA && descriptor.getParams() != null) {
                        b = b.withTailText("[" + String.join(";", descriptor.getParams()) + "]");
                    }
                    result.addElement(b);
                }
        );
    }

    private void addLocal(String qualifiedName, ElementContext context, CompletionResultSet result) {
        if (context.getScope() != ElementScope.LAMBDA) {
            return;
        }

        final PsiElement element = context.getElement();
        final Collection<QAssignmentExpr> childrenOfType = PsiTreeUtil.findChildrenOfType(element, QAssignmentExpr.class);

        final Set<String> names = new HashSet<>();
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

            final LookupElementBuilder b = LookupElementBuilder
                    .create(variable)
                    .withIcon(type.getIcon())
                    .withTypeText("Local " + type.name().toLowerCase(), true);
            result.addElement(b);
        }
    }
}
