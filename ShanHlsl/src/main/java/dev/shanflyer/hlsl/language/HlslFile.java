package dev.shanflyer.hlsl.language;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;

public final class HlslFile extends PsiFileBase {
    public HlslFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, HlslLanguage.instance);
    }

    @Override
    public @NotNull FileType getFileType() {
        return HlslFileType.instance;
    }
    @Override
    public String toString() {
        return "Shan HLSL file";
    }
}
