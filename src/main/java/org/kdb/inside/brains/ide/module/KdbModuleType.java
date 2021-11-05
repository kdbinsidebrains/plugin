package org.kdb.inside.brains.ide.module;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import icons.KdbIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.QLanguage;

import javax.swing.*;

public class KdbModuleType extends ModuleType<KdbModuleBuilder> {
    public static final String ID = "KDB_MODULE_TYPE";

    public KdbModuleType() {
        super(ID);
    }

    @Override
    public @NotNull KdbModuleBuilder createModuleBuilder() {
        return new KdbModuleBuilder();
    }

    @Override
    public @NotNull Icon getNodeIcon(boolean isOpened) {
        return KdbIcons.Main.Module;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getName() {
        return QLanguage.INSTANCE.getDisplayName();
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getDescription() {
        return "KDB+ Q Programming language";
    }

    public static boolean is(Module module) {
        return is(module, getModuleType());
    }

    public static KdbModuleType getModuleType() {
        return (KdbModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }
}
