package dev.shanflyer.glsl.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public final class GlslFileType extends LanguageFileType {
    public static final GlslFileType instance = new GlslFileType();

    private GlslFileType() {
        super(GlslLanguage.instance);
    }

    @Override
    public @NotNull String getName() {
        return "Shan GLSL";
    }
    @Override
    public @NotNull String getDescription() {
        return "GLSL shader";
    }
    @Override
    public @NotNull String getDefaultExtension() {
        return "glsl";
    }
    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/glsl.svg", GlslFileType.class);
    }
}
