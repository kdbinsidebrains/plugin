package org.kdb.inside.brains.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KdbQueryTest {
    @Test
    void normalize() {
        String expr = "" +
                "\\c 100 300\n\n" +
                "\\l asd\n" +
                "\\l qwe\n\n" +
                "a:10;b:20\n" +
                "f1:{[a;b]\n" +
                "   a+b\n" +
                "}  \n" +
                "f2:{[a;b]\n\n" +
                "   a+b\n\n" +
                " }\n\n\n" +
                "f2:{[a;b]\n" +
                "   a+b}\n" +
                "d:12\n" +
                "\\cd";

        String norm = "" +
                "system[\"c 100 300\"];\n\n" +
                "system[\"l asd\"];\n" +
                "system[\"l qwe\"];\n\n" +
                "a:10;b:20;\n" +
                "f1:{[a;b]\n" +
                "   a+b\n" +
                "}  ;\n" +
                "f2:{[a;b]\n\n" +
                "   a+b\n\n" +
                " };\n\n\n" +
                "f2:{[a;b]\n" +
                "   a+b};\n" +
                "d:12;\n" +
                "system[\"cd\"]";

        assertEquals(norm, KdbQuery.normalizeQuery(expr));
    }
}