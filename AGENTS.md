# AGENTS.md

Single-module Android app (`:app`), Jetpack Compose + Room + Retrofit. Package `id.calltag.app`. UI copy, docs, and code comments are in **Indonesian** — keep new user-facing strings and comments Indonesian for consistency.

## Build / verify
- Build: `./gradlew assembleDebug` (or `:app:compileDebugKotlin` for a fast compile check).
- **No tests exist.** There are no `test`/`androidTest` source dirs. Do not invent `./gradlew test` expectations; verify changes by compiling.
- Offline works if the Gradle cache is warm: append `--offline`.
- Requires JDK 17 (CI uses Temurin 17) and `local.properties` with `sdk.dir`. `compileSdk`/`targetSdk` = 34, `minSdk` = 26, `jvmTarget` = 17.
- All dependency versions live in `gradle/libs.versions.toml`; add libs there and reference via `libs.*` aliases, never hardcode versions in `app/build.gradle.kts`.

## Backend is NOT in this repo
- The REST API (Go `gocroot` + MongoDB) and Swagger docs are separate repos. This repo is only the mobile client.
- `BuildConfig.BASE_API_URL` defaults to `http://10.0.2.2:8080/` (emulator → host localhost). Override with `-PbaseApiUrl=...` or env `BASE_API_URL`.
- All network calls are wrapped in `runCatching` + `withTimeoutOrNull`, and the app **degrades to offline seed data** (`data/local/OfflineSeedData.kt`) rather than throwing. Preserve this offline-first fallback when touching `CallTagRepository`.

## Architecture (where things are wired)
- Wiring: `CallTagApplication` lazily builds `CallTagDatabase`, `CallTagPreferences`, `CallTagRepository`. No DI framework — add dependencies through `CallTagApplication` and pass down.
- `data/remote/NetworkModule.kt` exposes **two** Retrofit clients on `ApiService`: a standard client (8s timeouts) and a `fastScreeningApiService` with **800ms timeouts** used only by the call-screening service. Use the fast one for anything on the incoming-call path.
- `ApiService` endpoint contracts are documented against `PRD.md` §5.3 (11 endpoints). `PRD.md` is the spec source of truth; `DESIGN.md` is the visual/design source of truth (colors, 4-tier `reputation_level`, monospace phone digits, 56dp/64dp touch targets). Consult them before changing DTOs, categories, or UI styling.
- Call flow: `service/CallTagScreeningService` (a `CallScreeningService`) → `PhoneNormalizer.normalizeToE164` → `repository.screenIncomingPhone` (Room cache first, then 800ms API) → auto-block decision in `PhoneNormalizer.shouldAutoBlock` (gated by the DataStore `auto_block_scammer` toggle, default ON) → `service/CallerIdNotifier` (heads-up notification + `TYPE_APPLICATION_OVERLAY` window). The overlay draws with `android.view` + `WindowManager`, not Compose.
- Room DB is version 1 with `fallbackToDestructiveMigration()` and `exportSchema = false` — schema changes silently wipe local data; no migrations are written. Bump carefully and re-seed if needed.
- UI: `MainScaffold` holds a 4-tab bottom nav (Cari / Darurat / Beri #Tag / Proteksi) driven by an integer index, no Navigation library. Screens live in `ui/screens/`; state via `CallTagMainViewModel` (`UiLoadState` sealed type: Idle/Loading/Success/Error).
- Design tokens live in `ui/theme/CallTagTokens.kt` (`CallTagSpacing`, `CallTagRadius`, `CallTagType`, `CallTagTouch`). Take spacing, radius, font sizes and touch targets from these instead of writing `.dp`/`.sp` literals — `HomeScreen` and `EmergencyScreen` are already converted; `ReportAndHubScreen` and `SpamGuardScreen` still use inline literals.
- Brand palette is "Nusantara Teal" (`brandPrimary` `#0F766E` light / `#2DD4BF` dark) defined in `ui/theme/CallTagTheme.kt`. Danger semantics (`dangerScam`/`spamOrange`/`suspiciousAmber`) are a PRD contract — **do not change them**; they are duplicated as hex literals in `service/CallerIdNotifier.kt` and `ui/components/CallerIdOverlayCard.kt`, so palette changes must be applied in all three files.
- Two separate colour roles, do not mix them: `dangerScam`/`brandPrimary` are **foreground/text** colours (bright in dark mode), while `dangerSolid`/`brandSolid` are **button background** colours (always dark enough for white text). Using a foreground colour as a button background produced unreadable teal-on-teal and salmon-on-white in dark mode.

## Visual verification
The Android SDK and an AVD (`Pixel_5_Light`) are installed. You can screenshot the real UI headlessly instead of guessing:

```bash
~/Android/Sdk/emulator/emulator -avd Pixel_5_Light -no-window -no-audio -no-snapshot \
  -gpu swiftshader_indirect -no-boot-anim &
~/Android/Sdk/platform-tools/adb wait-for-device   # boot takes ~30s
./gradlew assembleDebug --offline
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Android/Sdk/platform-tools/adb shell am start -n id.calltag.app/.MainActivity
~/Android/Sdk/platform-tools/adb exec-out screencap -p > /tmp/shot.png   # then read the PNG
```

Tab coordinates at 1080x2340 (4 tabs, centers): Cari `135,2100`, Darurat `400,2100`, Beri #Tag `675,2100`, Proteksi `950,2100`. Sub-tab pills in Beri #Tag: `180,425` / `490,425` / `850,425`. The dark-mode toggle sits at `940,205`. `wm density` is 440, so 1dp ≈ 2.75px. Compose layout bugs (a `fillMaxHeight()` ribbon inside a `Row`, a chip overflowing its card) only showed up on screenshots — compile success proves nothing about layout.

## Design token status
All four screens (`HomeScreen`, `EmergencyScreen`, `ReportAndHubScreen`, `SpamGuardScreen`) use `CallTagSpacing`/`CallTagRadius`/`CallTagType`/`CallTagTouch`. Remaining inline `.dp`/`.sp` literals are accepted exceptions: icon sizes, spinner stroke, border widths, micro-gaps ≤3dp, `lineHeight` pairings, and monospace phone/score sizes (`CallTagDesign.monoStyle`).

## Conventions & gotchas
- Launcher icon is an adaptive icon (`mipmap-anydpi-v26` + `drawable/ic_launcher_foreground.xml`, background `#0F766E` in `values/colors.xml`); `minSdk` 26 means no PNG density buckets are needed.
- Category codes (`PINJOL_DC`, `APK_SNIFFING`, `IMPERSONATION`, `JUDOL`, `SOCIAL_ENG`, `TELEMARKETING`) and reputation levels (`CLEAN`/`SUSPICIOUS`/`SPAM`/`DANGER_SCAM`) are fixed enums in `dto/ApiDtos.kt` — do not add ad-hoc strings; they must match `PRD.md` tables.
- Phone numbers are normalized to E.164 (`+62…`); short emergency codes (≤6 digits like `112`) are intentionally kept as-is. Use `PhoneNormalizer`, don't re-implement.
- Gson DTOs use `@SerializedName`; some scores accept `spam_score` as an alternate for `risk_score`. Keep alternates when editing DTOs.
- ProGuard keeps all `data.remote.dto.**` and `data.local.entity.**` (Gson/Room reflection) — new DTO/entity packages need equivalent keep rules.
- Many UI strings are hardcoded inline in Composables (not in `strings.xml`, which only holds the app name and notification channel). Match the surrounding file's style.

## Release / CI
- `.github/workflows/release-android.yml` runs on **push to `main`** and on `workflow_dispatch` (with `bump_type`).
- It auto-computes the next semver tag from the last `v*.*.*` tag and the **latest commit message** (conventional commits: `feat` → minor, `feat!`/`BREAKING CHANGE` → major, else patch), then builds `assembleRelease bundleRelease` and publishes a GitHub Release with APK + AAB.
- So: commit prefixes on `main` directly control version numbers. `versionName`/`versionCode` may come from `-P` props or `VERSION_NAME`/`VERSION_CODE`/`GITHUB_REF_NAME` env. Release signing is optional — falls back to debug signing when keystore secrets are absent (see `app/build.gradle.kts`).
- `isMinifyEnabled = false` for release; don't assume R8/proguard runs.
