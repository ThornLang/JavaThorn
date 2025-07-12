package com.thorn;

public enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    PERCENT, COLON,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    ARROW,           // =>
    QUESTION_QUESTION, // ??
    PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, PERCENT_EQUAL,
    STAR_STAR,       // ** (power operator)
    AND_AND,         // &&
    OR_OR,           // ||

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FOR, IF, NULL, OR,
    RETURN, THROW, TRUE, WHILE, THIS,
    IMPORT, EXPORT, FROM, IN,
    MATCH, UNDERSCORE,  // _ for pattern matching default
    
    // Type system keywords
    STRING_TYPE, NUMBER_TYPE, BOOLEAN_TYPE, NULL_TYPE,
    ANY_TYPE, VOID_TYPE, ARRAY_TYPE, FUNCTION_TYPE, DICT_TYPE,

    // Thorn-specific tokens
    DOLLAR,          // $ for functions
    AT,              // @ for annotations
    IMMUT,           // immutable keyword

    EOF
}