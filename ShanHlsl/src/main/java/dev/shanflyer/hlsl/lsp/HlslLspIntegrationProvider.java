package dev.shanflyer.hlsl.lsp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.lsp.api.LspClient;
import com.intellij.platform.lsp.api.LspIntegrationProvider;
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem;
import dev.shanflyer.hlsl.language.HlslFileType;
import dev.shanflyer.hlsl.settings.HlslConfigurable;
import dev.shanflyer.hlsl.settings.HlslSettings;
import org.jetbrains.annotations.NotNull;

public final class HlslLspIntegrationProvider implements LspIntegrationProvider {
    @Override
    public void fileOpened(@NotNull Project project, @NotNull VirtualFile file,
                           @NotNull LspClientStarter starter) {
        if (HlslSettings.GetInstance().Snapshot().enabled &&
            file.getFileType() == HlslFileType.instance) {
            starter.ensureClientStarted(new HlslLspDescriptor(project));
        }
    }

    @Override
    public LspClientWidgetItem createWidgetItem(@NotNull LspClient client, VirtualFile file) {
        return new LspClientWidgetItem(client, file, HlslFileType.instance.getIcon(),
                                       HlslConfigurable.class);
    }
}
