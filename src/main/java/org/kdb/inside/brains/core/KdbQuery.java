package org.kdb.inside.brains.core;

import java.util.Arrays;

public class KdbQuery {
    private final String expr;
    private final Object[] args;

    public KdbQuery(String expr) {
        this.expr = expr;
        this.args = null;
    }

    public KdbQuery(String expr, Object... args) {
        this.expr = expr;
        this.args = args;
    }

    public Object toQueryObject(boolean normalize) {
        final char[] q = (normalize ? normalizeQuery(expr) : expr).toCharArray();
        if (args == null) {
            return q;
        }

        final Object[] res = new Object[args.length + 1];
        System.arraycopy(args, 0, res, 1, args.length);
        res[0] = q;
        return res;
    }

    public String getExpression() {
        return expr;
    }

    public Object[] getArguments() {
        return args == null ? null : args.clone();
    }

    @Override
    public String toString() {
        return "KdbQuery{" +
                "expr='" + expr + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }

    protected static String normalizeQuery(String q) {
        final String[] lines = q.lines().toArray(String[]::new);
        if (lines.length == 0) {
            return q;
        }

        int row = 0;
        for (int i = row; i < lines.length - 1; i++) {
            fixSystemCall(lines, i);

            final String next = lines[i + 1];
            final String strip = next.strip();
            if (strip.isEmpty()) {
                continue;
            }

            final char c = next.charAt(0);
            if (c != ' ' && c != '\t' && c != '/' && !strip.equals("}")) {
                lines[row] += ';';
            }
            row = i + 1;
        }
        fixSystemCall(lines, lines.length - 1);
        return String.join("\n", lines);
    }

    private static void fixSystemCall(String[] lines, int i) {
        final String line = lines[i];
        if (line.length() > 1 && line.charAt(0) == '\\') {
            lines[i] = "system[\"" + line.substring(1) + "\"]";
        }
    }
}
