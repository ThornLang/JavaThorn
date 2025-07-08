; Function definitions
(function_declaration
  name: (identifier) @local.definition.function)

; Function parameters
(parameter_list
  (identifier) @local.definition.parameter)

; Variable declarations
(variable_declaration
  name: (identifier) @local.definition.variable)

; Class definitions
(class_declaration
  name: (identifier) @local.definition.type)

; For loop variables
(for_statement
  variable: (identifier) @local.definition.variable)

; Scopes
(function_declaration
  body: (block) @local.scope)

(class_declaration
  body: (class_body) @local.scope)

(block) @local.scope