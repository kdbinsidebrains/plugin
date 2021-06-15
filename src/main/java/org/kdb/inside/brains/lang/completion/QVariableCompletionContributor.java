package org.kdb.inside.brains.lang.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QKeyword;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.psi.QVariable;
import org.kdb.inside.brains.psi.index.IdentifierType;
import org.kdb.inside.brains.psi.index.QIndexService;

import javax.swing.*;
import java.util.List;

public class QVariableCompletionContributor extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
        final PsiElement el = parameters.getOriginalPosition();
        if (el == null || !(el.getParent() instanceof QVariable)) {
            return;
        }

        final QVariable variable = (QVariable) el.getParent();
        final String qualifiedName = variable.getQualifiedName();

        addIndexes(qualifiedName, el.getProject(), result);
        addSystemFunction(qualifiedName, result);
        addKeywords(qualifiedName, result);
    }

    private void addKeywords(String qualifiedName, CompletionResultSet result) {
        addEntities(QLanguage.getKeywords(), qualifiedName, KdbIcons.Node.Keyword, result);
    }

    private void addSystemFunction(String qualifiedName, CompletionResultSet result) {
        QLanguage.getSystemNamespaces()
                .stream()
                .filter(qualifiedName::startsWith)
                .forEach(s -> addEntities(QLanguage.getSystemFunctions(s), qualifiedName, KdbIcons.Node.Function, result));
    }

    private void addEntities(List<QKeyword> entities, String qualifiedName, Icon icon, CompletionResultSet result) {
        for (QKeyword function : entities) {
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

    private void addIndexes(String qualifiedName, Project project, CompletionResultSet result) {
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
}
