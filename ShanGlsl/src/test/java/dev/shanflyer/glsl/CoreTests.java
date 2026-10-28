package dev.shanflyer.glsl;

import dev.shanflyer.glsl.diagnostics.CommandArguments;
import dev.shanflyer.glsl.diagnostics.CompilerLocator;
import dev.shanflyer.glsl.diagnostics.CompilerLog;
import dev.shanflyer.glsl.diagnostics.CompilerOptions;
import dev.shanflyer.glsl.diagnostics.ShaderCompiler;
import dev.shanflyer.glsl.diagnostics.ShaderStage;
import dev.shanflyer.glsl.diagnostics.SourceTransform;
import dev.shanflyer.glsl.language.GlslLexer;
import dev.shanflyer.glsl.language.GlslTokens;
import dev.shanflyer.glsl.language.GlslVocabulary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class CoreTests {
    private static int checks_ = 0;
    private CoreTests() {
    }

    public static void main(String[] arguments) throws Exception {
        VocabularyAndLexer();
        StagesAndArguments();
        Transformations();
        Diagnostics();
        RealCompiler();
        ProcessLifetime();
        System.out.println("PASS: " + checks_ +
                           " GLSL checks, including real glslang compilation.");
    }

    private static void Check(boolean passed, String message) {
        ++checks_;
        if (!passed) {
            throw new AssertionError(message);
        }
    }

    private static List<String> Tokens(String source) {
        final var tokens = new ArrayList<String>();
        final GlslLexer lexer = new GlslLexer();
        lexer.start(source);
        int offset = 0;
        while (lexer.getTokenType() != null) {
            Check(lexer.getTokenStart() == offset && lexer.getTokenEnd() > offset,
                  "Lexer must progress without gaps");
            tokens.add(lexer.getTokenType() + ":" + lexer.getTokenText());
            offset = lexer.getTokenEnd();
            lexer.advance();
        }
        Check(offset == source.length(), "Lexer covers all input");
        return tokens;
    }

    private static void VocabularyAndLexer() {
        Check(GlslVocabulary.keywords.contains("vec4"), "vec4 type");
        Check(GlslVocabulary.functions.contains("textureGrad"), "OpenGL functions");
        Check(GlslVocabulary.variables.contains("gl_Position"), "Built-in variable");
        Check(GlslVocabulary.functions.contains("traceRayEXT"), "Ray tracing function");
        final var tokens = Tokens("#version 460\n/* comment\n } */ vec4 v=vec4(1e-2+2.0); // " +
                                  "gl_Position\ntexture(t, uv); gl_Position=v;");
        Check(tokens.contains("PREPROCESSOR:#version"), "Directive token");
        Check(tokens.contains("NUMBER:1e-2"), "Exponent token");
        Check(tokens.contains("OPERATOR:+"), "Do not swallow operators into numbers");
        Check(tokens.contains("FUNCTION:texture"), "Built-in functions colored");
        Check(tokens.contains("VARIABLE:gl_Position"), "Built-in variables colored");
        Check(Tokens("/**/ vec4").contains("KEYWORD:vec4"), "Empty block comment terminates");
        Check(Tokens("/*/ vec4").size() == 1,
              "Opening asterisk cannot also terminate a block comment");
        Check(Tokens("\"https://example/ //! text\"").size() == 1,
              "String contains no line-comment token");
        Check(Tokens("0xffu .5 1.0e+2 7-2").contains("NUMBER:0xffu"), "Hex numeric literal");
        final String incomplete = "/* unfinished";
        final GlslLexer first = new GlslLexer();
        first.start(incomplete);
        Check(first.getTokenType() == GlslTokens.blockComment, "Unclosed comment token");
        first.advance();
        final GlslLexer resumed = new GlslLexer();
        resumed.start("continuation */ vec4", 0, "continuation */ vec4".length(), first.getState());
        Check(resumed.getTokenType() == GlslTokens.blockComment, "Incremental block-comment state");
    }

    private static void StagesAndArguments() {
        Check(ShaderStage.Detect("test.vert.glsl", "", "frag").equals("vert"),
              "Compound extension");
        Check(
            ShaderStage.Detect("test.glsl", "// gl_Position\nvoid main(){}", "frag").equals("frag"),
            "Ignore comments in stage detection");
        Check(ShaderStage.Detect("test.glsl", "layout(local_size_x=32) in;", "frag").equals("comp"),
              "Compute inference");
        Check(ShaderStage.Detect("test.glsl", "layout(vertices = 3) out;", "frag").equals("tesc"),
              "Tessellation inference");
        Check(ShaderStage.Detect("test.rmiss", "gl_Position", "frag").equals("rmiss"),
              "Explicit extension wins");
        Check(CommandArguments.Split("-I\"C:\\Shader Includes\" -DNAME=1")
                  .equals(List.of("-IC:\\Shader Includes", "-DNAME=1")),
              "Windows paths retain backslashes");
        Check(CommandArguments.Split("'path with spaces' \"\"")
                  .equals(List.of("path with spaces", "")),
              "Quoted arguments");
        boolean rejected = false;
        try {
            CommandArguments.Split("\"unclosed");
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        Check(rejected, "Unclosed argument quote rejected");
    }

    private static void Transformations() throws Exception {
        Check(SourceTransform.SpecialComments("wrong //! #version 460\r\n//? void main(){}", true)
                  .equals(" #version 460\n void main(){}"),
              "Root special comments replace preceding line code");
        Check(SourceTransform.SpecialComments("//? #version 460\n//! float x;", false)
                  .equals("//? #version 460\n float x;"),
              "Included //? stays inactive");
        Check(SourceTransform.SpecialComments("/* //! no */\n\"//! no\"", true)
                  .equals("/* //! no */\n\"//! no\""),
              "Markers inside block comments and strings are inert");
        final Path root = Path.of("test-shaders").toAbsolutePath();
        final Path main = root.resolve("main.frag");
        final Path include = root.resolve("common.glsl");
        final var expanded = SourceTransform.Expand(
            main, "#version 460\n#include \"common.glsl\"\nvoid main(){}\n", List.of(), path -> {
                if (path.equals(include)) {
                    return "//? #version 460\nfloat helper;\n";
                }
                throw new java.io.IOException("not found");
            });
        Check(expanded.text().startsWith("#version 460\n#line 1 1\n"), "#version remains first");
        Check(expanded.text().contains("#line 3 0\nvoid main(){}"),
              "Restore parent line numbering");
        Check(expanded.origins().get(1).path().equals(include), "Include file origin");
        Check(expanded.origins().get(1).rootIncludeLine() == 2, "Include annotation line");
        final var ignored =
            SourceTransform.Expand(main, "/*\n#include \"missing.glsl\"\n*/\nvoid main(){}",
                                   List.of(), SourceTransform::ReadDisk);
        Check(!ignored.text().contains("#error"), "Includes inside comments ignored");
        final var cycle = SourceTransform.Expand(main, "#include \"common.glsl\"", List.of(),
                                                 path -> "#include \"common.glsl\"");
        Check(cycle.text().contains("SHAN_GLSL_cyclic"), "Unbounded includes prevented");
    }

    private static void Diagnostics() {
        final var log = CompilerLog.Parse(
            "ERROR: 1:7: 'x' : undeclared identifier\nWARNING: 0:3: warning text\n"
            + "0(8) : error C0000: syntax error\nC:\\shaders\\demo.frag:9:4: error: bad value\n");
        Check(log.size() == 4, "glslang, NVIDIA and GCC diagnostic formats");
        Check(log.get(0).source().equals("1") && log.get(0).line() == 7, "Source id and line");
        Check(!log.get(1).error(), "Warning severity");
        Check(log.get(3).source().equals("C:\\shaders\\demo.frag") && log.get(3).line() == 9,
              "Windows filename diagnostic");
    }

    private static void RealCompiler() throws Exception {
        Check(CompilerLocator.Find("") != null,
              "Vulkan SDK / PATH compiler required for integration tests");
        final Path directory = Files.createTempDirectory("shan-glsl-tests-");
        try {
            final CompilerOptions options = new CompilerOptions();
            final Path file = directory.resolve("shader.vert");
            final String valid = "#version 460\nlayout(location=0) in vec3 position;\nvoid " +
                                 "main(){ gl_Position=vec4(position,1.0); }\n";
            var outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, valid, directory, options, Map.of()), () -> {});
            Check(outcome.problem().isEmpty() && outcome.diagnostics().isEmpty(),
                  "Valid Vulkan vertex shader: " + outcome);
            final String invalid = valid.replace("position,1.0", "missingName,1.0");
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, invalid, directory, options, Map.of()), () -> {});
            Check(outcome.diagnostics().stream().anyMatch(
                      diagnostic -> diagnostic.error() && diagnostic.line() == 3),
                  "Unsaved shader error on original line");
            final Path include = directory.resolve("common.glsl");
            Files.writeString(include,
                              "//? #version 460\nvec4 GetPosition(){ return vec4(1.0); }\n");
            final String withInclude = "#version 460\n#include \"common.glsl\"\nvoid main(){ " +
                                       "gl_Position=GetPosition(); }\n";
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, withInclude, directory, options, Map.of()),
                () -> {});
            Check(outcome.problem().isEmpty() && outcome.diagnostics().isEmpty(),
                  "Include works without requiring GOOGLE include extension: " + outcome);
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(
                    file, withInclude, directory, options,
                    Map.of(include, "//? #version 460\nvec4 GetPosition(){ return missing; }\n")),
                () -> {});
            Check(outcome.diagnostics().stream().anyMatch(
                      diagnostic -> diagnostic.source().equals("1") && diagnostic.line() == 2),
                  "Unsaved include with mapped line");
            final String inactive = "#version 460\n#if 0\n#include " +
                                    "\"missing.glsl\"\n#endif\nvoid main(){gl_Position=vec4(1.0);}";
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, inactive, directory, options, Map.of()), () -> {});
            Check(outcome.diagnostics().isEmpty() && outcome.problem().isEmpty(),
                  "Missing inactive include produces no false positive: " + outcome);
            final String commentInclude =
                withInclude.replace("#include", "/* block comment\n*/ #include");
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, commentInclude, directory, options, Map.of()),
                () -> {});
            Check(outcome.diagnostics().isEmpty() && outcome.problem().isEmpty(),
                  "Block comment ending on include line: " + outcome);
            Files.writeString(include,
                              "#ifndef COMMON_GLSL\n#define COMMON_GLSL\n#include " +
                              "\"common.glsl\"\nvec4 GetPosition(){return vec4(1.0);}\n#endif\n");
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, withInclude, directory, options, Map.of()),
                () -> {});
            Check(outcome.diagnostics().isEmpty() && outcome.problem().isEmpty(),
                  "Guarded cyclic include: " + outcome);
            options.target = "OpenGL";
            final String fragment = "#version 460\nlayout(location=0) out vec4 color;\nvoid " +
                                    "main(){ color=vec4(1.0); }\n";
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(directory.resolve("test.frag"), fragment, directory,
                                           options, Map.of()),
                () -> {});
            Check(outcome.problem().isEmpty() && outcome.diagnostics().isEmpty(),
                  "OpenGL source validation");
            options.compilerPath = directory.resolve("missing.exe").toString();
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, valid, directory, options, Map.of()), () -> {});
            Check(outcome.problem().contains("not found"), "Missing compiler is visible");
            options.enabled = false;
            outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(file, invalid, directory, options, Map.of()), () -> {});
            Check(outcome.problem().isEmpty() && outcome.diagnostics().isEmpty(),
                  "Disabled validation");
        } finally {
            try (final var paths = Files.walk(directory)) {
                for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    private static void ProcessLifetime() throws Exception {
        final CompilerOptions options = new CompilerOptions();
        options.target = "Custom";
        options.compilerPath =
            Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        options.arguments =
            "-classpath \"" +
            Path.of(CoreTests.class.getProtectionDomain().getCodeSource().getLocation().toURI()) +
            "\" dev.shanflyer.glsl.FakeCompiler {file}";
        options.timeoutMillis = 250;
        final Path file = Path.of("timeout.vert").toAbsolutePath();
        final var request =
            new ShaderCompiler.Request(file, "void main(){}", file.getParent(), options, Map.of());
        final long start = System.nanoTime();
        final var outcome = ShaderCompiler.Compile(request, () -> {});
        Check(outcome.problem().contains("timed out"), "Slow compiler times out: " + outcome);
        Check(System.nanoTime() - start < java.util.concurrent.TimeUnit.SECONDS.toNanos(5),
              "Timeout promptly returns");
        options.timeoutMillis = 60000;
        final int[] calls = {0};
        boolean canceled = false;
        try {
            ShaderCompiler.Compile(request, () -> {
                if (++calls[0] >= 3) {
                    throw new java.util.concurrent.CancellationException("Test cancellation");
                }
            });
        } catch (java.util.concurrent.CancellationException exception) {
            canceled = true;
        }
        Check(canceled, "Cancellation propagates and reaches process cleanup");
        Check(ProcessHandle.allProcesses().noneMatch(
                  process
                  -> process.info()
                         .commandLine()
                         .map(line
                              -> line.contains("dev.shanflyer.glsl.FakeCompiler") &&
                                     !line.contains("CoreTests"))
                         .orElse(false)),
              "No fake compiler process left alive");
    }
}
