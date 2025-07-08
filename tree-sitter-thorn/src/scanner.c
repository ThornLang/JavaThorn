#include <tree_sitter/parser.h>

enum TokenType {
  DUMMY_TOKEN = 0
};

void *tree_sitter_thorn_external_scanner_create() {
  return NULL;
}

bool tree_sitter_thorn_external_scanner_scan(void *payload, TSLexer *lexer,
                                            const bool *valid_symbols) {
  return false;
}

unsigned tree_sitter_thorn_external_scanner_serialize(void *payload, char *buffer) {
  return 0;
}

void tree_sitter_thorn_external_scanner_deserialize(void *payload, const char *buffer, unsigned length) {
}

void tree_sitter_thorn_external_scanner_destroy(void *payload) {
}