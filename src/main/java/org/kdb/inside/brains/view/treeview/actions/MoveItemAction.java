package org.kdb.inside.brains.view.treeview.actions;

import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;
import org.kdb.inside.brains.core.InstanceItem;
import org.kdb.inside.brains.core.StructuralItem;
import org.kdb.inside.brains.view.treeview.InstancesScopeView;

import java.util.ArrayList;
import java.util.List;

public abstract class MoveItemAction extends AbstractInstancesAction {
    private final Direction direction;

    public MoveItemAction(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final Presentation presentation = e.getPresentation();
        presentation.setEnabled(view.getSelectedItems().stream().allMatch(i -> getMovePosition(i) != null));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull InstancesScopeView view) {
        final List<InstanceItem> selectedItems = new ArrayList<>(view.getSelectedItems());

        final List<InstanceItem> items = direction == Direction.UP ? selectedItems : Lists.reverse(selectedItems);
        for (InstanceItem item : items) {
            final MovePosition movePosition = getMovePosition(item);
            if (movePosition != null) {
                movePosition.parent.moveItem(item, movePosition.index);
            }
        }
        view.selectItems(selectedItems);
    }

    private MovePosition getMovePosition(InstanceItem item) {
        final StructuralItem parent = item.getParent();
        if (parent == null) {
            return null;
        }

        final int index = parent.childIndex(item);
        if (direction == Direction.UP) {
            if (index != 0) {
                final InstanceItem child = parent.getChild(index - 1);
                if (child instanceof StructuralItem) {
                    return new MovePosition((StructuralItem) child, ((StructuralItem) child).getChildrenCount());
                }
                return new MovePosition(parent, index - 1);
            }
            final StructuralItem p = parent.getParent();
            if (p != null) {
                return new MovePosition(p, p.childIndex(parent));
            }
            return null;
        } else if (direction == Direction.DOWN) {
            if (index < parent.getChildrenCount() - 1) {
                final InstanceItem child = parent.getChild(index + 1);
                if (child instanceof StructuralItem) {
                    return new MovePosition((StructuralItem) child, 0);
                }
                return new MovePosition(parent, index + 2); // insert after next one
            }
            final StructuralItem p = parent.getParent();
            if (p != null) {
                return new MovePosition(p, p.childIndex(parent) + 1);
            }
        }
        return null;
    }

    protected enum Direction {
        UP,
        DOWN
    }

    private static class MovePosition {
        final int index;

        final StructuralItem parent;

        private MovePosition(StructuralItem parent, int index) {
            this.index = index;
            this.parent = parent;
        }
    }
}
