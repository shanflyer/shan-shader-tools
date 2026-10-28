package dev.shanflyer.hlsl.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;

public final class HlslFileType extends LanguageFileType {
    public static final HlslFileType instance = new HlslFileType();

    private HlslFileType() {
        super(HlslLanguage.instance);
    }

    @Override
    public @NotNull String getName() {
        return "Shan HLSL";
    }
    @Override
    public @NotNull String getDescription() {
        return "HLSL shader";
    }
    @Override
    public @NotNull String getDefaultExtension() {
        return "hlsl";
    }
    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/hlsl.svg", HlslFileType.class);
    }
}
