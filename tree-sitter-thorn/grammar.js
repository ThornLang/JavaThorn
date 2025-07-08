module.exports = grammar({
  name: 'thorn',

  extras: $ => [
    /\s/,
    $.comment,
  ],

  rules: {
    source_file: $ => repeat($._statement),

    _statement: $ => choice(
      $.function_declaration,
      $.class_declaration,
      $.variable_declaration,
      $.expression_statement,
      $.if_statement,
      $.while_statement,
      $.for_statement,
      $.return_statement,
      $.import_statement,
      $.export_statement,
      $.block,
    ),

    // Function declaration with $ prefix
    function_declaration: $ => seq(
      '$',
      field('name', $.identifier),
      field('parameters', $.parameter_list),
      field('body', $.block),
    ),

    parameter_list: $ => seq(
      '(',
      optional(seq(
        $.identifier,
        repeat(seq(',', $.identifier)),
      )),
      ')',
    ),

    // Class declaration
    class_declaration: $ => seq(
      'class',
      field('name', $.identifier),
      field('body', $.class_body),
    ),

    class_body: $ => seq(
      '{',
      repeat($._class_member),
      '}',
    ),

    _class_member: $ => choice(
      $.function_declaration,
      $.variable_declaration,
    ),

    // Variable declaration with optional @immut
    variable_declaration: $ => seq(
      optional('@immut'),
      field('name', $.identifier),
      '=',
      field('value', $._expression),
      ';',
    ),

    // Control flow statements
    if_statement: $ => seq(
      'if',
      '(',
      field('condition', $._expression),
      ')',
      field('then', $.block),
      optional(seq(
        'else',
        field('else', choice($.block, $.if_statement)),
      )),
    ),

    while_statement: $ => seq(
      'while',
      '(',
      field('condition', $._expression),
      ')',
      field('body', $.block),
    ),

    for_statement: $ => seq(
      'for',
      '(',
      field('variable', $.identifier),
      'in',
      field('iterable', $._expression),
      ')',
      field('body', $.block),
    ),

    return_statement: $ => seq(
      'return',
      optional(field('value', $._expression)),
      ';',
    ),

    // Import/Export statements
    import_statement: $ => choice(
      seq('import', field('path', $.string_literal), ';'),
      seq(
        'import',
        '{',
        field('imports', $.import_list),
        '}',
        'from',
        field('path', $.string_literal),
        ';',
      ),
    ),

    import_list: $ => seq(
      $.identifier,
      repeat(seq(',', $.identifier)),
    ),

    export_statement: $ => seq(
      'export',
      choice(
        $.function_declaration,
        $.class_declaration,
        $.variable_declaration,
      ),
    ),

    // Block statement
    block: $ => prec(1, seq(
      '{',
      repeat($._statement),
      '}',
    )),

    // Expression statement
    expression_statement: $ => seq(
      $._expression,
      ';',
    ),

    // Expressions with proper precedence
    _expression: $ => choice(
      $.ternary_expression,
      $.binary_expression,
      $.unary_expression,
      $.postfix_expression,
      $.primary_expression,
    ),

    ternary_expression: $ => prec.right(1, seq(
      $._expression,
      '?',
      $._expression,
      ':',
      $._expression,
    )),

    binary_expression: $ => choice(
      prec.left(2, seq($._expression, '??', $._expression)),  // null coalescing
      prec.left(3, seq($._expression, '||', $._expression)),  // logical or
      prec.left(4, seq($._expression, '&&', $._expression)),  // logical and
      prec.left(5, seq($._expression, '==', $._expression)),  // equality
      prec.left(5, seq($._expression, '!=', $._expression)),
      prec.left(6, seq($._expression, '<', $._expression)),   // comparison
      prec.left(6, seq($._expression, '<=', $._expression)),
      prec.left(6, seq($._expression, '>', $._expression)),
      prec.left(6, seq($._expression, '>=', $._expression)),
      prec.left(7, seq($._expression, '+', $._expression)),   // addition
      prec.left(7, seq($._expression, '-', $._expression)),
      prec.left(8, seq($._expression, '*', $._expression)),   // multiplication
      prec.left(8, seq($._expression, '/', $._expression)),
      prec.left(8, seq($._expression, '%', $._expression)),
      prec.left(9, seq($._expression, '**', $._expression)),  // power
    ),

    unary_expression: $ => choice(
      prec(10, seq('!', $._expression)),
      prec(10, seq('-', $._expression)),
      prec(10, seq('+', $._expression)),
    ),

    postfix_expression: $ => choice(
      $.assignment_expression,
      $.call_expression,
      $.member_expression,
      $.array_access,
    ),

    assignment_expression: $ => choice(
      prec.right(1, seq(
        field('left', choice($.identifier, $.member_expression, $.array_access)),
        '=',
        field('right', $._expression),
      )),
      prec.right(1, seq(
        field('left', $.identifier),
        '+=',
        field('right', $._expression),
      )),
      prec.right(1, seq(
        field('left', $.identifier),
        '-=',
        field('right', $._expression),
      )),
      prec.right(1, seq(
        field('left', $.identifier),
        '*=',
        field('right', $._expression),
      )),
      prec.right(1, seq(
        field('left', $.identifier),
        '/=',
        field('right', $._expression),
      )),
      prec.right(1, seq(
        field('left', $.identifier),
        '%=',
        field('right', $._expression),
      )),
    ),

    call_expression: $ => prec(11, seq(
      field('function', $._expression),
      field('arguments', $.argument_list),
    )),

    argument_list: $ => seq(
      '(',
      optional(seq(
        $._expression,
        repeat(seq(',', $._expression)),
      )),
      ')',
    ),

    member_expression: $ => prec(11, seq(
      field('object', $._expression),
      '.',
      field('property', $.identifier),
    )),

    array_access: $ => prec(11, seq(
      field('array', $._expression),
      '[',
      field('index', $._expression),
      ']',
    )),

    primary_expression: $ => choice(
      $.identifier,
      $.number_literal,
      $.string_literal,
      $.boolean_literal,
      $.null_literal,
      $.array_literal,
      $.object_literal,
      $.parenthesized_expression,
      $.this_expression,
    ),

    // Literals
    number_literal: $ => /\d+(\.\d+)?/,

    string_literal: $ => choice(
      seq('"', repeat(choice(/[^"\\]/, /\\./)), '"'),
      seq("'", repeat(choice(/[^'\\]/, /\\./)), "'"),
    ),

    boolean_literal: $ => choice('true', 'false'),

    null_literal: $ => 'null',

    array_literal: $ => seq(
      '[',
      optional(seq(
        $._expression,
        repeat(seq(',', $._expression)),
        optional(','),
      )),
      ']',
    ),

    object_literal: $ => seq(
      '{',
      optional(seq(
        $.object_pair,
        repeat(seq(',', $.object_pair)),
        optional(','),
      )),
      '}',
    ),

    object_pair: $ => seq(
      field('key', choice($.identifier, $.string_literal)),
      ':',
      field('value', $._expression),
    ),

    parenthesized_expression: $ => seq(
      '(',
      $._expression,
      ')',
    ),

    this_expression: $ => 'this',

    // Identifier
    identifier: $ => /[a-zA-Z_][a-zA-Z0-9_]*/,

    // Comments
    comment: $ => choice(
      seq('//', /.*/),
      seq('/*', /[^*]*\*+([^/*][^*]*\*+)*/, '/'),
    ),
  },
});