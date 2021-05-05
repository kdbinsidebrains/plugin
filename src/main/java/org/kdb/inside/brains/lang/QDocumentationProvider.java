package org.kdb.inside.brains.lang;

import com.intellij.lang.documentation.AbstractDocumentationProvider;

@Deprecated
public final class QDocumentationProvider extends AbstractDocumentationProvider {
/*

  @Nullable
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if (!isDeclaration(element)) {
      return null;
    }
    final QVariable userId = (QVariable)element;
    return getFunctionSignature(userId).orElse(null);
  }

  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (!isDeclaration(element)) {
      return null;
    }
    final QVariable userId = (QVariable)element;
    return getComments(userId).map(comments -> String.join("\n<br>", comments, getFunctionSignature(userId).orElse("")))
        .orElse(null);
  }

  @Nullable
  private boolean isDeclaration(PsiElement element) {
    if (!(element instanceof QVariable)) {
      return false;
    }
    final QVariable userId = (QVariable)element;
    return userId.isDeclaration();
  }

  @NotNull
  static Optional<String> getFunctionSignature(QVariable userId) {
    return Optional.of(userId)
        .map(QVariable::getParent)
        .filter(QAssignment.class::isInstance)
        .map(QAssignment.class::cast)
        .map(QAssignment::getExpression)
        .map(QExpression::getLambdaList)
        .filter(l -> !l.isEmpty())
        .map(l -> {
          final QParameters lambdaParams = l.get(0).getParameters();
          return lambdaParams == null ? Collections.<QVariable>emptyList() : lambdaParams.getVariableList();
        })
        .map(Collection::stream)
        .map(s -> {
          final List<String> paramNames = s.map(QVariable::getName).collect(Collectors.toList());
          return String.format("%s[%s] - %s", userId.getQualifiedName(), String.join(";", paramNames),
              userId.getContainingFile().getName());
        });
  }

  @Nullable
  private Optional<String> getComments(QVariable userId) {
    final Collection<String> comments = getFunctionDocs(userId);
    if (comments.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(String.join("\n<br>", comments));
  }

  @NotNull
  static Collection<String> getFunctionDocs(QVariable userId) {
    final QExpression fnDeclaration = userId.getContext(QExpression.class);
    final Deque<String> comments = new ArrayDeque<>();
    PsiElement curr = fnDeclaration.getPrevSibling();
    while (curr instanceof PsiComment) {
      final PsiComment comment = (PsiComment)curr;
      comments.push(comment.getText());
      curr = curr.getPrevSibling();
    }
    return comments;
  }
*/

}
