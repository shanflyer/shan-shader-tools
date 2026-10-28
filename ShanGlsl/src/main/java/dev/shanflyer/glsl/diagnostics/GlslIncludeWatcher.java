package dev.shanflyer.glsl.diagnostics;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiManager;
import com.intellij.util.Alarm;
import dev.shanflyer.glsl.language.GlslFileType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class GlslIncludeWatcher implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
        final Runnable schedule = () -> {
            if (project.isDisposed()) {
                return;
            }
            alarm.cancelAllRequests();
            alarm.addRequest(() -> {
                if (project.isDisposed()) {
                    return;
                }
                for (final var file : FileEditorManager.getInstance(project).getOpenFiles()) {
                    if (file.getFileType() == GlslFileType.instance) {
                        final var psi = PsiManager.getInstance(project).findFile(file);
                        if (psi != null) {
                            DaemonCodeAnalyzer.getInstance(project).restart(psi, "GLSL include changed");
                        }
                    }
                }
            }, 500);
        };
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
            new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    final var file = FileDocumentManager.getInstance().getFile(event.getDocument());
                    if (file != null && IsShaderDependency(file.getName())) {
                        schedule.run();
                    }
                }
            },
            project);
        project.getMessageBus().connect(project).subscribe(VirtualFileManager.VFS_CHANGES,
                                                           new BulkFileListener() {
            @Override public void after(@NotNull List<? extends VFileEvent> events) {
                if (events.stream().anyMatch(event -> IsShaderDependency(event.getPath()))) {
                    schedule.run();
                }
            }
                                                           });
    }

    private static boolean IsShaderDependency(String name) {
        final int dot = name.lastIndexOf('.');
        final String extension =
            dot >= 0 ? name.substring(dot + 1).toLowerCase(java.util.Locale.ROOT) : "";
        return ShaderStage.stages.contains(extension) ||
            List.of("glsl", "inc", "glslinc", "h").contains(extension);
    }
}
