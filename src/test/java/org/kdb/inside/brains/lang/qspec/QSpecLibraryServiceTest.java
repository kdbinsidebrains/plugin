package org.kdb.inside.brains.lang.qspec;

import com.intellij.openapi.util.JDOMUtil;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QSpecLibraryServiceTest {
    @Test
    void getState() {
        final QSpecLibraryService service = new QSpecLibraryService() {
            @Override
            protected void forceLibraryRescan() {
            }
        };
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());
        assertNull(service.getState());

        service.setCustomScript("script");
        assertEquals("<qspec_library>script</qspec_library>", JDOMUtil.write(service.getState()));

        service.setLibraryPath("path");
        assertEquals("<qspec_library path=\"path\">script</qspec_library>", JDOMUtil.write(service.getState()));

        service.setCustomScript("");
        assertEquals("<qspec_library path=\"path\" />", JDOMUtil.write(service.getState()));

        service.setLibraryPath("");
        assertNull(service.getState());
    }

    @Test
    void setState() {
        final QSpecLibraryService service = new QSpecLibraryService() {
            @Override
            protected void forceLibraryRescan() {
            }
        };
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());

        final Element e = new Element("qspec_library");
        service.loadState(e);
        assertNull(service.getCustomScript());
        assertNull(service.getLibraryPath());

        e.setAttribute("path", "path");
        service.loadState(e);
        assertNull(service.getCustomScript());
        assertEquals("path", service.getLibraryPath());

        e.setText("script");
        service.loadState(e);
        assertEquals("path", service.getLibraryPath());
        assertEquals("script", service.getCustomScript());
    }
}