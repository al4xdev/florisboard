import grammar_fix_benchmark as benchmark


DEEPSEEK_URL = "https://api.deepseek.com/chat/completions"
DEEPSEEK_MODEL = "deepseek-v4-flash"
original_build_body = benchmark.build_body


def build_deepseek_body(text, level, language_tag, model, provider):
    body = original_build_body(text, level, language_tag, model, provider)
    body.pop("provider")
    body.pop("tool_choice")
    body["thinking"] = {"type": "disabled"}
    body["messages"][0]["content"] += "\nYou must call return_grammar_correction with the edited text."
    return body


benchmark.URL = DEEPSEEK_URL
benchmark.DEFAULT_MODEL = DEEPSEEK_MODEL
benchmark.DEFAULT_PROVIDER = ""
benchmark.API_KEY_ENV = "DEEPSEEK_API_KEY"
benchmark.build_body = build_deepseek_body


if __name__ == "__main__":
    benchmark.main()
