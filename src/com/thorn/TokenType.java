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
    RETURN, TRUE, WHILE, THIS,
    IMPORT, EXPORT, FROM, IN,
    MATCH, UNDERSCORE,  // _ for pattern matching default

    // Thorn-specific tokens
    DOLLAR,          // $ for functions
    AT,              // @ for annotations
    IMMUT,           // immutable keyword

    EOF
}