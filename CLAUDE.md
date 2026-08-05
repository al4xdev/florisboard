# CLAUDE.md — FlorisBoard grammar-fix fork

## Why this fork exists

We were building an AI grammar-correction feature into a different keyboard fork
(Deskdrop, a HeliBoard-based project at `/home/alex/git/my/Deskdrop`) and hit a wall:
the AI Assist pipeline there is spread across ~4 overlapping code paths
(`processWithModelAndInstruction`, `processInlineWithModel`/`callWithModel`,
`processWithModelAndInstructionStream`, plus Prompt Aliases/Tone Chips that silently
use a *different*, unfixed prompt path), and after several rounds of real bugs
(weak instruction channel via flattened `user`-role prompts, a broken JSON-schema
"structured output" experiment, missing `system` role / `temperature`), the user
was still seeing unreliable corrections in the actual app that didn't reproduce via
direct `curl` testing to the same model. Given the codebase's size and the narrow
actual need (one reliable "fix grammar" button), we pivoted: clone FlorisBoard
(a clean, modern, actively-maintained AOSP-style keyboard) and add ONE hardcoded
toolbar button that does the correction via a local server, reusing a prompt/technique
already proven 100% reliable in the user's own script.

**Ignore anything about Deskdrop below — that project is set aside.**

## What actually works, proven

`~/.local/bin/grammar-fix-editor.sh` (a Claude Code `$EDITOR` hook, unrelated to
any keyboard) POSTs to a local `llama-server` at `http://localhost:8080/v1/chat/completions`
with:
- `role: system` = the grammar-fix instruction (see below)
- `role: user` = the raw text to fix
- `temperature: 0`

This corrects reliably, every time the user has tested it. The FlorisBoard button
mirrors the same prompt/request shape, but **does NOT hit that local server** —
see "Endpoint decision" below for why.

### Endpoint decision: OpenRouter cloud, not localhost

Initially built to POST to the local `llama-server` (mirroring the script above
exactly), but that only works if the phone can reach the dev machine's
`localhost:8080` — not true for a real phone unless you set up `adb reverse`,
LAN IP, or a tunnel. The user decided (explicitly) to skip that networking
problem for this prototype and just hardcode a cloud OpenRouter call instead,
using a test API key. This was **also already proven reliable** earlier in the
same working session (5/5 correct, live-tested via curl) with:
`model: ~deepseek/deepseek-v4-flash-latest`, `reasoning: {effort: none}`,
`temperature: 0`, same system prompt. So the current `GrammarFixAction.kt` POSTs
to `https://openrouter.ai/api/v1/chat/completions` with a hardcoded
`Authorization: Bearer sk-or-v1-...` key (search the file for `OPENROUTER_API_KEY`).

**This is explicitly a throwaway test key, hardcoded on purpose, per direct user
instruction** ("pode ser hardcoded... é só uma chave de testes"). Do not "fix"
this into a settings-stored key without being asked — the user said the next
step (in a future session, with "next AI agent") is to add a proper settings UI
and turn this into their own real fork. Until then, leave it hardcoded as-is.

The system prompt (hardcoded, do not "improve" without testing — this exact wording
was chosen deliberately):

```
You are a grammar fixer for a coding-agent message input.
Fix ONLY grammar, spelling, punctuation, typos, and capitalization errors.
Rules:
- Do NOT translate technical terms (code, commands, git terms like commit/branch/merge/staging, framework names, APIs, jargon, file paths, URLs, identifiers, quoted strings) — keep them in their technical form even when the surrounding text is in another language.
- Do not change code, commands, file paths, URLs, identifiers, or quoted strings.
- Do not rewrite style, add, or remove content.
- Keep line breaks and overall structure.
- Reply with ONLY the corrected text. No explanations, no quotes, no preamble.
```

## What's been done so far

1. **Cloned** FlorisBoard into `/home/alex/git/my/florisboard` (`git clone https://github.com/florisboard/florisboard.git`), fresh, no fork/PR relationship set up yet (this is a personal hack — FlorisBoard's own `AI_POLICY.md` forbids AI-written contributions anyway, so this will never be upstreamed).

2. **Explored the architecture** (via a research subagent) to find the right extension point. Key findings:
   - Toolbar buttons = `QuickAction` sealed class (`app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/QuickAction.kt`). Existing `InsertText` variant was the closest pattern to copy.
   - Text I/O: `FlorisImeService.currentInputConnection()` gives the raw Android `InputConnection` — used directly (`getSelectedText`, `performContextMenuAction(android.R.id.selectAll)`, `commitText`) rather than the higher-level `EditorInstance` wrapper, to keep the hack simple and not depend on FlorisBoard's windowed-text-snapshot semantics.
   - No existing HTTP client dependency in the project at all — used plain `java.net.HttpURLConnection` (matches the pattern from the Deskdrop work, no new Gradle dependency needed). `kotlinx-serialization-json` IS already a dependency, used for JSON building/parsing.
   - `AI_POLICY.md` restricts contribution *process* (no AI-agentic code contributions upstream), not app features that call AI/network services — irrelevant here anyway since we're not upstreaming.

3. **Implemented the button** (all committed to disk, not yet built/tested on device):
   - **New file**: `app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/quickaction/GrammarFixAction.kt`
     - `object FixGrammar : QuickAction()` — on tap: reads selected text via `InputConnection.getSelectedText()`; if nothing selected, does `performContextMenuAction(android.R.id.selectAll)` first then reads the (now full-field) selection; POSTs to OpenRouter (`https://openrouter.ai/api/v1/chat/completions`, hardcoded `Bearer` key, model `~deepseek/deepseek-v4-flash-latest`, `reasoning: {effort: none}`, `temperature: 0`, `max_tokens: 2048`, hardcoded prompt above); on success, replaces the selection via `InputConnection.commitText(...)`.
     - Runs the network call on a module-level `CoroutineScope(SupervisorJob() + Dispatchers.IO)`; commits the result back on `Dispatchers.Main`.
     - Silent no-op on any failure (no toast/error UI yet — see "Not done yet" below).
   - **`QuickActionArrangement.kt`**: registered `FixGrammar::class` in the polymorphic JSON serializer (`QuickActionJsonConfig`), and added `FixGrammar` as the first entry in `QuickActionArrangement.Default.dynamicActions` so it shows in the toolbar by default with no settings-UI configuration needed.
   - **`QuickActionButton.kt`**: added an `is FixGrammar ->` branch in the button-rendering `when` block (renders as text label `"Fix"` for now — no icon).
   - **`QuickAction.kt`**: added `is FixGrammar ->` branches to `computeDisplayName()` (`"Fix grammar"`) and `computeTooltip()` (`"Fix grammar (selection, or select-all if nothing selected)"`) — required because these are exhaustive `when` blocks over the sealed class, Kotlin forces it.
   - **`AndroidManifest.xml`**: added `<uses-permission android:name="android.permission.INTERNET"/>` (app had zero network permissions before — first network feature ever) and `android:usesCleartextTraffic="true"` on `<application>`. The cleartext flag was needed for the original localhost-plain-HTTP plan; it's harmless now that the endpoint is HTTPS OpenRouter, left in place in case localhost mode comes back later.

4. **Docker build environment** — FlorisBoard's build is heavier than Deskdrop's:
   - `compileSdk`/`targetSdk` = 36, `minSdk` = 26 (vs Deskdrop's 35/21) — from `gradle.properties`.
   - `buildTools` 36.0.0, `ndk` 29.0.14206865, `cmake` 4.1.2 — pinned in `gradle/tools.versions.toml`.
   - **Native code is Rust, not C/C++ Android.mk** — `lib/native` module (`lib/native/src/main/rust/`) is built via CMake (`CMakeLists.txt`) which shells out to `cargo`/`rustup` directly (`find_program(RUSTUP_EXECUTABLE ...)`, `cargo rustc --release --locked --target <android-target>`), producing a static lib linked into `libfl_native_rust.a` → `fl_native.so`. Requires `rustup` + `cargo` on `PATH`, with Android targets added (`armv7-linux-androideabi`, `aarch64-linux-android`, `i686-linux-android`, `x86_64-linux-android`).
   - Gradle 9.4.1, AGP 9.0.0, JDK 17 (`gradle/wrapper/gradle-wrapper.properties`, `gradle/tools.versions.toml`).
   - **Dockerfile is in-repo**: `docker/Dockerfile` (committed to the project itself, not a scratchpad path — durable, use this).
     - Base: `eclipse-temurin:17-jdk`
     - Installs: `cmake`, `ninja-build`, Android cmdline-tools → SDK platform 36, build-tools 36.0.0, NDK 29.0.14206865, plus `rustup` (toolchain 1.93.0, minimal profile) with the 4 Android Rust targets added.
     - Also bakes in `git config --global --add safe.directory /workspace` (see gotcha below).
   - Image tag: `florisboard-android-build:latest`
   - Build the image:
     ```bash
     cd /home/alex/git/my/florisboard/docker
     docker build -t florisboard-android-build:latest .
     ```
     (Took two tries in this session — first attempt hit `gzip: invalid checksum` mid-NDK-download, a transient Docker layer-diff corruption unrelated to the Dockerfile itself. `docker container prune -f && docker image prune -f` then a plain retry fixed it.)
   - Build the app:
     ```bash
     mkdir -p /tmp/florisboard-gradle-cache   # or anywhere — just a persistent cache dir, reused across runs
     docker run --rm \
       -v /home/alex/git/my/florisboard:/workspace \
       -v /tmp/florisboard-gradle-cache:/root/.gradle \
       florisboard-android-build:latest \
       bash -c "chmod +x gradlew && ./gradlew assembleDebug --no-daemon"
     ```
   - **Gotcha already hit and fixed in the Dockerfile**: the container runs as root, `/workspace` is bind-mounted from the host and owned by the host user, and `app/build.gradle.kts` (`getGitCommitHash()`, around line 78) shells out to `git` to embed `BUILD_COMMIT_HASH`. Without `safe.directory` configured, git refuses ("detected dubious ownership in repository") and the build fails with exit 128 at that exact line. Already baked into `docker/Dockerfile` — if you rebuild the image from this file, no extra step needed. If you're running an *older* already-built image that predates this fix, prefix the app-build command with `git config --global --add safe.directory /workspace &&`.
   - No `local.properties`-style issue like Deskdrop had — not hit in this session, but hasn't been extensively stress-tested either.
   - APK output: check `app/build/outputs/apk/debug/` after a successful build for the exact filename (the CI workflow `.github/workflows/android.yml` uploads `app/build/outputs/apk/debug/app-debug.apk`, so that's the likely name, but confirm — this repo may rename it like Deskdrop did).
   - Ownership: files written by the container (build outputs) end up owned by `root` on the host, since the container runs as root against the bind mount. Fix with:
     ```bash
     docker run --rm -v /home/alex/git/my/florisboard:/workspace alpine \
       chown -R $(id -u alex):$(id -g alex) /workspace/app/build/outputs/apk
     ```

## Status: builds clean, APK produced, NOT YET TESTED ON DEVICE

`./gradlew assembleDebug` succeeds end to end (2m18s, warm gradle cache). APK is at
`app/build/outputs/apk/debug/app-debug.apk` (~37.6MB). Two build issues were hit
and fixed along the way (both already baked into `docker/Dockerfile`, see gotchas
below) — a fresh `docker build` from the current Dockerfile should not need either
manual fix again:
1. `git` "dubious ownership" (container runs as root, `/workspace` bind-mount owned
   by host user) → fixed with `git config --global --add safe.directory /workspace`.
2. `[CXX1300] CMake '4.1.2' was not found` → the Dockerfile's `apt-get install cmake`
   pulled Ubuntu's system cmake (4.2.3), but AGP's native build wants the *exact*
   pinned version from `tools.versions.toml` and only looks in the SDK/PATH/`cmake.dir`
   for that exact version — fixed by installing `cmake;4.1.2` via `sdkmanager`
   instead of relying on the apt package.

The Kotlin code (`GrammarFixAction.kt` and the 4 files it touches) compiled with
**no changes needed** from what was written blind against the explored architecture
— no wrong imports, no `kotlinx.serialization.json` API mistakes, exhaustive-`when`
branches all accepted.

## Icon

Toolbar button renders `Icons.Default.Spellcheck` (from `material-icons-extended`,
already a project dependency) instead of a text glyph — swapped in after the
first successful build. If you want to change it, edit the `is FixGrammar ->`
branch in `QuickActionButton.kt`.

⚠️ **The base image (`florisboard-android-build:latest`) still does NOT have
`cmake;4.1.2` baked in** — every `docker run` so far has installed it ad-hoc via
`sdkmanager --install 'cmake;4.1.2'` chained into the build command (see the
Dockerfile gotcha above; the Dockerfile file itself is already fixed, the image
just hasn't been rebuilt from it yet). Either keep chaining that sdkmanager
install before `./gradlew`, or run `docker build` again from `docker/Dockerfile`
once to bake it in properly and stop needing the workaround.

## Not done yet / next steps, in likely order

1. **Get the APK onto the device and manually test**: select text → tap the spellcheck icon → confirm it POSTs to OpenRouter and replaces text correctly.
3. **Known rough edges to refine once the basic flow works** (per user: "se der certo nós refinamos"):
   - No error feedback at all right now — if the server's down, the request times out, or JSON parsing fails, the button just does nothing silently. Should probably show a toast.
   - Button renders as a bare "Fix" text glyph, no real icon.
   - URL (`http://localhost:8080`) and the system prompt are both hardcoded constants in `GrammarFixAction.kt` — no settings UI. Fine for now per explicit instruction, but obvious next step if this becomes a keeper.
   - Select-all fallback (`performContextMenuAction(android.R.id.selectAll)`) is untested — not all `InputConnection` implementations honor this the same way; worth confirming it behaves as expected across a couple of target apps.
   - Network reachability is a non-issue now (cloud OpenRouter, works from anywhere with internet) — the earlier localhost/`adb reverse`/LAN-IP problem no longer applies. If a switch back to local `llama-server` is wanted later (privacy, cost, offline use), revisit `GRAMMAR_FIX_URL`/`GRAMMAR_FIX_MODEL`/the `Authorization` header and reintroduce the reachability question above.
4. Nothing has been committed to git yet in this repo (no `git init`-level check even done) — decide on a commit strategy once the button is confirmed working.
