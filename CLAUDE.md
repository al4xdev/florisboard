# FlorisBoard AI fork

## Purpose

This fork adds two OpenRouter-powered quick actions to FlorisBoard:

- Grammar Fix edits the selected text, or the full field when nothing is selected.
- Custom AI Prompt applies a user-configured transformation.

The fork is personal and is not intended for upstream contribution.

## AI configuration

The AI settings screen stores:

- OpenRouter API key
- Model slug
- Provider slug
- Grammar intervention level
- Custom prompt

The default model is `deepseek/deepseek-v4-flash-0731`. The default provider is
`deepinfra/fp4`. Users can clear the provider field to allow automatic OpenRouter
routing or enter another provider that supports the selected request parameters.

Grammar Fix uses a forced `return_grammar_correction` tool call with a
`corrected_text` string. OpenRouter fallback is disabled when a provider is
selected. The selected provider must support tools and `tool_choice`. Custom AI
Prompt continues to read normal message content.

The grammar prompts are defined independently for Low, Med, High, XHigh, and Max
in `AiLevel.kt`. Max performs an aggressive professional rewrite and removes
redundancy while preserving unique information. The keyboard locale is supplied as a hint. Prompts treat the
input as text to edit, never as a request to answer or execute, and preserve
unknown names, commands, paths, URLs, identifiers, quoted strings, and technical
terms.

## Benchmark

`utils/grammar_fix_benchmark.py` tests the same structured OpenRouter contract as
the app. It covers English and Brazilian Portuguese across all five levels,
including this regression:

```text
Consegue traduzir a lingua do chebaka?
```

The expected behavior is to correct the text without answering the question:

```text
Consegue traduzir a língua do chebaka?
```

Run the OpenRouter benchmark with an environment-provided key:

```bash
OPENROUTER_API_KEY=... uv run --python 3.12 utils/grammar_fix_benchmark.py
```

`utils/grammar_fix_benchmark_deepseek.py` is a fast test adapter for the official
OpenAI-compatible DeepSeek API. It does not change the app endpoint or provider.

```bash
DEEPSEEK_API_KEY=... uv run --python 3.12 utils/grammar_fix_benchmark_deepseek.py --workers 30 --request-interval 0
```

API keys must never be committed.

## Android build

The durable build environment is `docker/Dockerfile`. The image tag used locally
is `florisboard-android-build:latest`. The project requires the exact Android SDK
CMake package `cmake;4.1.2`.

```bash
docker run --rm \
  -v /home/alex/git/my/florisboard:/workspace \
  -v /tmp/florisboard-gradle-cache:/root/.gradle \
  florisboard-android-build:latest \
  bash -lc "git config --global --add safe.directory /workspace && yes | sdkmanager --install 'cmake;4.1.2' >/dev/null && cd /workspace && chmod +x gradlew && ./gradlew assembleDebug --no-daemon"
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Build after
the final commit because the debug version embeds the Git commit hash.

The current project version is `0.6.0-alpha02`.
