package dev.shanflyer.hlsl.lsp;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspClientManager;
import com.intellij.platform.lsp.api.LspServerState;
import com.intellij.psi.PsiFile;
import dev.shanflyer.hlsl.editor.HlslHighlighter;
import dev.shanflyer.hlsl.settings.HlslSettings;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.jetbrains.annotations.NotNull;

public final class HlslClassificationAnnotator
    extends ExternalAnnotator<HlslClassificationAnnotator.Snapshot,
                              HlslLanguageServer.ClassificationResult> {
    public record Snapshot(Project project, VirtualFile file) {}

    @Override
    public Snapshot collectInformation(@NotNull PsiFile file, @NotNull Editor editor,
                                       boolean errors) {
        final var options = HlslSettings.GetInstance().Snapshot();
        return options.enabled && options.semanticColors && file.getVirtualFile() != null
            ? new Snapshot(file.getProject(), file.getVirtualFile())
            : null;
    }

    @Override
    public HlslLanguageServer.ClassificationResult doAnnotate(Snapshot snapshot) {
        if (snapshot == null || snapshot.project().isDisposed()) {
            return null;
        }
        final var clients = LspClientManager.getInstance(snapshot.project())
                                .getClients(HlslLspIntegrationProvider.class);
        for (final var client : clients) {
            if (client.getState() != LspServerState.Running) {
                continue;
            }
            final var params = new HlslLanguageServer.ClassificationParams(
                client.getDocumentIdentifier(snapshot.file()));
            try {
                return client.sendRequestSync(
                    5000, server -> ((HlslLanguageServer)server).classifications(params));
            } catch (ProcessCanceledException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                // External unmodified servers may not implement this optional request.
                return null;
            }
        }
        return null;
    }

    @Override
    public void apply(@NotNull PsiFile file, HlslLanguageServer.ClassificationResult result,
                      @NotNull AnnotationHolder holder) {
        if (result == null || result.spans() == null || !file.getText().equals(result.text())) {
            return;
        }
        for (final var span : result.spans()) {
            if (span.kind() == null || span.start() < 0 || span.end() <= span.start() ||
                span.end() > file.getTextLength()) {
                continue;
            }
            final var key = HlslHighlighter.Classification(span.kind());
            if (key == null) {
                continue;
            }
            final var annotation = holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                                       .range(new TextRange(span.start(), span.end()));
            if (key == HlslHighlighter.excluded) {
                annotation.enforcedTextAttributes(
                    EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key));
            } else {
                annotation.textAttributes(key);
            }
            annotation.create();
        }
    }
}
