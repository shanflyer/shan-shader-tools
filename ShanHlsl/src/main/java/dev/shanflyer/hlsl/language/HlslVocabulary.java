package dev.shanflyer.hlsl.language;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class HlslVocabulary {
    public static final Set<String> keywords = Load("keywords.txt");
    public static final Set<String> functions = Load("functions.txt");
    private static final Pattern semantics_ =
        Pattern.compile("SV_[A-Za-z0-9_]+|(?:POSITION|NORMAL|TEXCOORD|COLOR|TANGENT|BINORMAL|" +
                        "PSIZE|BLENDINDICES|BLENDWEIGHT|DEPTH|VPOS|VFACE)[0-9]*",
                        Pattern.CASE_INSENSITIVE);

    private HlslVocabulary() {
    }
    public static boolean IsSemantic(String word) {
        return semantics_.matcher(word).matches();
    }

    private static Set<String> Load(String name) {
        try (final InputStream stream =
                 HlslVocabulary.class.getResourceAsStream("/vocabulary/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("Missing HLSL vocabulary: " + name);
            }
            return Arrays
                .stream(new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\s+"))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
