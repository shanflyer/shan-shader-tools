package dev.shanflyer.glsl.diagnostics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import dev.shanflyer.glsl.settings.GlslSettings;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class GlslExternalAnnotator
    extends ExternalAnnotator<GlslExternalAnnotator.Snapshot, GlslExternalAnnotator.Result>
    implements DumbAware {
    public record Snapshot(ShaderCompiler.Request request) {}
    public record Result(Snapshot snapshot, ShaderCompiler.Outcome outcome) {}

    @Override
    public Snapshot collectInformation(@NotNull PsiFile file, @NotNull Editor editor,
                                       boolean hasErrors) {
        final var options = GlslSettings.GetInstance().Snapshot();
        final var virtual = file.getVirtualFile();
        if (!options.enabled || virtual == null || !virtual.isInLocalFileSystem()) {
            return null;
        }
        final Path path = virtual.toNioPath().toAbsolutePath().normalize();
        final String base = file.getProject().getBasePath();
        final Path project = base == null ? path.getParent() : Path.of(base);
        final Map<Path, String> unsaved = new HashMap<>();
        final var manager = FileDocumentManager.getInstance();
        for (final var document : manager.getUnsavedDocuments()) {
            final var candidate = manager.getFile(document);
            if (candidate != null && candidate.isInLocalFileSystem()) {
                unsaved.put(candidate.toNioPath().toAbsolutePath().normalize(), document.getText());
            }
        }
        return new Snapshot(new ShaderCompiler.Request(path, file.getText(), project, options,
                                                       Map.copyOf(unsaved)));
    }

    @Override
    public Result doAnnotate(Snapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new Result(
            snapshot, ShaderCompiler.Compile(snapshot.request(), ProgressManager::checkCanceled));
    }

    @Override
    public void apply(@NotNull PsiFile file, Result result, @NotNull AnnotationHolder holder) {
        if (result == null || !file.getText().equals(result.snapshot().request().text())) {
            return;
        }
        final var document = file.getViewProvider().getDocument();
        if (document == null) {
            return;
        }
        final var outcome = result.outcome();
        if (!outcome.problem().isBlank()) {
            holder.newAnnotation(HighlightSeverity.WARNING, outcome.problem())
                .range(new TextRange(0, 0))
                .fileLevel()
                .create();
        }
        for (final var diagnostic : outcome.diagnostics()) {
            final SourceTransform.Origin origin = Origin(outcome, diagnostic.source());
            final boolean included =
                origin != null && !origin.path().equals(result.snapshot().request().file());
            final int sourceLine = included ? origin.rootIncludeLine() : diagnostic.line();
            final int line = Math.clamp(sourceLine - 1, 0, document.getLineCount() - 1);
            int start = document.getLineStartOffset(line);
            final int end = document.getLineEndOffset(line);
            final var text = document.getCharsSequence();
            while (start < end && Character.isWhitespace(text.charAt(start))) {
                ++start;
            }
            final String message =
                included ? origin.path() + ":" + diagnostic.line() + ": " + diagnostic.message()
                         : diagnostic.message();
            final var annotation =
                holder
                    .newAnnotation(diagnostic.error() ? HighlightSeverity.ERROR
                                                      : HighlightSeverity.WARNING,
                                   message)
                    .range(new TextRange(start, end));
            if (start == end) {
                annotation.afterEndOfLine();
            }
            annotation.create();
        }
    }

    private static SourceTransform.Origin Origin(ShaderCompiler.Outcome outcome, String source) {
        try {
            return outcome.origins().get(Integer.parseInt(source));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
