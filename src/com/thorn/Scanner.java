package com.thorn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thorn.TokenType.*;

class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("if", IF);
        keywords.put("null", NULL);
        keywords.put("or", OR);
        keywords.put("return", RETURN);
        keywords.put("throw", THROW);
        keywords.put("true", TRUE);
        keywords.put("while", WHILE);
        keywords.put("this", THIS);
        keywords.put("import", IMPORT);
        keywords.put("export", EXPORT);
        keywords.put("from", FROM);
        keywords.put("in", IN);
        keywords.put("match", MATCH);
        keywords.put("immut", IMMUT);
        keywords.put("try", TRY);
        keywords.put("catch", CATCH);
        
        // Type system keywords
        keywords.put("string", STRING_TYPE);
        keywords.put("number", NUMBER_TYPE);
        keywords.put("boolean", BOOLEAN_TYPE);
        keywords.put("Any", ANY_TYPE);
        keywords.put("void", VOID_TYPE);
        keywords.put("Array", ARRAY_TYPE);
        keywords.put("Function", FUNCTION_TYPE);
        keywords.put("Dict", DICT_TYPE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case '[': addToken(LEFT_BRACKET); break;
            case ']': addToken(RIGHT_BRACKET); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-':
                if (match('=')) {
                    addToken(MINUS_EQUAL);
                } else {
                    addToken(MINUS);
                }
                break;
            case '+':
                if (match('=')) {
                    addToken(PLUS_EQUAL);
                } else {
                    addToken(PLUS);
                }
                break;
            case ';': addToken(SEMICOLON); break;
            case ':': addToken(COLON); break;
            case '*':
                if (match('*')) {
                    addToken(STAR_STAR);
                } else if (match('=')) {
                    addToken(STAR_EQUAL);
                } else {
                    addToken(STAR);
                }
                break;
            case '%':
                if (match('=')) {
                    addToken(PERCENT_EQUAL);
                } else {
                    addToken(PERCENT);
                }
                break;
            case '$': addToken(DOLLAR); break;
            case '@': addToken(AT); break;
            case '_': 
                if (isAlphaNumeric(peek())) {
                    identifier();
                } else {
                    addToken(UNDERSCORE);
                }
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                if (match('=')) {
                    addToken(EQUAL_EQUAL);
                } else if (match('>')) {
                    addToken(ARROW);
                } else {
                    addToken(EQUAL);
                }
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '?':
                if (match('?')) {
                    addToken(QUESTION_QUESTION);
                }
                break;
            case '&':
                if (match('&')) {
                    addToken(AND_AND);
                }
                break;
            case '|':
                if (match('|')) {
                    addToken(OR_OR);
                }
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // Multi-line comment
                    blockComment();
                } else if (match('=')) {
                    addToken(SLASH_EQUAL);
                } else {
                    addToken(SLASH);
                }
                break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;
            case '\n':
                line++;
                break;
            case '"': string(); break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Thorn.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                // Found the end of the comment
                advance(); // consume *
                advance(); // consume /
                return;
            }
            if (peek() == '\n') line++;
            advance();
        }
        Thorn.error(line, "Unterminated block comment.");
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        StringBuilder value = new StringBuilder();
        
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\\') {
                // Handle escape sequences
                advance(); // consume the backslash
                if (isAtEnd()) {
                    Thorn.error(line, "Unterminated string escape sequence.");
                    return;
                }
                
                char escaped = advance();
                switch (escaped) {
                    case 'n':
                        value.append('\n');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    case 'r':
                        value.append('\r');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    case '"':
                        value.append('"');
                        break;
                    case '\'':
                        value.append('\'');
                        break;
                    case '0':
                        value.append('\0');
                        break;
                    default:
                        // For unknown escape sequences, just include the character
                        Thorn.error(line, "Unknown escape sequence: \\" + escaped);
                        value.append(escaped);
                        break;
                }
            } else {
                if (peek() == '\n') line++;
                value.append(advance());
            }
        }

        if (isAtEnd()) {
            Thorn.error(line, "Unterminated string.");
            return;
        }

        // The closing "
        advance();

        addToken(STRING, value.toString());
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}