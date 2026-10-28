package dev.shanflyer.hlsl.editor;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import dev.shanflyer.hlsl.language.HlslLexer;
import dev.shanflyer.hlsl.language.HlslTokens;
import java.util.ArrayDeque;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public final class HlslFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    @Override
    public FoldingDescriptor @NotNull[]
    buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        final var regions = new ArrayList<FoldingDescriptor>();
        final var braces = new ArrayDeque<Integer>();
        final var directives = new ArrayDeque<Integer>();
        final HlslLexer lexer = new HlslLexer();
        lexer.start(document.getCharsSequence());
        while (lexer.getTokenType() != null) {
            ProgressManager.checkCanceled();
            final var type = lexer.getTokenType();
            if (type == HlslTokens.lBrace) {
                braces.push(lexer.getTokenStart());
            } else if (type == HlslTokens.rBrace && !braces.isEmpty()) {
                Add(regions, root, document, braces.pop(), lexer.getTokenEnd(), "{...}");
            } else if (type == HlslTokens.blockComment) {
                Add(regions, root, document, lexer.getTokenStart(), lexer.getTokenEnd(), "/*...*/");
            } else if (type == HlslTokens.preprocessor) {
                final String directive = lexer.getTokenText().replaceAll("\\s", "");
                if (directive.equals("#if") || directive.equals("#ifdef") ||
                    directive.equals("#ifndef")) {
                    directives.push(lexer.getTokenStart());
                } else if (directive.equals("#endif") && !directives.isEmpty()) {
                    Add(regions, root, document, directives.pop(), lexer.getTokenEnd(),
                        "#if ... #endif");
                }
            }
            lexer.advance();
        }
        return regions.toArray(FoldingDescriptor[] ::new);
    }

    private static void Add(ArrayList<FoldingDescriptor> regions, PsiElement root,
                            Document document, int start, int end, String placeholder) {
        if (end > start + 1 && document.getLineNumber(start) < document.getLineNumber(end - 1)) {
            regions.add(new FoldingDescriptor(root.getNode(), new TextRange(start, end), null,
                                              placeholder));
        }
    }
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "...";
    }
    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
