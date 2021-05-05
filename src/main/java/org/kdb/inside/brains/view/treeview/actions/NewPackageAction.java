package org.kdb.inside.brains.view.treeview.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.UIUtils;
import org.kdb.inside.brains.action.ActionPlaces;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.KdbScope;
import org.kdb.inside.brains.core.PackageItem;
import org.kdb.inside.brains.core.StructuralItem;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

import java.util.Set;
import java.util.stream.Collectors;

public class NewPackageAction extends SingleItemAction {
    @Override
    public void update(@NotNull AnActionEvent e, InstanceItem item) {
        final Presentation presentation = e.getPresentation();
        final boolean enabled = item instanceof KdbScope || item instanceof PackageItem;

        if (ActionPlaces.isPopupPlace(e.getPlace())) {
            presentation.setEnabledAndVisible(enabled);
        } else {
            presentation.setEnabled(enabled);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, InstanceItem item) {
        if (!(item instanceof StructuralItem)) {
            return;
        }
        createPackage(e, (StructuralItem) item);
    }

    public static void modifyPackage(@NotNull AnActionEvent e, PackageItem item) {
        modifyPackage(e, item, true);
    }

    public static void createPackage(@NotNull AnActionEvent e, StructuralItem item) {
        modifyPackage(e, item, false);
    }

    private static void modifyPackage(@NotNull AnActionEvent e, StructuralItem item, boolean modify) {
        final DataContext dataContext = e.getDataContext();
        final Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            return;
        }

        final StructuralItem folder = modify ? item.getParent() : item;
        final InstancesScopeView instancesScopeView = getInstancesScopeView(e);

        final String currentName = modify ? item.getName() : "";

        final Set<String> collect = folder.getChildren().stream().map(InstanceItem::getName).collect(Collectors.toSet());

        UIUtils.createNameDialog(project, "New Package", currentName, dataContext,
                name -> !collect.contains(name),
                name -> {
                    if (modify) {
                        item.setName(name);
                    } else {
                        instancesScopeView.selectItem(folder.createPackage(name));
                    }
                });
    }
}