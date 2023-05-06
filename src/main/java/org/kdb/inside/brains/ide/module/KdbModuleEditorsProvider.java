package org.kdb.inside.brains.ide.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.roots.ui.configuration.ClasspathEditor;
import com.intellij.openapi.roots.ui.configuration.CommonContentEntriesEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import org.jetbrains.jps.model.java.JavaSourceRootType;

public class KdbModuleEditorsProvider implements ModuleConfigurationEditorProvider {
    @Override
    public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state) {
        final var rootModel = state.getModifiableRootModel();
        final Module module = rootModel.getModule();
        if (!(ModuleType.get(module) instanceof KdbModuleType)) {
            return ModuleConfigurationEditor.EMPTY;
        }

        return new ModuleConfigurationEditor[]{
                new CommonContentEntriesEditor(module.getName(), state, JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE),
                new ClasspathEditor(state)
        };
    }
}
