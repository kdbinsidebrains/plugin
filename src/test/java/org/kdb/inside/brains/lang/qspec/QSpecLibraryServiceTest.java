package org.kdb.inside.brains.lang.qspec;

import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QSpecLibraryServiceTest {
    private static void loadState(QSpecLibraryService service, Element e) {
        try {
            service.loadState(e);
        } catch (Exception ignore) {
            // library never can be loaded in the test
        }
    }

    private static void changePath(QSpecLibraryService service, String path) {
        try {
            service.setLibraryPath(path);
        } catch (Exception ignore) {
            // library never can be loaded in the test
        }
    }

    @Test
    void getState() {
        final QSpecLibraryService service = new QSpecLibraryService();
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());
        assertNull(service.getState());

        service.setCustomScript("script");
        assertEquals("<qspec_library>script</qspec_library>", new XMLOutputter().outputString(service.getState()));

        changePath(service, "path");
        assertEquals("<qspec_library path=\"path\">script</qspec_library>", new XMLOutputter().outputString(service.getState()));

        service.setCustomScript("");
        assertEquals("<qspec_library path=\"path\" />", new XMLOutputter().outputString(service.getState()));

        changePath(service, "");
        assertNull(service.getState());
    }

    @Test
    void setState() {
        final QSpecLibraryService service = new QSpecLibraryService();
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());

        final Element e = new Element("qspec_library");
        loadState(service, e);
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());

        e.setAttribute("path", "path");
        loadState(service, e);
        assertNull(service.getCustomScript());
        assertEquals("path", service.getLibraryPath());

        e.setText("script");
        loadState(service, e);
        assertEquals("path", service.getLibraryPath());
        assertEquals("script", service.getCustomScript());
    }
}