package org.kdb.inside.brains.core;

public final class PackageItem extends StructuralItem {
    PackageItem(String name) {
        super(name);
    }

    @Override
    public PackageItem copy() {
        final PackageItem s = new PackageItem(getName());
        for (InstanceItem child : children) {
            s.copyItem(child);
        }
        return s;
    }
}