; Function definitions
(function_declaration
  "$" @keyword
  name: (identifier) @function)

; Function calls
(call_expression
  function: (identifier) @function.call)

; Class definitions
(class_declaration
  "class" @keyword
  name: (identifier) @type)

; Keywords
[
  "if"
  "else"
  "while"
  "for"
  "in"
  "return"
  "import"
  "export"
  "from"
  "class"
  "this"
] @keyword

; Operators
[
  "="
  "+="
  "-="
  "*="
  "/="
  "%="
  "+"
  "-"
  "*"
  "/"
  "%"
  "**"
  "=="
  "!="
  "<"
  "<="
  ">"
  ">="
  "&&"
  "||"
  "!"
  "??"
] @operator

; Punctuation
[
  "("
  ")"
  "{"
  "}"
  "["
  "]"
  ";"
  ","
  "."
  ":"
] @punctuation.delimiter

; Special symbols
"$" @punctuation.special
"@immut" @attribute

; Literals
(number_literal) @number
(string_literal) @string
(boolean_literal) @boolean
(null_literal) @constant.builtin

; Comments
(comment) @comment

; Identifiers
(identifier) @variable

; Member access
(member_expression
  property: (identifier) @property)

; This expression
(this_expression) @variable.builtin