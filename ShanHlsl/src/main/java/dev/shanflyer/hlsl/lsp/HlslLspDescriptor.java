package dev.shanflyer.hlsl.lsp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor;
import com.intellij.util.execution.ParametersListUtil;
import dev.shanflyer.hlsl.language.HlslFileType;
import dev.shanflyer.hlsl.settings.HlslSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.eclipse.lsp4j.services.LanguageServer;
import org.jetbrains.annotations.NotNull;

public final class HlslLspDescriptor extends ProjectWideLspClientDescriptor {
    public HlslLspDescriptor(Project project) {
        super(project, "Shan HLSL / HlslTools");
    }

    @Override
    public boolean isSupportedFile(@NotNull VirtualFile file) {
        return file.getFileType() == HlslFileType.instance;
    }

    @Override
    public @NotNull String getLanguageId(@NotNull VirtualFile file) {
        return "hlsl";
    }

    public static Path BundledServer() throws ExecutionException {
        try {
            final Path jar = PathManager.getJarForClass(HlslLspDescriptor.class);
            if (jar == null) {
                throw new ExecutionException("Shan HLSL plugin JAR location is unavailable");
            }
            if (!Files.isRegularFile(jar) || jar.getParent() == null ||
                jar.getParent().getParent() == null) {
                throw new ExecutionException(
                    "Install the packaged Shan HLSL plugin to locate its bundled server");
            }
            return jar.getParent().getParent().resolve(
                "server/win-x64/ShaderTools.LanguageServer.exe");
        } catch (java.nio.file.InvalidPathException exception) {
            throw new ExecutionException("Invalid Shan HLSL plugin code location", exception);
        }
    }

    @Override
    public @NotNull GeneralCommandLine createCommandLine() throws ExecutionException {
        final var options = HlslSettings.GetInstance().Snapshot();
        final String architecture = System.getProperty("os.arch", "");
        final boolean x64 =
            architecture.equalsIgnoreCase("amd64") || architecture.equalsIgnoreCase("x86_64");
        if (options.serverPath.isBlank() && (!SystemInfo.isWindows || !x64)) {
            throw new ExecutionException("The bundled HLSL server requires Windows x64. "
                                         + "Configure an external server in Shan HLSL settings.");
        }
        final Path server;
        try {
            server = options.serverPath.isBlank() ? BundledServer() : Path.of(options.serverPath);
        } catch (java.nio.file.InvalidPathException exception) {
            throw new ExecutionException("Invalid HLSL server path", exception);
        }
        if (!Files.isRegularFile(server)) {
            throw new ExecutionException("HLSL language server not found: " + server +
                                         ". Check Languages & Frameworks | Shan HLSL.");
        }
        final var command =
            new GeneralCommandLine(server.toString()).withCharset(StandardCharsets.UTF_8);
        command.addParameters(ParametersListUtil.parse(options.arguments));
        if (!StringUtil.isEmpty(getProject().getBasePath())) {
            command.withWorkDirectory(getProject().getBasePath());
        }
        return command;
    }

    @Override
    public @NotNull Class<? extends LanguageServer> getLsp4jServerClass() {
        return HlslLanguageServer.class;
    }
}
