package dev.shanflyer.hlsl.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.shanflyer.hlsl.language.HlslTokens;
import org.jetbrains.annotations.NotNull;

public final class HlslBraceMatcher implements PairedBraceMatcher {
    @Override
    public BracePair @NotNull[] getPairs() {
        return new BracePair[] {new BracePair(HlslTokens.lBrace, HlslTokens.rBrace, true),
                                new BracePair(HlslTokens.lParen, HlslTokens.rParen, false),
                                new BracePair(HlslTokens.lBracket, HlslTokens.rBracket, false)};
    }
    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType left,
                                                   IElementType context) {
        return true;
    }
    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
