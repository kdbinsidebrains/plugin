package org.kdb.inside.brains.view.inspector;

import com.intellij.navigation.ItemPresentation;
import icons.KdbIcons;
import kx.c;
import org.junit.jupiter.api.Test;
import org.kdb.inside.brains.core.InstanceConnection;
import org.kdb.inside.brains.core.KdbResult;
import org.kdb.inside.brains.view.inspector.model.InstanceElement;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class InspectorToolWindowTest {
    @Test
    void getSuggestions() {
        final KdbResult result = new KdbResult();
        result.complete(new Object[]{
                "",
                new Object[]{f("myFunction", 100, "x", "y"), f("anotherFunction", 102)},
                new Object[]{t("table1", 10), t("myTable", 100)},
                new Object[]{v("variable", -6), v("variuble", 6)},
                new Object[]{
                        new Object[]{
                                "ns",
                                new Object[]{f("myFunction", 100, "x", "y")},
                                new Object[]{t("table1", 10)},
                                new Object[]{v("variable", -6)},
                                new Object[]{
                                        new Object[]{
                                                "inner",
                                                new Object[]{f("anotherFunction", 102)},
                                                new Object[]{t("myTable", 100)},
                                                new Object[]{v("variuble", 6)},
                                                new Object[0]
                                        }
                                }
                        }
                },
        });

        final InstanceElement ie = new InstanceElement(mock(InstanceConnection.class), result);
        // Functions
        assertEquals(0, InspectorToolWindow.getSuggestions("asd", ie).size());
        assertEquals(0, InspectorToolWindow.getSuggestions(".myFunction", ie).size());
        assertEquals(1, InspectorToolWindow.getSuggestions("myF", ie).size());
        assertItem("myFunction", "\u03BB[x, y]", KdbIcons.Node.Function, InspectorToolWindow.getSuggestions("myF", ie).get(0));

        // Tables
        assertEquals(0, InspectorToolWindow.getSuggestions("tableus", ie).size());
        assertEquals(0, InspectorToolWindow.getSuggestions(".table", ie).size());
        assertEquals(1, InspectorToolWindow.getSuggestions("table", ie).size());
        assertItem("table1", "1000 columns, 10 rows, memorable", KdbIcons.Node.Table, InspectorToolWindow.getSuggestions("table", ie).get(0));

        // Variable
        assertEquals(0, InspectorToolWindow.getSuggestions("vauiriable", ie).size());
        assertEquals(0, InspectorToolWindow.getSuggestions(".vari", ie).size());
        assertEquals(2, InspectorToolWindow.getSuggestions("vari", ie).size());
        assertItem("variable", "int", KdbIcons.Node.Variable, InspectorToolWindow.getSuggestions("vari", ie).get(0));
        assertItem("variuble", "list of ints", KdbIcons.Node.Variable, InspectorToolWindow.getSuggestions("vari", ie).get(1));

        // Namespace
        assertEquals(6, InspectorToolWindow.getSuggestions(".ns", ie).size());
        assertEquals(6, InspectorToolWindow.getSuggestions(".ns.", ie).size());
        assertEquals(3, InspectorToolWindow.getSuggestions(".ns.inn", ie).size());
        assertEquals(1, InspectorToolWindow.getSuggestions(".ns.inner.myTa", ie).size());

    }

    private Object v(String name, int type) {
        return new Object[]{name, type, null};
    }

    private Object f(String name, int type, String... args) {
        return new Object[]{name, type, args};
    }

    private Object t(String name, long size) {
        c.Flip flip = new c.Flip(new c.Dict(new String[]{"asd"}, new Object[]{new Object[1000]}));
        return new Object[]{name, size, flip, new String[]{"asd"}};
    }

    private void assertItem(String name, String desc, Icon icon, ItemPresentation presentation) {
        assertEquals(name, presentation.getPresentableText());
        assertEquals(desc, presentation.getLocationString());
        assertEquals(icon, presentation.getIcon(false));
    }
}