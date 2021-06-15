package org.kdb.inside.brains;

import com.intellij.lang.Language;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class QLanguage extends Language {
    private final List<QKeyword> keywords = new ArrayList<>();
    private final Map<String, List<QKeyword>> systemFunctions = new LinkedHashMap<>();

    private static final String VARIABLE_PATTERN = "([.a-zA-Z][a-zA-Z]|[a-zA-Z])[._a-zA-Z0-9]*";
    private static final Predicate<String> IDENTIFIER = Pattern.compile(VARIABLE_PATTERN).asPredicate();

    public static final QLanguage INSTANCE = new QLanguage();

    private QLanguage() {
        super("q");

        keywords.addAll(loadCompletionItems("keywords", QKeyword.Type.KEYWORD));
        systemFunctions.put(".Q", loadCompletionItems("q", QKeyword.Type.FUNCTION));

        for (String s : Set.of("z", "j", "h")) {
            systemFunctions.put("." + s, loadCompletionItemsOld(s, QKeyword.Type.FUNCTION));
        }
        systemFunctions.put("\\", loadCompletionItemsOld("commands", QKeyword.Type.COMMAND));
    }

    private List<QKeyword> loadCompletionItems(String scope, QKeyword.Type type) {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/org/kdb/inside/brains/completion/" + scope + ".csv");
        if (resourceAsStream == null) {
            return List.of();
        }

        final List<QKeyword> res = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String s = r.readLine();
            while (s != null) {
                final String[] split = s.split(",");

                final String name = split[0].trim();
                final String args = split[1].trim();
                final String desc = split[2].trim();
                res.add(new QKeyword(name, type, args, desc));
                s = r.readLine();
            }
        } catch (IOException ignore) {
        }
        return res;
    }

    private List<QKeyword> loadCompletionItemsOld(String namespace, QKeyword.Type type) {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/org/kdb/inside/brains/completion/" + namespace + ".csv");
        if (resourceAsStream == null) {
            return List.of();
        }

        final List<QKeyword> res = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String s = r.readLine();
            while (s != null) {
                final String[] split = s.split(",");

                final String name = split[0].trim();
                final String desc = split.length > 1 ? split[1].trim() : "";
                res.add(new QKeyword(name, type, desc));
                s = r.readLine();
            }
        } catch (IOException ignore) {
        }
        return res;
    }

    public static List<QKeyword> getKeywords() {
        return Collections.unmodifiableList(INSTANCE.keywords);
    }

    public static Set<String> getSystemNamespaces() {
        return Collections.unmodifiableSet(INSTANCE.systemFunctions.keySet());
    }

    public static List<QKeyword> getSystemFunctions(String namespace) {
        return Collections.unmodifiableList(INSTANCE.systemFunctions.get(namespace));
    }

    public static boolean isSystemFunction(String variableName) {
        final Map<String, List<QKeyword>> systemFunctions = INSTANCE.systemFunctions;
        for (Map.Entry<String, List<QKeyword>> entry : systemFunctions.entrySet()) {
            if (variableName.startsWith(entry.getKey())) {
                return entry.getValue().stream().anyMatch(e -> e.name.equals(variableName));
            }
        }
        return false;
    }

    public static boolean isKeyword(String name) {
        return INSTANCE.keywords.stream().anyMatch(e -> e.name.equals(name));
    }

    public static boolean isIdentifier(String name) {
        return IDENTIFIER.test(name);
    }
}
