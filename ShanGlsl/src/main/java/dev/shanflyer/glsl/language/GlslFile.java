package dev.shanflyer.glsl.language;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public final class GlslFile extends PsiFileBase {
    public GlslFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, GlslLanguage.instance);
    }

    @Override
    public @NotNull FileType getFileType() {
        return GlslFileType.instance;
    }
    @Override
    public String toString() {
        return "Shan GLSL file";
    }
}
