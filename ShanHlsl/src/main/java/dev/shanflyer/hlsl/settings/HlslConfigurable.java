package dev.shanflyer.hlsl.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.platform.lsp.api.LspClientManager;
import dev.shanflyer.hlsl.lsp.HlslLspIntegrationProvider;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.jetbrains.annotations.NotNull;

public final class HlslConfigurable implements SearchableConfigurable {
    private JPanel panel_ = null;
    private JCheckBox enabled_ = null;
    private JCheckBox semantic_ = null;
    private JTextField server_ = null;
    private JTextField arguments_ = null;

    @Override
    public @NotNull String getId() {
        return "dev.shanflyer.hlsl.settings";
    }
    @Override
    public String getDisplayName() {
        return "Shan HLSL";
    }
    @Override
    public JComponent createComponent() {
        panel_ = new JPanel(new BorderLayout(0, 16));
        final JPanel rows = new JPanel(new GridLayout(0, 1, 0, 8));
        enabled_ = new JCheckBox("Enable HlslTools language service");
        semantic_ =
            new JCheckBox("Semantic colors and inactive preprocessor regions (bundled server)");
        server_ = new JTextField();
        arguments_ = new JTextField();
        rows.add(enabled_);
        rows.add(semantic_);
        rows.add(new JLabel("Language server executable (blank = bundled Windows x64 server):"));
        rows.add(server_);
        rows.add(new JLabel("Additional server arguments:"));
        rows.add(arguments_);
        rows.add(new JLabel(
            "<html>Use shadertoolsconfig.json for preprocessor definitions, include " +
            "directories,<br>"
            + "and virtual directory mappings. Relative paths resolve from the config " +
              "location.<br><br>"
            + "Colors: Editor | Color Scheme | Shan HLSL.<br>"
            + "File extensions: Editor | File Types | Shan HLSL.<br>"
            + "Changing these settings restarts HLSL services in open projects.</html>"));
        panel_.add(rows, BorderLayout.NORTH);
        reset();
        return panel_;
    }
    @Override
    public boolean isModified() {
        if (panel_ == null) {
            return false;
        }
        final var options = HlslSettings.GetInstance().Snapshot();
        return enabled_.isSelected() != options.enabled ||
            semantic_.isSelected() != options.semanticColors ||
            !server_.getText().trim().equals(options.serverPath) ||
            !arguments_.getText().equals(options.arguments);
    }
    @Override
    public void apply() throws ConfigurationException {
        final var options = HlslSettings.GetInstance().Snapshot();
        options.enabled = enabled_.isSelected();
        options.semanticColors = semantic_.isSelected();
        options.serverPath = server_.getText().trim();
        options.arguments = arguments_.getText();
        try {
            if (!options.serverPath.isBlank() &&
                !Files.isRegularFile(Path.of(options.serverPath))) {
                throw new ConfigurationException("Language server executable does not exist");
            }
        } catch (java.nio.file.InvalidPathException exception) {
            throw new ConfigurationException("Invalid language server path");
        }
        HlslSettings.GetInstance().loadState(options);
        for (final var project : ProjectManager.getInstance().getOpenProjects()) {
            final var manager = LspClientManager.getInstance(project);
            manager.stopClients(HlslLspIntegrationProvider.class);
            if (options.enabled) {
                manager.startClientsIfNeeded(HlslLspIntegrationProvider.class);
            }
        }
    }
    @Override
    public void reset() {
        if (panel_ == null) {
            return;
        }
        final var options = HlslSettings.GetInstance().Snapshot();
        enabled_.setSelected(options.enabled);
        semantic_.setSelected(options.semanticColors);
        server_.setText(options.serverPath);
        arguments_.setText(options.arguments);
    }
    @Override
    public void disposeUIResources() {
        panel_ = null;
        enabled_ = null;
        semantic_ = null;
        server_ = null;
        arguments_ = null;
    }
}
