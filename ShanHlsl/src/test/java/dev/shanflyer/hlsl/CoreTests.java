package dev.shanflyer.hlsl;

import com.intellij.psi.TokenType;
import dev.shanflyer.hlsl.language.HlslLexer;
import dev.shanflyer.hlsl.language.HlslTokens;
import dev.shanflyer.hlsl.language.HlslVocabulary;
import dev.shanflyer.hlsl.editor.HlslHighlighter;
import java.util.ArrayList;

public final class CoreTests {
    private static int checks_ = 0;
    private static void Check(boolean result, String message) {
        ++checks_;
        if (!result) {
            throw new AssertionError(message);
        }
    }
    public static void main(String[] arguments) {
        Check(HlslVocabulary.keywords.contains("float4"), "Vector types");
        Check(HlslVocabulary.keywords.contains("Texture2D"), "Texture types");
        Check(HlslVocabulary.functions.contains("saturate"), "Intrinsic functions");
        Check(HlslVocabulary.IsSemantic("SV_Position"), "System semantics");
        Check(HlslVocabulary.IsSemantic("TEXCOORD3"), "Numbered semantics");
        Check(!HlslVocabulary.IsSemantic("positionOffset"), "Ordinary identifiers");
        final String source = "#include \"lighting.hlsli\"\n/* comment */ float4 PSMain(float2 " +
                              "uv : TEXCOORD0) : SV_Target { return float4(1e-2+2.0,0,0,1); }";
        final HlslLexer lexer = new HlslLexer();
        final var tokens = new ArrayList<String>();
        lexer.start(source);
        int offset = 0;
        while (lexer.getTokenType() != null) {
            Check(lexer.getTokenStart() == offset && lexer.getTokenEnd() > offset,
                  "Lexer progresses");
            Check(lexer.getTokenType() != TokenType.BAD_CHARACTER, "Valid HLSL character");
            tokens.add(lexer.getTokenType() + ":" + lexer.getTokenText());
            offset = lexer.getTokenEnd();
            lexer.advance();
        }
        Check(offset == source.length(), "Lexer covers file");
        Check(tokens.contains("NUMBER:1e-2") && tokens.contains("OPERATOR:+"),
              "Number/operator boundary");
        lexer.start("/*/ float4");
        Check(lexer.getTokenType() == HlslTokens.blockComment && lexer.getTokenEnd() == 10,
              "Unterminated block comment");
        Check(HlslHighlighter.Classification("excluded code") == HlslHighlighter.excluded,
              "Inactive code colors");
        Check(HlslHighlighter.Classification("Hlsl.Function") == HlslHighlighter.function,
              "Semantic function colors");
        System.out.println("PASS: " + checks_ + " frontend checks");
    }
}
