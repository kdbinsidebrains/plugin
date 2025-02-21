package org.kdb.inside.brains.ide.qspec;

import org.jdom.Element;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

// https://github.com/nugend/qspec
public record QSpecLibrary(String specFolder, String script) {
    public static QSpecLibrary read(Element e) {
        final Element child = e.getChild("qspec_library");
        if (child == null) {
            return null;
        }
        return new QSpecLibrary(child.getAttributeValue("path"), child.getText());
    }

    public static void validate(String path) throws IllegalStateException {
        final boolean exists = Files.exists(Path.of(path).resolve("lib/init.q"));
        if (!exists) {
            throw new IllegalStateException("QSpec folder doesn't have lib/init.q inside");
        }
    }

    public QSpecLibrary validate() throws IllegalStateException {
        validate(specFolder);
        return this;
    }

    public void write(Element element) {
        final Element l = new Element("qspec_library");
        l.setAttribute("path", specFolder);
        l.setText(script);
        element.addContent(l);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof QSpecLibrary library)) return false;
        return Objects.equals(specFolder, library.specFolder) && Objects.equals(script, library.script);
    }
}
