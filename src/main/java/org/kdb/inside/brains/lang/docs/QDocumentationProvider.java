package org.kdb.inside.brains.lang.docs;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.QLanguage;
import org.kdb.inside.brains.QWord;
import org.kdb.inside.brains.psi.QTypes;
import org.kdb.inside.brains.psi.QVariable;
import org.kdb.inside.brains.psi.QVariableAssignment;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static com.intellij.lang.documentation.DocumentationMarkup.*;

public final class QDocumentationProvider extends AbstractDocumentationProvider {
    private static final Set<IElementType> ALLOWED_ELEMENTS = Set.of(
            QTypes.UNARY_FUNCTION,
            QTypes.BINARY_FUNCTION,
            QTypes.COMPLEX_FUNCTION,
            QTypes.COMMAND_SYSTEM,
            QTypes.COMMAND_IMPORT,
            QTypes.COMMAND_CONTEXT,
            QTypes.VAR_REFERENCE,
            QTypes.VAR_DECLARATION
    );

    private static final String SYSTEM_DOC_STYLE = ".language-txt, .language-q { color: #657b83; }";

    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        final QWord word = getWord(element);
        if (word != null) {
            return createSystemDoc(word);
        }

        if (element instanceof QVariable) {
            return createVariableDoc((QVariable) element);
        }

        return null;
    }

    private String createVariableDoc(QVariable variable) {
        final PsiReference[] references = variable.getReferences();
        if (references.length != 1) {
            return null;
        }

        final PsiElement resolve = references[0].resolve();
        if (resolve == null) {
            return null;
        }

        final QVariableAssignment context = PsiTreeUtil.getContextOfType(resolve, QVariableAssignment.class);
        if (context == null) {
            return null;
        }

        final QVariableDoc doc = QVariableDoc.from(context);
        if (doc == null) {
            return context.getText();
        }
        return doc.toHtml();
    }

    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        final QWord word = getWord(element);
        if (word != null) {
            if (word.getDocsUrl() != null) {
                return List.of("https://code.kx.com/q/" + word.getDocsUrl());
            }
        }
        return null;
    }

    @Override
    public @Nullable PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement, int targetOffset) {
        if (contextElement == null) {
            return null;
        }

        if (ALLOWED_ELEMENTS.contains(contextElement.getNode().getElementType())) {
            return contextElement;
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset);
    }

    private QWord getWord(PsiElement element) {
        return QLanguage.getWord(element.getText());
    }

    private String getFilename(QWord word) {
        final String name = word.getName();

        final StringBuilder b = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            if (c == '\\' || c == '.') {
                b.append('_');
            } else if (Character.isUpperCase(c)) {
                b.append('_').append(Character.toLowerCase(c));
            } else {
                b.append(c);
            }
        }
        final String s = b.toString();
        return s + ".html";
    }

    private String createSystemDoc(QWord s) {
        return SYSTEM_DOC_STYLE + DEFINITION_START + s.getDescription() + DEFINITION_END + CONTENT_START + loadWordDescription(s) + CONTENT_END;
    }

    private String loadWordDescription(QWord word) {
        try {
            final URL resource = getClass().getResource("/org/kdb/inside/brains/words/docs/" + word.getType().getCode() + "/" + getFilename(word));
            if (resource == null) {
                return "No internal documentation found. Try to check <a href=\"https://code.kx.com\">code.kx.com</a> website.";
            }

            return IOUtils.toString(resource, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }
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
