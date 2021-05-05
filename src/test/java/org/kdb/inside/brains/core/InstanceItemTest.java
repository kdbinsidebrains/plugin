package org.kdb.inside.brains.core;

import org.junit.jupiter.api.Test;

import javax.swing.tree.TreePath;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class InstanceItemTest {
    @Test
    public void treePath() {
        KdbInstance i = new KdbInstance("i1", "local", 34534, "asdas", null);
        assertEquals(new TreePath(new Object[]{i}), i.getTreePath());

        PackageItem p = new PackageItem("p1");
        assertEquals(new TreePath(new Object[]{p}), p.getTreePath());

        i = p.createInstance("i1", "local", 12412, "asdasd", null);
        assertEquals(new TreePath(new Object[]{p, i}), i.getTreePath());

        KdbScope s = new KdbScope("s1", ScopeType.LOCAL);
        assertEquals(new TreePath(new Object[]{s}), s.getTreePath());

        i = s.createInstance("i1", "local", 12412, "asdasd", null);
        assertEquals(new TreePath(new Object[]{s, i}), i.getTreePath());

        p = s.createPackage("p");
        assertEquals(new TreePath(new Object[]{s, i}), i.getTreePath());

        i = p.createInstance("i1", "local", 12412, "asdasd", null);
        assertEquals(new TreePath(new Object[]{s, p, i}), i.getTreePath());
    }
}