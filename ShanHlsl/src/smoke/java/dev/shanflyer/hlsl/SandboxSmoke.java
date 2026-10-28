package dev.shanflyer.hlsl;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationStarter;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiDocumentManager;
import dev.shanflyer.hlsl.editor.HlslFoldingBuilder;
import dev.shanflyer.hlsl.editor.HlslHighlighter;
import dev.shanflyer.hlsl.language.HlslFile;
import dev.shanflyer.hlsl.language.HlslFileType;
import dev.shanflyer.hlsl.language.HlslLanguage;
import dev.shanflyer.hlsl.lsp.HlslLspDescriptor;
import dev.shanflyer.hlsl.settings.HlslConfigurable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        int result = 0;
        try {
            Check(PluginManagerCore.isLoaded(PluginId.getId("dev.shanflyer.hlsl")),
                  "Plugin loaded in CLion");
            Check(Files.isRegularFile(HlslLspDescriptor.BundledServer()),
                  "Bundled language server packaged");
            ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.run(() -> {
                for (final String extension : List.of("hlsl", "hlsli", "fx", "fxh")) {
                    Check(FileTypeManager.getInstance().getFileTypeByFileName(
                              "test." + extension) == HlslFileType.instance,
                          "File association ." + extension);
                }
                final var project = ProjectManager.getInstance().getDefaultProject();
                final var file = PsiFileFactory.getInstance(project).createFileFromText(
                    "test.hlsl", HlslFileType.instance,
                    "float4 PSMain() : SV_Target {\nreturn float4(1,0,0,1);\n}\n", 0, true);
                Check(file instanceof HlslFile, "Registered parser");
                Check(SyntaxHighlighterFactory.getSyntaxHighlighter(HlslLanguage.instance, project,
                                                                    null)
                              instanceof HlslHighlighter,
                      "Registered highlighter");
                Check(new HlslFoldingBuilder()
                              .buildFoldRegions(
                                  file, PsiDocumentManager.getInstance(project).getDocument(file),
                                  false)
                              .length == 1,
                      "Function folding");
                final var settings = new HlslConfigurable();
                Check(settings.createComponent() != null && !settings.isModified(), "Settings UI");
                settings.disposeUIResources();
                try {
                    final var descriptor = new HlslLspDescriptor(project);
                    Check(descriptor.createCommandLine().getExePath().endsWith(
                              "ShaderTools.LanguageServer.exe"),
                          "LSP process command");
                    Check(descriptor.getLanguageId(file.getVirtualFile()).equals("hlsl"),
                          "LSP language id mapping");
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }));
            report_.append("PASS: ").append(checks_).append(" CLion integration checks\n");
        } catch (Throwable exception) {
            result = 1;
            final var text = new java.io.StringWriter();
            exception.printStackTrace(new java.io.PrintWriter(text));
            report_.append("FAIL\n").append(text);
        }
        try {
            Files.writeString(Path.of(System.getProperty("shan.hlsl.smoke.report")), report_);
        } catch (Exception exception) {
            result = 1;
            exception.printStackTrace();
        }
        System.out.print(report_);
        System.exit(result);
    }
    private void Check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        ++checks_;
        report_.append("OK ").append(message).append('\n');
    }
}
