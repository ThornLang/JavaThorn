#include <napi.h>

typedef struct TSLanguage TSLanguage;

extern "C" TSLanguage *tree_sitter_thorn();

namespace {

Napi::Object Init(Napi::Env env, Napi::Object exports) {
  exports["name"] = Napi::String::New(env, "thorn");
  auto language = reinterpret_cast<void *>(tree_sitter_thorn());
  exports["language"] = Napi::External<void>::New(env, language);
  return exports;
}

NODE_API_MODULE(tree_sitter_thorn_binding, Init)

}  // namespace