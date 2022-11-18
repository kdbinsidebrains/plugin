package org.kdb.inside.brains.ide.library;

import com.intellij.ide.JavaUiBundle;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.openapi.roots.libraries.ui.LibraryEditorComponent;
import com.intellij.openapi.roots.libraries.ui.LibraryPropertiesEditor;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.VirtualFile;
import icons.KdbIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class KdbLibraryType extends LibraryType<KdbLibraryProperties> {
    public static final PersistentLibraryKind<KdbLibraryProperties> KDB_KIND = new PersistentLibraryKind<>("KdbInsideBrains") {
        @Override
        public @NotNull KdbLibraryProperties createDefaultProperties() {
            return new KdbLibraryProperties();
        }
    };

    protected KdbLibraryType() {
        super(KDB_KIND);
    }

    public static KdbLibraryType getInstance() {
        return EP_NAME.findExtension(KdbLibraryType.class);
    }

    @Override
    public @Nullable String getCreateActionName() {
        return "KDB+ Q";
    }

    @Override
    public @Nullable Icon getIcon(@Nullable KdbLibraryProperties properties) {
        return KdbIcons.Main.Library;
    }

    @Override
    public String getDescription(@NotNull KdbLibraryProperties properties) {
        return "Q Code";
    }

    @Override
    public @Nullable NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory, @NotNull Project project) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, true, true, false, true, true);
        descriptor.setTitle(JavaUiBundle.message("new.library.file.chooser.title"));
        descriptor.setDescription("Choose files or directories where additional Q Source Code is located");
        final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parentComponent, project, contextDirectory);
        if (files.length == 0) {
            return null;
        }

        final VirtualFile file = files[0];

        return new NewLibraryConfiguration(file.getPresentableName(), this, new KdbLibraryProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                for (VirtualFile file : files) {
                    editor.addRoot(file, OrderRootType.SOURCES);
                }
            }
        };
    }

    @Override
    public KdbLibraryProperties detect(@NotNull List<VirtualFile> classesRoots) {
        return null;
    }

    @Override
    public OrderRootType @NotNull [] getExternalRootTypes() {
        return new OrderRootType[]{OrderRootType.SOURCES};
    }

    @Override
    public @Nullable LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<KdbLibraryProperties> editorComponent) {
        return null;
    }
}
