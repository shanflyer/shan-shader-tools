package dev.shanflyer.glsl.diagnostics;

import dev.shanflyer.glsl.language.GlslLexer;
import dev.shanflyer.glsl.language.GlslTokens;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class SourceTransform {
    private static final Pattern includePattern_ =
        Pattern.compile("(?m)^[\\t ]*#[\\t ]*include[\\t ]*[\"<]([^\">]+)[\">][^\\n]*");
    private static final int maxExpandedChars_ = 4 * 1024 * 1024;
    private static final int maxFileChars_ = 1024 * 1024;

    @FunctionalInterface
    public interface Reader {
        String Read(Path path) throws IOException;
    }
    public record Origin(Path path, int rootIncludeLine) {}
    public record Expanded(String text, Map<Integer, Origin> origins) {}

    private final Reader reader_;
    private final List<Path> directories_;
    private final Map<Integer, Origin> origins_ = new LinkedHashMap<>();
    private final Set<Path> stack_ = new HashSet<>();
    private int expandedChars_ = 0;

    private SourceTransform(Reader reader, List<Path> directories) {
        reader_ = reader;
        directories_ = directories;
    }

    public static Expanded Expand(Path path, String text, List<Path> directories, Reader reader)
        throws IOException {
        final SourceTransform transform = new SourceTransform(reader, directories);
        final Path normalized = path.toAbsolutePath().normalize();
        transform.origins_.put(0, new Origin(normalized, 1));
        final String expanded = transform.ExpandFile(normalized, text, 0, 1);
        return new Expanded(expanded, Map.copyOf(transform.origins_));
    }

    public static String ReadDisk(Path path) throws IOException {
        if (Files.size(path) > maxFileChars_) {
            throw new IOException("Include exceeds 1 MiB: " + path);
        }
        return Files.readString(path).replaceFirst("^\\uFEFF", "");
    }

    public static String SpecialComments(String source, boolean root) {
        final String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        final GlslLexer lexer = new GlslLexer();
        lexer.start(normalized);
        final List<int[]> replacements = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() == GlslTokens.lineComment) {
                final int start = lexer.getTokenStart();
                final String comment = lexer.getTokenText();
                if (comment.startsWith("//!") || root && comment.startsWith("//?")) {
                    replacements.add(
                        new int[] {normalized.lastIndexOf('\n', start - 1) + 1, start + 3});
                }
            }
            lexer.advance();
        }
        final StringBuilder result = new StringBuilder(normalized);
        for (int i = replacements.size() - 1; i >= 0; --i) {
            final int[] range = replacements.get(i);
            result.delete(range[0], range[1]);
        }
        return result.toString();
    }

    public static String MaskComments(String source) {
        final StringBuilder masked = new StringBuilder(source);
        final GlslLexer lexer = new GlslLexer();
        lexer.start(source);
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() == GlslTokens.lineComment ||
                lexer.getTokenType() == GlslTokens.blockComment) {
                for (int i = lexer.getTokenStart(); i < lexer.getTokenEnd(); ++i) {
                    if (masked.charAt(i) != '\n' && masked.charAt(i) != '\r') {
                        masked.setCharAt(i, ' ');
                    }
                }
            }
            lexer.advance();
        }
        return masked.toString();
    }

    private String ExpandFile(Path path, String source, int sourceId, int rootLine)
        throws IOException {
        if (source.length() > maxFileChars_) {
            throw new IOException("Shader source exceeds 1 MiB: " + path);
        }
        expandedChars_ += source.length();
        if (expandedChars_ > maxExpandedChars_) {
            throw new IOException("Expanded shader exceeds 4 MiB");
        }
        if (stack_.size() >= 32) {
            return "#error SHAN_GLSL_cyclic_include_or_depth_exceeded\n";
        }
        if (!stack_.add(path)) {
            // Preserve include guards around a repeated header. An active unguarded
            // recursive include still reaches #error; an inactive guarded one does not.
            final String repeated = MaskComments(SpecialComments(source, false));
            return includePattern_.matcher(repeated).replaceAll("#error SHAN_GLSL_cyclic_include");
        }
        try {
            final String transformed = MaskComments(SpecialComments(source, sourceId == 0));
            final var matcher = includePattern_.matcher(transformed);
            final StringBuilder result = new StringBuilder();
            int previous = 0;
            int line = 1;
            while (matcher.find()) {
                result.append(transformed, previous, matcher.start());
                line += (int)transformed.substring(previous, matcher.start())
                            .chars()
                            .filter(c -> c == '\n')
                            .count();
                final String includeName = matcher.group(1);
                final Path include = Resolve(path, includeName);
                final int includeId = origins_.size();
                final int includeRootLine = sourceId == 0 ? line : rootLine;
                origins_.put(includeId, new Origin(include, includeRootLine));
                result.append("#line 1 ").append(includeId).append('\n');
                try {
                    final String included = reader_.Read(include);
                    result.append(ExpandFile(include, included, includeId, includeRootLine));
                } catch (IOException exception) {
                    // Let the compiler evaluate surrounding #if / #ifdef directives first.
                    result.append("#error SHAN_GLSL_cannot_read_include ")
                        .append(includeName.replaceAll("[\\r\\n]", " "))
                        .append('\n');
                }
                result.append("\n#line ")
                    .append(line + 1)
                    .append(' ')
                    .append(sourceId)
                    .append('\n');
                previous = matcher.end();
                // Consume the original newline; #line already starts the next source line.
                if (previous < transformed.length() && transformed.charAt(previous) == '\n') {
                    ++previous;
                }
                ++line;
            }
            result.append(transformed, previous, transformed.length());
            return result.toString();
        } finally {
            stack_.remove(path);
        }
    }

    private Path Resolve(Path parent, String include) throws IOException {
        try {
            final Path requested = Path.of(include);
            if (requested.isAbsolute()) {
                return requested.normalize();
            }
            final Path relative = parent.getParent().resolve(requested).normalize();
            try {
                reader_.Read(relative);
                return relative;
            } catch (IOException ignored) {
            }
            for (final Path directory : directories_) {
                final Path candidate = directory.resolve(requested).normalize();
                try {
                    reader_.Read(candidate);
                    return candidate;
                } catch (IOException ignored) {
                }
            }
            return relative;
        } catch (java.nio.file.InvalidPathException exception) {
            throw new IOException("Invalid include path: " + include, exception);
        }
    }
}
