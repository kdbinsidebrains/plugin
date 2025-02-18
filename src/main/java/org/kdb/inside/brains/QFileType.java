package org.kdb.inside.brains;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kdb.inside.brains.psi.QFile;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;

public final class QFileType extends LanguageFileType {
    public static final QFileType INSTANCE = new QFileType();

    private QFileType() {
        super(QLanguage.INSTANCE);
    }

    @Override
    public Icon getIcon() {
        return KdbIcons.Main.File;
    }

    @Override
    public @NotNull String getName() {
        return "KDB+ Q";
    }

    @Override
    public @NotNull String getDescription() {
        return "Q language file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "q";
    }

    public static boolean hasExtension(Path file) {
        return hasExtension(file.getFileName().toString());
    }

    public static boolean hasExtension(File file) {
        return hasExtension(file.getName());
    }

    public static boolean hasExtension(String file) {
        return FileUtilRt.extensionEquals(file, "q") || FileUtilRt.extensionEquals(file, "k");
    }

    /**
     * Creates fake file for factory usage.
     *
     * @param project the project
     * @param text    the text
     * @return the file
     */
    public static QFile createFactoryFile(Project project, String text) {
        return (QFile) PsiFileFactory.getInstance(project).createFileFromText("QElementFactory.q", QFileType.INSTANCE, text);
    }

    public static boolean is(@Nullable Path file) {
        return file != null && hasExtension(file.toFile());
    }

    public static boolean is(@Nullable PsiFile file) {
        return (file instanceof QFile) || (file != null && is(file.getVirtualFile()));
    }

    public static boolean is(@Nullable VirtualFile file) {
        return file != null && (INSTANCE == file.getFileType() || hasExtension(file.getName()));
    }
}
