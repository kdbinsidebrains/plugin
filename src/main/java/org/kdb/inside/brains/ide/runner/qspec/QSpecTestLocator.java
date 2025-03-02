package org.kdb.inside.brains.ide.runner.qspec;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.lang.qspec.TestDescriptor;
import org.kdb.inside.brains.lang.qspec.TestItem;
import org.kdb.inside.brains.psi.QFile;
import org.kdb.inside.brains.psi.QVarReference;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

@SuppressWarnings("RawUseOfParameterized")
public class QSpecTestLocator implements SMTestLocator {
    private static final String PROTOCOL_SCRIPT = "qspec:script";
    private static final String PROTOCOL_DESC = "qspec:suite";
    private static final String PROTOCOL_SHOULD = "qspec:test";

    private static final List<Location> NO_LOCATION = List.of();

    public static final QSpecTestLocator INSTANCE = new QSpecTestLocator();

    private QSpecTestLocator() {
    }

    @Override
    public @NotNull List<Location> getLocation(@NotNull String protocol, @NotNull String path, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return switch (protocol) {
            case PROTOCOL_SCRIPT -> getLocation(project, path, this::findScript);
            case PROTOCOL_DESC -> getLocation(project, path, this::findDescVar);
            case PROTOCOL_SHOULD -> getLocation(project, path, this::findShouldVar);
            default -> NO_LOCATION;
        };
    }

    private @NotNull List<Location> getLocation(@NotNull Project project, @NotNull String path, BiFunction<Project, String, PsiElement> finder) {
        final PsiElement element = finder.apply(project, path);
        if (element == null) {
            return NO_LOCATION;
        }
        if (element instanceof QVarReference ref) {
            //  Not sure about this one. Doesn't like. It's better to select the keyword.
            //  final QExpression ex = ((QArguments) ref.getParent().getNextSibling()).getExpressions().get(0);
            //  return List.of(PsiLocation.fromPsiElement(ex));
            return List.of(PsiLocation.fromPsiElement(ref.getFirstChild()));
        }
        return List.of(PsiLocation.fromPsiElement(element));
    }

    private @Nullable PsiFileSystemItem findScript(@NotNull Project project, @NotNull String path) {
        final VirtualFile file = VirtualFileManager.getInstance().findFileByNioPath(Path.of(path));
        if (file == null) {
            return null;
        }
        final PsiManager instance = PsiManager.getInstance(project);
        return file.isDirectory() ? instance.findDirectory(file) : instance.findFile(file);
    }

    private @Nullable QVarReference findDescVar(@NotNull Project project, @NotNull String path) {
        final TestItem descItem = findDescItem(project, path);
        return descItem != null ? descItem.getNameElement() : null;
    }

    private @Nullable QVarReference findShouldVar(@NotNull Project project, @NotNull String path) {
        final int i = path.indexOf("]/[");
        if (i < 0) {
            return null;
        }

        final TestItem descItem = findDescItem(project, path.substring(0, i + 1));
        if (descItem == null) {
            return null;
        }

        final String shouldName = path.substring(i + 3, path.length() - 1);
        final TestItem item = TestDescriptor.findShouldItem(descItem, shouldName);
        return item == null ? null : item.getNameElement();
    }

    private @Nullable TestItem findDescItem(@NotNull Project project, @NotNull String path) {
        final int i = path.indexOf('?');
        if (i < 0) {
            return null;
        }

        final PsiFileSystemItem fileItem = findScript(project, path.substring(0, i));
        if (!(fileItem instanceof QFile file)) {
            return null;
        }

        final String descName = path.substring(i + 2, path.length() - 1);
        return TestDescriptor.findDescItem(file, descName);
    }
}