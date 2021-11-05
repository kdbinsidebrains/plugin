package org.kdb.inside.brains.psi.refs;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.*;
import org.kdb.inside.brains.psi.index.QIndexService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QVariableReferenceProvider extends QReferenceProvider<QVariable> {
    protected QVariableReferenceProvider() {
        super(QVariable.class);
    }

    @Override
    protected PsiReference @NotNull [] getElementReferences(@NotNull QVariable var, @NotNull ProcessingContext context) {
        if (var instanceof QVarReference) {
            return createReference(var);
        }

        final ElementScope scope = var.getVariableContext().getScope();
        if (scope == ElementScope.PARAMETERS || scope == ElementScope.TABLE || scope == ElementScope.QUERY) {
            return PsiReference.EMPTY_ARRAY;
        }
        return createReference(var);
    }

    @NotNull
    private PsiReference[] createReference(QVariable var) {
        return new PsiReference[]{new QVariableReference(var)};
    }

    public static class QVariableReference extends PsiPolyVariantReferenceBase<QVariable> {
        public QVariableReference(@NotNull QVariable element) {
            super(element);
        }

        @Override
        public boolean isReferenceTo(@NotNull PsiElement element) {
            if (!element.isValid() || !(element instanceof QVariable)) {
                return false;
            }

            final ResolveResult[] resolveResults = multiResolve(false);
            for (ResolveResult resolveResult : resolveResults) {
                if (!resolveResult.isValidResult()) {
                    continue;
                }
                final PsiElement el = resolveResult.getElement();
                if (el == null) {
                    continue;
                }

                if (element.equals(el)) {
                    return true;
                }
            }

            final PsiReference myRef = myElement.getReference();
            final PsiReference otherRef = element.getReference();
            if (myRef != null && otherRef != null) {
                final PsiElement myR = myRef.resolve();
                final PsiElement otR = otherRef.resolve();
                if (myElement == otR) {
                    return true;
                }
                return otR != null && myR != null && Objects.equals(otR, myR);
            }
            return false;
        }

        @Override
        public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
            return resolveVariable(myElement, QPsiUtil.getElementContext(myElement));
        }

        private ResolveResult[] resolveVariable(QVariable variable, ElementContext context) {
            switch (context.getScope()) {
                case QUERY:
                    return resolveQuery(variable, context.query());
                case LAMBDA:
                    return resolveLambda(variable, context.lambda());
                default:
                    return resolveGlobal(variable);
            }
        }

        private ResolveResult[] resolveQuery(QVariable var, QQueryExpr query) {
            final QExpression expression = query.getExpression();
            if (expression == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final List<QVarReference> refs = new ArrayList<>();
            PsiElement child = expression.getFirstChild();
            while (child != null) {
                if (child instanceof QVarReference) {
                    refs.add((QVarReference) child);
                }
                if (child instanceof LeafPsiElement && "where".equals(child.getText())) {
                    break;
                }
                child = child.getNextSibling();
            }

            final ElementContext queryContext = QPsiUtil.getElementContext(query);

            // It's the table definition
            if (refs.contains(var)) {
                return resolveVariable(var, queryContext);
            }

            final List<QTableExpr> tables = refs.stream()
                    .map(r -> resolveVariable(r, queryContext))
                    .flatMap(Stream::of)
                    .map(ResolveResult::getElement)
                    .filter(Objects::nonNull)
                    .map(PsiElement::getParent)
                    .filter(e -> e instanceof QAssignmentExpr)
                    .map(e -> ((QAssignmentExpr) e).getExpression())
                    .filter(e -> e instanceof QTableExpr)
                    .map(e -> (QTableExpr) e)
                    .collect(Collectors.toList());

            final String qualifiedName = var.getQualifiedName();
            List<QVarDeclaration> res = new ArrayList<>();
            for (QTableExpr table : tables) {
                Stream.of(table.getKeys(), table.getValues())
                        .filter(Objects::nonNull)
                        .flatMap(v -> v.getColumns().stream())
                        .map(QTableColumn::getVarDeclaration)
                        .filter(Objects::nonNull)
                        .filter(v -> v.getQualifiedName().equals(qualifiedName))
                        .forEach(res::add);
            }

            if (res.isEmpty()) {
                return resolveVariable(var, queryContext);
            }
            return multi(res);
        }

        private ResolveResult[] resolveLambda(QVariable var, QLambdaExpr lambda) {
            if (QPsiUtil.isImplicitVariable(var) && lambda.getParameters() == null) {
                return single(lambda);
            }

            final String qualifiedName = var.getQualifiedName();
            final Optional<QVarDeclaration> first = firstFirstInLambda(qualifiedName, lambda);
            if (first.isPresent()) {
                final QVarDeclaration el = first.get();
                if (QPsiUtil.isGlobalDeclaration(el)) {
                    return resolveGlobal(var);
                } else {
                    return single(el);
                }
            }
            return resolveGlobal(var);
        }

        private ResolveResult[] resolveGlobal(QVariable var) {
            final PsiFile file = var.getContainingFile();
            if (file == null) {
                return ResolveResult.EMPTY_ARRAY;
            }

            final String qualifiedName = var.getQualifiedName();
            final QIndexService index = QIndexService.getInstance(myElement);
            final QVarDeclaration initial = index.findFirstInFile(qualifiedName, file);
            if (initial == null) {
                return multi(findGlobalVariables(qualifiedName, GlobalSearchScope.allScope(var.getProject())));
            }
            return single(initial);
        }

        private List<QVarDeclaration> findGlobalVariables(String qualifiedName, @NotNull GlobalSearchScope scope) {
            final List<QVarDeclaration> elements = new ArrayList<>();
            final QIndexService index = QIndexService.getInstance(myElement);
            index.processVariables(s -> s.equals(qualifiedName), scope, (key, file, descriptor, variable) -> {
                if (QPsiUtil.isGlobalDeclaration(variable)) {
                    elements.add(variable);
                }
            });
            return elements;
        }

        @NotNull
        private ResolveResult[] single(QPsiElement el) {
            return new ResolveResult[]{new PsiElementResolveResult(el)};
        }

        private ResolveResult[] multi(List<QVarDeclaration> allGlobal) {
            return allGlobal.stream().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
        }

        @NotNull
        private Optional<QVarDeclaration> firstFirstInLambda(String qualifiedName, QLambdaExpr lambda) {
            return PsiTreeUtil.findChildrenOfType(lambda, QVarDeclaration.class).stream().
                    filter(v -> v.getQualifiedName().equals(qualifiedName)).
                    filter(v -> ElementContext.of(v).any(ElementScope.LAMBDA, ElementScope.PARAMETERS)).
                    findFirst();
        }
    }
}