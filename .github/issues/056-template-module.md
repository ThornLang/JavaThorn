# Implement Template Engine Module for ThornLang Standard Library

## Overview
Implement a template engine module (`template.thorn`) for string interpolation and template processing in ThornLang. This module will be implemented in pure Thorn to provide a flexible templating system for generating dynamic content.

## Technical Requirements

### Core Template Functions
- [ ] `render(template, data)` - Basic template rendering with data
- [ ] `renderFile(filename, data)` - Render template from file
- [ ] `compile(template)` - Compile template for reuse
- [ ] `renderCompiled(compiled, data)` - Render pre-compiled template

### Template Syntax Support
- [ ] `{{variable}}` - Variable interpolation
- [ ] `{{#if condition}}...{{/if}}` - Conditional rendering
- [ ] `{{#unless condition}}...{{/unless}}` - Negative conditional
- [ ] `{{#each array}}...{{/each}}` - Loop rendering
- [ ] `{{#with object}}...{{/with}}` - Context switching
- [ ] `{{> partial}}` - Partial template inclusion
- [ ] `{{{raw}}}` - Raw/unescaped output
- [ ] `{{! comment }}` - Template comments

### Helper Functions
- [ ] `registerHelper(name, func)` - Register custom helper
- [ ] `unregisterHelper(name)` - Remove custom helper
- [ ] `registerPartial(name, template)` - Register partial template
- [ ] `unregisterPartial(name)` - Remove partial template
- [ ] Built-in helpers: `eq`, `ne`, `lt`, `gt`, `and`, `or`, `not`

### Escaping and Safety
- [ ] `escapeHTML(str)` - HTML entity escaping
- [ ] `escapeJS(str)` - JavaScript string escaping
- [ ] `escapeURL(str)` - URL encoding
- [ ] `escapeSQL(str)` - SQL escaping
- [ ] `safeString(str)` - Mark string as safe (no escaping)
- [ ] `setEscapeFunction(func)` - Set custom escape function

### Advanced Features
- [ ] `templateCache` - Template caching system
- [ ] `clearCache()` - Clear template cache
- [ ] `precompileAll(directory)` - Precompile template directory
- [ ] `setDelimiters(open, close)` - Custom delimiter support
- [ ] `extends(parentTemplate)` - Template inheritance
- [ ] `block(name)` - Named blocks for inheritance

### Expression Evaluation
- [ ] `evaluateExpression(expr, context)` - Evaluate template expressions
- [ ] Support for property access (`user.name`)
- [ ] Support for array access (`items[0]`)
- [ ] Support for method calls (`name.toUpperCase()`)
- [ ] Support for filters (`value | uppercase | truncate:20`)

## Implementation Details

### Pure Thorn Implementation
```thorn
# Example implementation structure
let templateCache = {};
let helpers = {};
let partials = {};

export $ compile(template) {
    # Parse template into AST
    let tokens = tokenize(template);
    let ast = parse(tokens);
    
    # Return compiled function
    return $ (data) {
        return evaluate(ast, data);
    };
}

export $ render(template, data) {
    # Check cache first
    if (template in templateCache) {
        return templateCache[template](data);
    }
    
    # Compile and cache
    let compiled = compile(template);
    templateCache[template] = compiled;
    
    return compiled(data);
}

export $ registerHelper(name, func) {
    helpers[name] = func;
}

# Example helper registration
registerHelper("formatDate", $ (date, format) {
    # Date formatting logic
    return formattedDate;
});
```

## Testing Requirements
- [ ] Unit tests for all template syntax features
- [ ] Integration tests with complex templates
- [ ] Performance benchmarks for template compilation
- [ ] Security testing for XSS prevention
- [ ] Edge case testing (nested loops, deep objects)

## Documentation Requirements
- [ ] Template syntax guide
- [ ] Helper function reference
- [ ] Security best practices
- [ ] Performance optimization tips
- [ ] Migration guides from other template engines

## References

### Python Standard Library Equivalents
- `string.Template` - Basic template substitution
- Third-party: `jinja2` - Advanced templating
- `mako` - Python-based templating
- `django.template` - Django template engine
- `string.Formatter` - Advanced string formatting

### Rust Standard Library Equivalents
- `format!` macro - Basic string interpolation
- Third-party: `handlebars` - Handlebars templating
- `tera` - Jinja2-like templating
- `askama` - Type-safe templating
- `liquid` - Liquid templating engine
- `maud` - Compile-time HTML templating

## Acceptance Criteria
- [ ] Full Handlebars-compatible syntax support
- [ ] Custom helper and partial registration
- [ ] Template compilation and caching
- [ ] HTML escaping by default
- [ ] Template inheritance support
- [ ] Performance within 3x of native string operations
- [ ] Comprehensive security features

## Priority
Medium - Important for web development and text generation

## Labels
- stdlib
- templating
- pure-thorn
- text-processing