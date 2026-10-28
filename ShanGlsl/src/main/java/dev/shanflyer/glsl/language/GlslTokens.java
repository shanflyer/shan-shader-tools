package dev.shanflyer.glsl.language;

import com.intellij.psi.tree.IElementType;

public final class GlslTokens {
    public static final IElementType identifier = NewType("IDENTIFIER");
    public static final IElementType keyword = NewType("KEYWORD");
    public static final IElementType function = NewType("FUNCTION");
    public static final IElementType variable = NewType("VARIABLE");
    public static final IElementType number = NewType("NUMBER");
    public static final IElementType string = NewType("STRING");
    public static final IElementType lineComment = NewType("LINE_COMMENT");
    public static final IElementType blockComment = NewType("BLOCK_COMMENT");
    public static final IElementType preprocessor = NewType("PREPROCESSOR");
    public static final IElementType operator = NewType("OPERATOR");
    public static final IElementType lBrace = NewType("{");
    public static final IElementType rBrace = NewType("}");
    public static final IElementType lParen = NewType("(");
    public static final IElementType rParen = NewType(")");
    public static final IElementType lBracket = NewType("[");
    public static final IElementType rBracket = NewType("]");

    private GlslTokens() {
    }

    private static IElementType NewType(String name) {
        return new IElementType(name, GlslLanguage.instance);
    }
}
