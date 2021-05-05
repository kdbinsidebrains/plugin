package org.kdb.inside.brains.view.treeview.tree;

import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;

public class TransferableItems implements Transferable {
    private final ArrayList<InstanceItem> instanceItems;

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=" + ArrayList.class.getName(), "Kdb Instance Items");

    public TransferableItems(List<InstanceItem> instanceItems) {
        this.instanceItems = new ArrayList<>(instanceItems);
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DATA_FLAVOR};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DATA_FLAVOR);
    }

    @NotNull
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return instanceItems;
    }
}