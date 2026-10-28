package dev.shanflyer.glsl;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFileFactory;
import dev.shanflyer.glsl.diagnostics.*;
import dev.shanflyer.glsl.editor.*;
import dev.shanflyer.glsl.language.*;
import dev.shanflyer.glsl.settings.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loaded only into the isolated sandbox, never shipped in the release ZIP. */
public final class SandboxSmoke implements ApplicationStarter {
    private final StringBuilder report_ = new StringBuilder();
    private int checks_ = 0;

    @Override
    public boolean isHeadless() {
        return true;
    }
    @Override
    public int getRequiredModality() {
        return NOT_IN_EDT;
    }

    @Override
    public void main(List<String> arguments) {
        int exitCode = 0;
        try {
            Check(PluginManagerCore.isLoaded(PluginId.getId("dev.shanflyer.glsl")),
                  "Plugin loaded by real CLion plugin manager");
            ApplicationManager.getApplication().invokeAndWait(
                () -> WriteAction.run(this::EditorChecks));
            final Path project = Path.of(System.getProperty("shan.glsl.project"));
            final Path shader = project.resolve("examples/error.frag");
            final var outcome = ShaderCompiler.Compile(
                new ShaderCompiler.Request(shader, Read(shader), project,
                                           GlslSettings.GetInstance().Snapshot(), Map.of()),
                () -> {});
            Check(outcome.diagnostics().stream().anyMatch(CompilerLog.Diagnostic::error),
                  "Compiler runs from plugin classloader");
            report_.append("PASS: ").append(checks_).append(" sandbox integration checks.\n");
        } catch (Throwable exception) {
            exitCode = 1;
            final var writer = new java.io.StringWriter();
            exception.printStackTrace(new java.io.PrintWriter(writer));
            report_.append("FAIL\n").append(writer);
        }
        try {
            Files.writeString(Path.of(System.getProperty("shan.glsl.smoke.report")), report_);
        } catch (Exception exception) {
            exception.printStackTrace();
            exitCode = 1;
        }
        System.out.print(report_);
        System.exit(exitCode);
    }

    private static String Read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void EditorChecks() {
        for (final String extension : ShaderStage.stages) {
            Check(FileTypeManager.getInstance().getFileTypeByFileName("test." + extension) ==
                      GlslFileType.instance,
                  "File association ." + extension);
        }
        final var project = ProjectManager.getInstance().getDefaultProject();
        final String text = "#version 460\nvoid main(){\n vec4 customColor=vec4(1.0);\n " +
                            "gl_Position=customColor;\n}\n";
        final var file = PsiFileFactory.getInstance(project).createFileFromText(
            "test.vert", GlslFileType.instance, text, 0, true);
        Check(file instanceof GlslFile, "Registered parser creates GLSL PSI");
        final var document = PsiDocumentManager.getInstance(project).getDocument(file);
        Check(document != null, "GLSL document exists");
        Check(SyntaxHighlighterFactory.getSyntaxHighlighter(GlslLanguage.instance, project, null)
                      instanceof GlslHighlighter,
              "Registered syntax highlighter");
        Check(new GlslFoldingBuilder().buildFoldRegions(file, document, false).length == 1,
              "Multiline function folds");
        final var settings = GlslSettings.GetInstance();
        final var state = settings.Snapshot();
        state.userKeywords1 = "CUSTOM_FLAG";
        settings.loadState(state);
        Check(settings.Snapshot().userKeywords1.equals("CUSTOM_FLAG"),
              "Persistent settings service");
        final var configurable = new GlslConfigurable();
        Check(configurable.createComponent() != null, "Settings UI constructs in real platform");
        Check(!configurable.isModified(), "Settings reset has no spurious changes");
        configurable.disposeUIResources();
        final var colors = new GlslColorSettingsPage();
        Check(colors.getAttributeDescriptors().length == 12, "Color categories available");
        final var editor = EditorFactory.getInstance().createEditor(document, project);
        try {
            final int offset = text.indexOf("gl_Position") + 2;
            final var parameters =
                new CompletionParameters(file.findElementAt(offset), file, CompletionType.BASIC,
                                         offset, 1, editor, () -> false);
            final var contributor = new GlslCompletionContributor();
            final CapturingResults results = new CapturingResults(contributor);
            contributor.fillCompletionVariants(parameters, results);
            Check(results.words_.contains("gl_Position"), "Built-in completion");
            Check(results.words_.contains("texture"), "Function completion");
            Check(results.words_.contains("vec4"), "Type completion");
            Check(results.words_.contains("customColor"), "Current file identifier completion");
            Check(results.words_.contains("CUSTOM_FLAG"), "User keyword completion");
            final int commentOffset = 6;
            final var commentFile = PsiFileFactory.getInstance(project).createFileFromText(
                "comment.frag", GlslFileType.instance, "// tex\nvoid main(){}\n");
            final var commentParameters = new CompletionParameters(
                commentFile.findElementAt(commentOffset - 1), commentFile, CompletionType.BASIC,
                commentOffset, 1, editor, () -> false);
            final CapturingResults commentResults = new CapturingResults(contributor);
            contributor.fillCompletionVariants(commentParameters, commentResults);
            Check(commentResults.words_.isEmpty(), "No completion in comments");
            Check(CompletionContributor.forLanguage(GlslLanguage.instance)
                      .stream()
                      .anyMatch(item -> item instanceof GlslCompletionContributor),
                  "Completion extension registered");
        } finally {
            EditorFactory.getInstance().releaseEditor(editor);
        }
    }

    private void Check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        ++checks_;
        report_.append("OK ").append(message).append('\n');
    }

    private static final class CapturingResults extends CompletionResultSet {
        private final Set<String> words_ = new HashSet<>();
        private CapturingResults(CompletionContributor contributor) {
            super(PrefixMatcher.ALWAYS_TRUE, result -> {}, contributor);
        }
        @Override
        public void addElement(LookupElement element) {
            words_.add(element.getLookupString());
        }
        @Override
        public CompletionResultSet withPrefixMatcher(PrefixMatcher matcher) {
            return this;
        }
        @Override
        public CompletionResultSet withPrefixMatcher(String matcher) {
            return this;
        }
        @Override
        public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
            return this;
        }
        @Override
        public void addLookupAdvertisement(String text) {
        }
        @Override
        public CompletionResultSet caseInsensitive() {
            return this;
        }
        @Override
        public void restartCompletionOnPrefixChange(ElementPattern<String> condition) {
        }
        @Override
        public void restartCompletionWhenNothingMatches() {
        }
    }
}
