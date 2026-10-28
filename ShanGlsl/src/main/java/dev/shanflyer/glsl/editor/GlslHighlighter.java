package dev.shanflyer.glsl.editor;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import dev.shanflyer.glsl.language.GlslLexer;
import dev.shanflyer.glsl.language.GlslTokens;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class GlslHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey keyword = Fallback("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey function = Fallback("FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public static final TextAttributesKey variable = Fallback("VARIABLE", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey userOne = Fallback("USER_ONE", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey userTwo = Fallback("USER_TWO", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey comment =
        Fallback("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey number =
        Fallback("NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey string =
        Fallback("STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey preprocessor =
        Fallback("PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey operator =
        Fallback("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey braces =
        Fallback("BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey identifier =
        Fallback("IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    private static final Map<IElementType, TextAttributesKey> tokenColors_ = Map.ofEntries(
        Map.entry(GlslTokens.keyword, keyword), Map.entry(GlslTokens.function, function),
        Map.entry(GlslTokens.variable, variable), Map.entry(GlslTokens.identifier, identifier),
        Map.entry(GlslTokens.lineComment, comment), Map.entry(GlslTokens.blockComment, comment),
        Map.entry(GlslTokens.number, number), Map.entry(GlslTokens.string, string),
        Map.entry(GlslTokens.preprocessor, preprocessor), Map.entry(GlslTokens.operator, operator),
        Map.entry(GlslTokens.lBrace, braces), Map.entry(GlslTokens.rBrace, braces),
        Map.entry(GlslTokens.lParen, braces), Map.entry(GlslTokens.rParen, braces),
        Map.entry(GlslTokens.lBracket, braces), Map.entry(GlslTokens.rBracket, braces),
        Map.entry(TokenType.BAD_CHARACTER, HighlighterColors.BAD_CHARACTER));

    private static TextAttributesKey Fallback(String name, TextAttributesKey fallback) {
        return TextAttributesKey.createTextAttributesKey("SHAN_GLSL_" + name, fallback);
    }

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new GlslLexer();
    }

    @Override
    public TextAttributesKey @NotNull[] getTokenHighlights(IElementType type) {
        return pack(tokenColors_.get(type));
    }
}
