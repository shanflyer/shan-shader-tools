package dev.shanflyer.glsl.diagnostics;

import dev.shanflyer.glsl.language.GlslLexer;
import dev.shanflyer.glsl.language.GlslTokens;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ShaderStage {
    public static final List<String> stages =
        List.of("vert", "frag", "geom", "comp", "tesc", "tese", "mesh", "task", "rgen", "rint",
                "rmiss", "rahit", "rchit", "rcall");
    private ShaderStage() {
    }

    public static String Detect(String filename, String source, String fallback) {
        final String[] parts = filename.toLowerCase(Locale.ROOT).split("\\.");
        for (int i = parts.length - 1; i >= Math.max(0, parts.length - 2); --i) {
            if (stages.contains(parts[i])) {
                return parts[i];
            }
        }
        final String code = SourceTransform.MaskComments(source);
        final Set<String> words = new HashSet<>();
        final GlslLexer lexer = new GlslLexer();
        lexer.start(code);
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != GlslTokens.string) {
                words.add(lexer.getTokenText());
            }
            lexer.advance();
        }
        if (words.contains("SetMeshOutputsEXT") || words.contains("gl_MeshVerticesEXT")) {
            return "mesh";
        }
        if (words.contains("EmitMeshTasksEXT")) {
            return "task";
        }
        if (words.contains("traceRayEXT") || words.contains("traceNV")) {
            return "rgen";
        }
        if (words.contains("reportIntersectionEXT") || words.contains("reportIntersectionNV")) {
            return "rint";
        }
        if (words.contains("EmitVertex") || words.contains("EmitStreamVertex")) {
            return "geom";
        }
        if (Pattern.compile("\\blocal_size_[xyz](?:_id)?\\b").matcher(code).find()) {
            return "comp";
        }
        if (Pattern.compile("\\blayout\\s*\\([^)]*\\bvertices\\s*=").matcher(code).find()) {
            return "tesc";
        }
        if (words.contains("gl_TessCoord")) {
            return "tese";
        }
        if (words.contains("gl_Position") || words.contains("gl_VertexID") ||
            words.contains("gl_VertexIndex")) {
            return "vert";
        }
        return stages.contains(fallback) ? fallback : "frag";
    }
}
