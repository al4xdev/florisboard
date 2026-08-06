# FlorisBoard AI fork

## Purpose

This fork adds two AI-powered quick actions to FlorisBoard:

- Grammar Fix edits the selected text, or the full field when nothing is selected.
- Custom AI Prompt applies a user-configured transformation.

The fork is personal and is not intended for upstream contribution.

## AI configuration

### Backend selector

The top of the AI settings screen selects the backend, and the whole settings
block below it swaps. Two backends exist, defined in `AiBackend.kt`:

- `OPEN_ROUTER` (default) talks to `https://openrouter.ai/api/v1/chat/completions`.
- `DEEPSEEK` talks to `https://api.deepseek.com/chat/completions`.

The selector uses an inline `listPrefEntries` block with hardcoded Kotlin
strings. This fork deliberately avoids `enumDisplayEntriesOf` and `strings.xml`
for AI-facing copy.

### Preference namespaces

API keys are stored per backend on purpose: an OpenRouter key and a DeepSeek key
are not interchangeable, so switching backends never sends the wrong credential.

| Key | Purpose |
| --- | --- |
| `ai__backend` | Active backend |
| `ai__openrouter__api_key` | OpenRouter key |
| `ai__openrouter__model` | Default `deepseek/deepseek-v4-flash-0731` |
| `ai__openrouter__provider` | Default `deepinfra/fp4` |
| `ai__deepseek__api_key` | DeepSeek key |
| `ai__deepseek__model` | Default `deepseek-v4-flash` |
| `ai__custom_prompt` | Shared across backends |
| `ai__level` | Shared across backends |

`AppPrefs.migrate` moves the old flat keys `ai__api_key`,
`ai__open_router_model`, and `ai__open_router_provider` into the OpenRouter
namespace, because existing users were on OpenRouter.

The default custom prompt lives once in `AiDefaults.CUSTOM_PROMPT`.

### Request contracts

Grammar Fix uses a forced `return_grammar_correction` tool call with a
`corrected_text` string on both backends. Custom AI Prompt reads normal message
content on both backends. Both backends share the executor, cancellation
plumbing, and response parsing in
`ime/smartbar/quickaction/ai/AiClient.kt`; only the endpoint URL and the request
body differ.

OpenRouter sends `reasoning: {effort: "none"}` plus a `provider` block. Fallback
is disabled when a provider is selected, and `require_parameters` is set for the
structured path. Users can clear the provider field to allow automatic OpenRouter
routing or enter another provider that supports the selected request parameters.
The selected provider must support tools and `tool_choice`.

The native DeepSeek API silently ignores `provider` and `reasoning`, and ignoring
`reasoning` leaves thinking enabled. The DeepSeek adapter therefore sends
`thinking: {"type": "disabled"}` and omits `provider` and `reasoning` entirely.
Valid model slugs are exactly `deepseek-v4-flash` and `deepseek-v4-pro`.

The response envelope is identical on both backends
(`choices[0].message.tool_calls[0].function.arguments` for the structured path,
`choices[0].message.content` for the plain path).

### Help screen

There is a single route `settings/ai/help`. `AiHelpScreen` reads `ai__backend`
and renders either the OpenRouter or the DeepSeek body, with the title following
the backend. Both quick actions deep-link to it whenever the active backend's API
key is blank.

### Prompt ladder

The grammar prompts live in `AiLevel.kt`, one per level, each built from shared
`private const val` fragments (`QUESTION_INTEGRITY`, `TENSE_INFERENCE`, `SAFETY`,
and others) so the repeated blocks stay consistent across levels.

| Level | Behavior |
| --- | --- |
| Low | Spelling and diacritics only, structure untouched |
| Med | Full copy-edit, preserves organization |
| High | Copy-edit plus gentle rephrasing for natural flow |
| XHigh | Aggressive professional rewrite that removes redundancy |
| Max | Free rebuild: may reorder, merge, and split sentences |

Max may restructure without limit but must keep every unique fact, must not add
information, and must not convert a question into a statement. Every level
preserves the exact number of question marks, which is what stops the model
answering the input instead of editing it. The keyboard locale is supplied as a
hint. Prompts treat the input as text to edit, never as a request to answer or
execute, and preserve unknown names, commands, paths, URLs, identifiers, quoted
strings, and technical terms.

Changing a prompt is a behavioral change: run the benchmark before and after.

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
OpenAI-compatible DeepSeek API. It drops `provider` and sends
`thinking: {"type": "disabled"}`, matching `DeepSeekClient`. Forced `tool_choice`
works on the native DeepSeek API, so the adapter keeps it.

```bash
DEEPSEEK_API_KEY=... uv run --python 3.12 utils/grammar_fix_benchmark_deepseek.py --workers 30 --request-interval 0
```

The Python prompt copies are kept byte-identical to `AiLevel.kt`, including the
blank line before the locale guidance. Cases assert question-mark counts, facts
that must survive, patterns that indicate the model answered instead of edited,
and condensation ratios for the redundancy cases.

Two known failures, both predating the prompt-ladder shift and reproducible with
the old prompts:

- One English redundancy case does not condense enough at XHigh and Max. The
  Portuguese equivalents condense correctly, so this is an English-side model
  weakness.
- Low intermittently splits `ligar aondme isso ?` into two questions, violating
  its own rule against creating sentence fragments.

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
  bash -lc "git config --global --add safe.directory /workspace && yes | sdkmanager --install 'cmake;4.1.2' >/dev/null && cd /workspace && chmod +x gradlew && ./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs='-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC'"
```

`-XX:+UseParallelGC` is required. Without it the Gradle daemon intermittently dies
with `SIGSEGV` inside `G1ParScanThreadState::trim_queue_to_threshold`, reported as
"Gradle build daemon disappeared unexpectedly" and leaving an `hs_err_pid*.log` in
the repo root. It is a G1 collector crash, not a project error, and it is not
fixed by raising the heap.

Never pipe the build into `tail` or `head`: the pipeline reports the exit status
of the last command, so a failed build looks like it passed. Redirect to a log
file and check `$?`.

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Build after
the final commit because the debug version embeds the Git commit hash. Verify with
`aapt2 dump badging <apk> | head -1` — the `versionName` must end in the short
hash of `HEAD`.

The current project version is `0.6.0-alpha02`.
