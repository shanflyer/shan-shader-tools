package dev.shanflyer.hlsl.editor;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import dev.shanflyer.hlsl.language.HlslLexer;
import dev.shanflyer.hlsl.language.HlslTokens;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public final class HlslHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey keyword =
        Key("KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey function =
        Key("FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_CALL);
    public static final TextAttributesKey type =
        Key("TYPE", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey field =
        Key("FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey parameter =
        Key("PARAMETER", DefaultLanguageHighlighterColors.PARAMETER);
    public static final TextAttributesKey variable =
        Key("VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    public static final TextAttributesKey semantic =
        Key("SEMANTIC", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey macro =
        Key("MACRO", DefaultLanguageHighlighterColors.CONSTANT);
    public static final TextAttributesKey excluded =
        Key("EXCLUDED", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    public static final TextAttributesKey comment =
        Key("COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey number =
        Key("NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey string =
        Key("STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey preprocessor =
        Key("PREPROCESSOR", DefaultLanguageHighlighterColors.METADATA);
    public static final TextAttributesKey operator =
        Key("OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey braces =
        Key("BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey identifier =
        Key("IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    private static final Map<IElementType, TextAttributesKey> tokenColors_ = Map.ofEntries(
        Map.entry(HlslTokens.keyword, keyword), Map.entry(HlslTokens.function, function),
        Map.entry(HlslTokens.variable, semantic), Map.entry(HlslTokens.identifier, identifier),
        Map.entry(HlslTokens.lineComment, comment), Map.entry(HlslTokens.blockComment, comment),
        Map.entry(HlslTokens.number, number), Map.entry(HlslTokens.string, string),
        Map.entry(HlslTokens.preprocessor, preprocessor), Map.entry(HlslTokens.operator, operator),
        Map.entry(HlslTokens.lBrace, braces), Map.entry(HlslTokens.rBrace, braces),
        Map.entry(HlslTokens.lParen, braces), Map.entry(HlslTokens.rParen, braces),
        Map.entry(HlslTokens.lBracket, braces), Map.entry(HlslTokens.rBracket, braces),
        Map.entry(TokenType.BAD_CHARACTER, HighlighterColors.BAD_CHARACTER));

    private static TextAttributesKey Key(String name, TextAttributesKey fallback) {
        return TextAttributesKey.createTextAttributesKey("SHAN_HLSL_" + name, fallback);
    }
    public static TextAttributesKey Classification(String name) {
        return switch (name) {
            case "excluded code" -> excluded;
            case "Hlsl.Function", "Hlsl.Method" -> function;
            case "Hlsl.Class", "Hlsl.Struct", "Hlsl.Interface", "Hlsl.ConstantBuffer" -> type;
            case "Hlsl.Field", "Hlsl.ConstantBufferVariable" -> field;
            case "Hlsl.Parameter" -> parameter;
            case "Hlsl.LocalVariable", "Hlsl.GlobalVariable" -> variable;
            case "Hlsl.Semantic", "Hlsl.RegisterLocation", "Hlsl.PackOffset" -> semantic;
            case "Hlsl.Macro" -> macro;
            default -> null;
        };
    }
    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new HlslLexer();
    }
    @Override
    public TextAttributesKey @NotNull[] getTokenHighlights(IElementType tokenType) {
        return pack(tokenColors_.get(tokenType));
    }
}
