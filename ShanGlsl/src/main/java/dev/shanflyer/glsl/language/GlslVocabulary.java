package dev.shanflyer.glsl.language;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GlslVocabulary {
    public static final Set<String> keywords = Load("keywords.txt");
    public static final Set<String> functions = Load("functions.txt");
    public static final Set<String> variables = Load("variables.txt");
    public static final Set<String> directives =
        Set.of("define", "undef", "if", "ifdef", "ifndef", "else", "elif", "endif", "error",
               "pragma", "extension", "version", "line", "include");

    private GlslVocabulary() {
    }

    private static Set<String> Load(String name) {
        try (final InputStream stream =
                 GlslVocabulary.class.getResourceAsStream("/vocabulary/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing GLSL vocabulary: " + name);
            }
            final String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(text.trim().split("\\s+"))));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read GLSL vocabulary: " + name, exception);
        }
    }
}
