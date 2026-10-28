package dev.shanflyer.glsl.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import dev.shanflyer.glsl.diagnostics.CompilerLocator;
import dev.shanflyer.glsl.diagnostics.CompilerOptions;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import org.jetbrains.annotations.NotNull;

public final class GlslConfigurable implements SearchableConfigurable {
    private JPanel panel_ = null;
    private JCheckBox enabled_ = null;
    private TextFieldWithBrowseButton compiler_ = null;
    private JComboBox<String> target_ = null;
    private JTextField arguments_ = null;
    private JComboBox<String> fallback_ = null;
    private JTextArea includes_ = null;
    private JTextArea userOne_ = null;
    private JTextArea userTwo_ = null;
    private JSpinner timeout_ = null;

    @Override
    public @NotNull String getId() {
        return "dev.shanflyer.glsl.settings";
    }
    @Override
    public String getDisplayName() {
        return "Shan GLSL";
    }

    @Override
    public JComponent createComponent() {
        panel_ = new JPanel(new GridBagLayout());
        enabled_ = new JCheckBox("Live compiler diagnostics (unsaved editor content)");
        compiler_ = new TextFieldWithBrowseButton();
        compiler_.addBrowseFolderListener(
            null, FileChooserDescriptorFactory.singleFile().withTitle(
                      "Select GLSL Compiler"));
        target_ = new JComboBox<>(new String[] {"Vulkan", "OpenGL", "Custom"});
        arguments_ = new JTextField();
        fallback_ = new JComboBox<>(new String[] {"frag", "vert", "comp", "geom", "tesc", "tese",
                                                  "mesh", "task", "rgen", "rint", "rmiss", "rahit",
                                                  "rchit", "rcall"});
        includes_ = new JTextArea(3, 50);
        userOne_ = new JTextArea(2, 50);
        userTwo_ = new JTextArea(2, 50);
        timeout_ = new JSpinner(new SpinnerNumberModel(5000, 250, 60000, 250));
        int row = 0;
        Row(row++, "", enabled_);
        Row(row++, "Compiler", compiler_);
        final var detected = CompilerLocator.Find("");
        Row(row++, "",
            new JLabel("<html>Blank = auto-detect VULKAN_SDK / PATH.<br>Detected: " +
                       Escape(detected == null ? "not found" : detected.toString()) + "</html>"));
        Row(row++, "Target", target_);
        Row(row++, "Extra arguments", arguments_);
        Row(row++, "",
            new JLabel(
                "<html>Vulkan: -V --target-env vulkan1.3. OpenGL: source validation.<br>"
                + "Custom: compiler receives file first unless {file} is supplied.<br>"
                + "Placeholders: {file}, {stage}, {output}. Paths may use %VULKAN_SDK%.</html>"));
        Row(row++, "Include directories", new JScrollPane(includes_));
        Row(row++, "",
            new JLabel("One directory per line; relative paths use the project directory."));
        Row(row++, "Unknown .glsl stage", fallback_);
        Row(row++, "Timeout (ms)", timeout_);
        Row(row++, "UserKeyword1", new JScrollPane(userOne_));
        Row(row++, "UserKeyword2", new JScrollPane(userTwo_));
        Row(row++, "",
            new JLabel("<html>Separate keywords with spaces, commas, or newlines.<br>"
                       + "Colors: Editor | Color Scheme | Shan GLSL.<br>"
                       + "Extensions: Editor | File Types | Shan GLSL.</html>"));
        final GridBagConstraints filler = new GridBagConstraints();
        filler.gridy = row;
        filler.weighty = 1;
        panel_.add(new JPanel(), filler);
        final JPanel outer = new JPanel(new BorderLayout());
        outer.add(panel_, BorderLayout.NORTH);
        reset();
        return outer;
    }

    private static String Escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void Row(int row, String label, JComponent component) {
        final GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = row;
        constraints.gridx = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.insets = new Insets(5, 0, 5, 12);
        panel_.add(new JLabel(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel_.add(component, constraints);
    }

    private CompilerOptions Read() {
        final CompilerOptions state = new CompilerOptions();
        state.enabled = enabled_.isSelected();
        state.compilerPath = compiler_.getText().trim();
        state.target = Objects.toString(target_.getSelectedItem());
        state.arguments = arguments_.getText().trim();
        state.includeDirectories = includes_.getText().trim();
        state.fallbackStage = Objects.toString(fallback_.getSelectedItem());
        state.timeoutMillis = ((Number)timeout_.getValue()).intValue();
        state.userKeywords1 = userOne_.getText().trim();
        state.userKeywords2 = userTwo_.getText().trim();
        return state;
    }

    @Override
    public boolean isModified() {
        if (panel_ == null) {
            return false;
        }
        final var left = Read();
        final var right = GlslSettings.GetInstance().Snapshot();
        return left.enabled != right.enabled || left.timeoutMillis != right.timeoutMillis ||
            !left.compilerPath.equals(right.compilerPath) || !left.target.equals(right.target) ||
            !left.arguments.equals(right.arguments) ||
            !left.includeDirectories.equals(right.includeDirectories) ||
            !left.fallbackStage.equals(right.fallbackStage) ||
            !left.userKeywords1.equals(right.userKeywords1) ||
            !left.userKeywords2.equals(right.userKeywords2);
    }

    @Override
    public void apply() throws ConfigurationException {
        final var state = Read();
        if (state.enabled && !state.compilerPath.isBlank() &&
            CompilerLocator.Find(state.compilerPath) == null) {
            throw new ConfigurationException("Compiler executable does not exist: " +
                                             state.compilerPath);
        }
        try {
            dev.shanflyer.glsl.diagnostics.CommandArguments.Split(state.arguments);
        } catch (IllegalArgumentException exception) {
            throw new ConfigurationException(exception.getMessage());
        }
        GlslSettings.GetInstance().loadState(state);
        for (final var project : ProjectManager.getInstance().getOpenProjects()) {
            DaemonCodeAnalyzer.getInstance(project).restart("Shan GLSL settings changed");
        }
    }

    @Override
    public void reset() {
        if (panel_ == null) {
            return;
        }
        final var state = GlslSettings.GetInstance().Snapshot();
        enabled_.setSelected(state.enabled);
        compiler_.setText(state.compilerPath);
        target_.setSelectedItem(state.target);
        arguments_.setText(state.arguments);
        includes_.setText(state.includeDirectories);
        fallback_.setSelectedItem(state.fallbackStage);
        timeout_.setValue(state.timeoutMillis);
        userOne_.setText(state.userKeywords1);
        userTwo_.setText(state.userKeywords2);
    }

    @Override
    public void disposeUIResources() {
        if (compiler_ != null) {
            compiler_.dispose();
        }
        panel_ = null;
        enabled_ = null;
        compiler_ = null;
        target_ = null;
        arguments_ = null;
        fallback_ = null;
        includes_ = null;
        userOne_ = null;
        userTwo_ = null;
        timeout_ = null;
    }
}
