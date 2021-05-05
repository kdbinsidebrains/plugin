package org.kdb.inside.brains.view.struct;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Filter;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.psi.QVariable;

final class QStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
    @SuppressWarnings("WeakerAccess")
    public QStructureViewModel(PsiFile psiFile) {
        super(psiFile, new QStructureViewElement(psiFile));
//        KSettingsService.getInstance().addListener(settingsListener);
    }

    @Override
    @NotNull
    public Sorter[] getSorters() {
        return new Sorter[]{Sorter.ALPHA_SORTER};
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return element.getValue() instanceof QVariable;
    }

    @NotNull
    @Override
    public Filter[] getFilters() {
        return new Filter[0];
    }

    @Override
    public void dispose() {
//        KSettingsService.getInstance().removeListener(settingsListener);
        super.dispose();
    }

//    private class TheSettingsListener implements KSettingsService.Listener {
//        @Override
//        public void settingsChanged(KSettings oldSettings, KSettings newSettings) {
//            fireModelUpdate();
//        }
//    }
}
