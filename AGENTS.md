# AGENTS.md

Single-module Android app (`:app`), Jetpack Compose + Room + Retrofit. Package `id.calltag.app`. UI copy, docs, and code comments are in **Indonesian** — keep new user-facing strings and comments Indonesian for consistency.

## Build / verify
- Build: `./gradlew assembleDebug` (or `:app:compileDebugKotlin` for a fast compile check).
- Unit tests now exist in `app/src/test/` (Tahap 5/5B). Run `./gradlew testDebugUnitTest assembleDebug` for verification. There are no instrumented `androidTest` suites.
- Offline works if the Gradle cache is warm: append `--offline`.
- Requires JDK 17 (CI uses Temurin 17) and `local.properties` with `sdk.dir`. `compileSdk`/`targetSdk` = 34, `minSdk` = 26, `jvmTarget` = 17.
- All dependency versions live in `gradle/libs.versions.toml`; add libs there and reference via `libs.*` aliases, never hardcode versions in `app/build.gradle.kts`.

## Backend is NOT in this repo
- The REST API (Go `gocroot` + MongoDB) and Swagger docs are separate repos. This repo is only the mobile client.
- Debug defaults to `http://10.0.2.2:8080/` (emulator → host localhost). Set `-PbaseApiUrl=...` or env `BASE_API_URL` to override it. Release requires an explicit HTTPS URL and blocks cleartext traffic.
- All network calls are wrapped in `runCatching` + `withTimeoutOrNull`, and the app **degrades to offline seed data** (`data/local/OfflineSeedData.kt`) rather than throwing. Preserve this offline-first fallback when touching `CallTagRepository`.

## Architecture (where things are wired)
- Wiring: `CallTagApplication` lazily builds `CallTagDatabase`, `CallTagPreferences`, `CallTagRepository`. No DI framework — add dependencies through `CallTagApplication` and pass down.
- `data/remote/NetworkModule.kt` exposes **two** Retrofit clients on `ApiService`: a standard client (8s timeouts) and a `fastScreeningApiService` with **800ms timeouts** used only by the call-screening service. Use the fast one for anything on the incoming-call path.
- Tahap 5B: login is explicit and optional. Legacy tokens do not grant account mode; never bootstrap guests through 401 handling. Session/outbox ownership and personal whitelist are local. Guest may still submit/track disputes. Account settings live in `AccountSettingsScreen`; theme persists in DataStore.
- `PRD.md` §5.3 summarizes API groups. The wire contract is in the separate `apidocs` repository (`openapi.json`). `PRD.md` is the product spec and `DESIGN.md` is the visual design reference (colors, four reputation levels, monospace phone digits, 56dp/64dp touch targets). Consult them before changing DTOs, categories, or UI styling.
- Call flow: `service/CallTagScreeningService` (a `CallScreeningService`) → `PhoneNormalizer.normalizeToE164` → `repository.screenIncomingPhone` (Room cache first, then 800ms API) → auto-block decision in `PhoneNormalizer.shouldAutoBlock` (gated by the DataStore `auto_block_scammer` toggle, default OFF (opt-in)) → `service/CallerIdNotifier` (heads-up notification + `TYPE_APPLICATION_OVERLAY` window). The overlay draws with `android.view` + `WindowManager`, not Compose.
- Room DB is version 2 with explicit `Migration(1, 2)` for outbox/sync/provenance. Preserve existing data and add explicit migrations when changing schema.
- UI: `MainScaffold` holds a 5-tab bottom nav (Cari / Darurat / Beri #Tag / Proteksi / Akun) driven by an integer index, no Navigation library. Screens live in `ui/screens/`; state via `CallTagMainViewModel` (`UiLoadState` sealed type: Idle/Loading/Success/Error).
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
- Category codes (`PINJOL_DC`, `APK_SNIFFING`, `IMPERSONATION`, `JUDOL`, `SOCIAL_ENG`, `TELEMARKETING`) and reputation levels (`CLEAN`/`SUSPICIOUS`/`SPAM`/`DANGER_SCAM`) are fixed enums in `dto/ApiDtos.kt` (plus neutral `INFO_TAG`, weight 0) — do not add ad-hoc strings; they must match `PRD.md` tables.
- Phone numbers are normalized to E.164 (`+62…`); short emergency codes (≤6 digits like `112`) are intentionally kept as-is. Use `PhoneNormalizer`, don't re-implement.
- Gson DTOs use `@SerializedName`; some scores accept `spam_score` as an alternate for `risk_score`. Keep alternates when editing DTOs.
- ProGuard keeps all `data.remote.dto.**` and `data.local.entity.**` (Gson/Room reflection) — new DTO/entity packages need equivalent keep rules.
- Many UI strings are hardcoded inline in Composables (not in `strings.xml`, which only holds the app name and notification channel). Match the surrounding file's style.

## Release / CI
- `.github/workflows/release-android.yml` runs for changes to Android build files on `main` and supports `workflow_dispatch` on `main` with `bump_type`.
- The workflow computes the next semver from the latest `v*.*.*` tag and commit message, builds the signed APK and AAB, then tags the built commit and publishes both files.
- Set the GitHub Actions variable `BASE_API_URL` and the secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`. Local release builds use `KEYSTORE_FILE` in place of `KEYSTORE_BASE64`. See [deployment and demo](docs/planning/16-DEPLOYMENT-DAN-DEMO.md).
- `isMinifyEnabled = false` for release; don't assume R8/proguard runs.
