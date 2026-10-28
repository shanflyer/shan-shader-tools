package dev.shanflyer.hlsl.language;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public final class HlslParserDefinition implements ParserDefinition {
    private static final IFileElementType fileNodeType_ =
        new IFileElementType(HlslLanguage.instance);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new HlslLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        // Syntax/type checking belongs to HlslTools language server. Keep incomplete shader text
        // editable.
        return (root, builder) -> {
            final var marker = builder.mark();
            while (!builder.eof()) {
                builder.advanceLexer();
            }
            marker.done(root);
            return builder.getTreeBuilt();
        };
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return fileNodeType_;
    }
    @Override
    public @NotNull TokenSet getCommentTokens() {
        return TokenSet.create(HlslTokens.lineComment, HlslTokens.blockComment);
    }
    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return TokenSet.create(HlslTokens.string);
    }
    @Override
    public @NotNull PsiElement createElement(ASTNode node) {
        return new ASTWrapperPsiElement(node);
    }
    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider provider) {
        return new HlslFile(provider);
    }
}
