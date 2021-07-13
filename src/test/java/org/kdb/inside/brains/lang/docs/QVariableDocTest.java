package org.kdb.inside.brains.lang.docs;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QVariableDocTest {
    @ParameterizedTest
    @MethodSource("docExamples")
    void docsGroup(String txt) {
        final QVariableDoc docs = QVariableDoc.from(".asd.qwe[x;qe;f]", txt);
        assertEquals("This is test function", docs.getDescription());

        final List<QVariableDoc.Parameter> parameters = docs.getParameters();
        assertEquals(4, parameters.size());
        assertParameter("params", "z this is test but many lines", parameters.get(0));
        assertParameter("returns", "z*10", parameters.get(1));
        assertParameter("throws", "'type if parameter is not number", parameters.get(2));
        assertParameter("undefined", "", parameters.get(3));

        System.out.println(docs.toHtml());
    }

    private void assertParameter(String name, String description, QVariableDoc.Parameter parameter) {
        assertEquals(name, parameter.getName());
        assertEquals(description, parameter.getDescription());
    }

    private static Stream<String> docExamples() {
        String v1 = "/\n" +
                "This \n" +
                "is \n" +
                "test \n" +
                "function\n" +
                "\n" +
                "\n" +
                "@params z this is test\n" +
                "but many lines\n" +
                "@returns z*10\n" +
                "\n" +
                "@throws 'type if parameter is not number\n" +
                "@undefined\n" +
                "\\";

        String v2 = "/ This \n" +
                "/ is \n" +
                "/ test \n" +
                "/ function\n" +
                "/ \n" +
                "/ \n" +
                "/ @params z this is test\n" +
                "// but many lines\n" +
                "// @returns z*10\n" +
                "/ \n" +
                "/ @throws 'type if parameter is not number\n" +
                "/ @undefined";

        return Stream.of(v1, v2);
    }
}