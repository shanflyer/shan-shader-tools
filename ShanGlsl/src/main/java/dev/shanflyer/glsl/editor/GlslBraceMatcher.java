package dev.shanflyer.glsl.editor;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.shanflyer.glsl.language.GlslTokens;
import org.jetbrains.annotations.NotNull;

public final class GlslBraceMatcher implements PairedBraceMatcher {
    @Override
    public BracePair @NotNull[] getPairs() {
        return new BracePair[] {new BracePair(GlslTokens.lBrace, GlslTokens.rBrace, true),
                                new BracePair(GlslTokens.lParen, GlslTokens.rParen, false),
                                new BracePair(GlslTokens.lBracket, GlslTokens.rBracket, false)};
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
