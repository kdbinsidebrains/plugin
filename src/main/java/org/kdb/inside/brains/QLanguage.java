package org.kdb.inside.brains;

import com.intellij.lang.Language;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.stream.Stream.concat;

public final class QLanguage extends Language {
    private final List<QWord> keywords;
    private final Map<String, List<QWord>> systemFunctions = new LinkedHashMap<>();

    private final Map<String, QWord> words = new HashMap<>();

    private static final String VARIABLE_PATTERN = "([.a-zA-Z][a-zA-Z]|[a-zA-Z])[._a-zA-Z0-9]*";
    private static final Predicate<String> IDENTIFIER = Pattern.compile(VARIABLE_PATTERN).asMatchPredicate();

    public static final QLanguage INSTANCE = new QLanguage();

    public static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("KdbInsideBrains");

    private QLanguage() {
        super("q");

        keywords = loadCompletionItems("keywords", QWord.Type.KEYWORD);
        systemFunctions.put(".Q", loadCompletionItems("q", QWord.Type.FUNCTION));
        for (String s : Set.of("z", "j", "h")) {
            systemFunctions.put("." + s, loadCompletionItems(s, QWord.Type.FUNCTION));
        }
        systemFunctions.put("\\", loadCompletionItems("commands", QWord.Type.COMMAND));

        concat(keywords.stream(), systemFunctions.values().stream().flatMap(Collection::stream)).forEach(w -> words.put(w.getName(), w));
    }

    @Override
    public @NotNull String getDisplayName() {
        return "KDB+ Q";
    }

    public static QWord getWord(String name) {
        return INSTANCE.words.get(name);
    }


    public static Collection<QWord> getKeywords() {
        return Collections.unmodifiableCollection(INSTANCE.keywords);
    }

    public static Set<String> getSystemNamespaces() {
        return Collections.unmodifiableSet(INSTANCE.systemFunctions.keySet());
    }

    public static List<QWord> getSystemFunctions(String namespace) {
        return Collections.unmodifiableList(INSTANCE.systemFunctions.get(namespace));
    }

    public static boolean isSystemFunction(String name) {
        final QWord w = getWord(name);
        return w != null && w.getType() == QWord.Type.FUNCTION;
    }

    public static boolean isKeyword(String name) {
        final QWord w = getWord(name);
        return w != null && w.getType() == QWord.Type.KEYWORD;
    }

    private List<QWord> loadCompletionItems(String scope, QWord.Type type) {
        final InputStream resourceAsStream = getClass().getResourceAsStream("/org/kdb/inside/brains/words/" + scope + ".csv");
        if (resourceAsStream == null) {
            return List.of();
        }

        final List<QWord> res = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resourceAsStream))) {
            String s = r.readLine();
            while (s != null) {
                final String[] split = s.split(",");

                final String name = split[0].trim();
                final String args = split[1].trim();
                final String desc = split[2].trim();
                final String docs = split.length > 3 ? split[3].trim() : null;
                res.add(new QWord(name, type, args, desc, docs));
                s = r.readLine();
            }
        } catch (IOException ignore) {
        }
        return res;
    }

    public static boolean isIdentifier(String name) {
        return IDENTIFIER.test(name);
    }
}
