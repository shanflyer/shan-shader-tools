package dev.shanflyer.hlsl.language;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public final class HlslLexer extends LexerBase {
    private static final Pattern numberPattern_ =
        Pattern.compile("(?:0[xX][0-9a-fA-F]+[uUlL]*|(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-"
                        + "]?[0-9]+)?[fFlLuU]*)");
    private CharSequence buffer_ = "";
    private int end_ = 0;
    private int start_ = 0;
    private int tokenEnd_ = 0;
    private int state_ = 0;
    private int nextState_ = 0;
    private IElementType token_ = null;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset,
                      int initialState) {
        buffer_ = buffer;
        end_ = endOffset;
        tokenEnd_ = startOffset;
        nextState_ = initialState;
        advance();
    }

    @Override
    public int getState() {
        return state_;
    }
    @Override
    public IElementType getTokenType() {
        return token_;
    }
    @Override
    public int getTokenStart() {
        return start_;
    }
    @Override
    public int getTokenEnd() {
        return tokenEnd_;
    }
    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer_;
    }
    @Override
    public int getBufferEnd() {
        return end_;
    }

    @Override
    public void advance() {
        start_ = tokenEnd_;
        state_ = nextState_;
        if (start_ >= end_) {
            token_ = null;
            return;
        }
        int position = start_;
        final char current = buffer_.charAt(position++);
        if (state_ == 1 || (current == '/' && At(position, '*'))) {
            position = state_ == 1 ? start_ : start_ + 2;
            while (position + 1 < end_ && !(At(position, '*') && At(position + 1, '/'))) {
                ++position;
            }
            final boolean closed = position + 1 < end_;
            nextState_ = closed ? 0 : 1;
            tokenEnd_ = closed ? position + 2 : end_;
            token_ = HlslTokens.blockComment;
            return;
        }
        nextState_ = 0;
        if (Character.isWhitespace(current)) {
            while (position < end_ && Character.isWhitespace(buffer_.charAt(position))) {
                ++position;
            }
            token_ = TokenType.WHITE_SPACE;
        } else if (current == '/' && At(position, '/')) {
            while (position < end_ && !At(position, '\n') && !At(position, '\r')) {
                ++position;
            }
            token_ = HlslTokens.lineComment;
        } else if (current == '"' || current == '\'') {
            while (position < end_ && !At(position, '\n') && !At(position, '\r')) {
                final char character = buffer_.charAt(position++);
                if (character == current) {
                    break;
                }
                if (character == '\\' && position < end_) {
                    ++position;
                }
            }
            token_ = HlslTokens.string;
        } else if (current == '#') {
            while (At(position, ' ') || At(position, '\t')) {
                ++position;
            }
            while (position < end_ && IsIdentifierPart(buffer_.charAt(position))) {
                ++position;
            }
            token_ = HlslTokens.preprocessor;
        } else if (Character.isDigit(current) || (current == '.' && position < end_ &&
                                                  Character.isDigit(buffer_.charAt(position)))) {
            final Matcher matcher = numberPattern_.matcher(buffer_).region(start_, end_);
            position = matcher.lookingAt() ? matcher.end() : position;
            token_ = HlslTokens.number;
        } else if (IsIdentifierStart(current)) {
            while (position < end_ && IsIdentifierPart(buffer_.charAt(position))) {
                ++position;
            }
            final String word = buffer_.subSequence(start_, position).toString();
            token_ = HlslVocabulary.functions.contains(word)  ? HlslTokens.function
                     : HlslVocabulary.keywords.contains(word) ? HlslTokens.keyword
                     : HlslVocabulary.IsSemantic(word)        ? HlslTokens.variable
                                                              : HlslTokens.identifier;
        } else {
            token_ = switch (current) {
                case '{' -> HlslTokens.lBrace;
                case '}' -> HlslTokens.rBrace;
                case '(' -> HlslTokens.lParen;
                case ')' -> HlslTokens.rParen;
                case '[' -> HlslTokens.lBracket;
                case ']' -> HlslTokens.rBracket;
                default ->
                    "+-*/%=!<>&|^~?:;,.".indexOf(current) >= 0 ? HlslTokens.operator
                                                                 : TokenType.BAD_CHARACTER;
            };
        }
        tokenEnd_ = position;
    }

    private boolean At(int offset, char character) {
        return offset < end_ && buffer_.charAt(offset) == character;
    }

    public static boolean IsIdentifierStart(char character) {
        return character == '_' || character >= 'a' && character <= 'z' ||
            character >= 'A' && character <= 'Z';
    }

    public static boolean IsIdentifierPart(char character) {
        return IsIdentifierStart(character) || character >= '0' && character <= '9';
    }
}
