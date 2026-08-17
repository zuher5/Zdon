# Zdon — Working Notes for Agents

## Build & verify
- Project: /home/zuher/Zdon (Kotlin, Jetpack Compose, multi-module, Gradle 8.14.3).
- Commands (run **sequentially, never in parallel** — parallel heavy tasks OOM-freeze this
  7.4 GiB machine; memory is already capped in `gradle.properties`):
  1. `./gradlew test`
  2. `./gradlew :app:assembleDebug`
  3. `./gradlew :app:assembleRelease` (only when signing is needed)
- SDK: `export ANDROID_HOME=/home/zuher/Android/Sdk` before any build.
- Release signing resolves via `providers.gradleProperty` first, then env
  (`ZDON_KEYSTORE_PATH`, `ZDON_STORE_PASSWORD`, `ZDON_KEY_ALIAS`,
  `ZDON_KEY_PASSWORD`). The private keystore + credentials live in
  `/home/zuher/.zdon-release/` (never commit). Debug signing is the fallback —
  a release APK is only "real" if `apksigner verify --print-certs` shows the
  Zdon cert (SHA-256 `535f200a...`), not the Android debug key.
- Gotchas learned:
  - The Gradle daemon does not see env vars set after it starts. Prefer the
    user-level `~/.gradle/gradle.properties` (or `-P`) for config the daemon reads.
  - PKCS12 keystores use a single password: store == key password (Android's
    key reader uses the key password to decrypt even though keytool ignores it).
  - `java.io.File` is not in the Kotlin DSL default imports — import it.
  - Reboots clear `/tmp`; keep working copies under `/home/zuher`, not `/tmp`.

## Design system
- Follow the DnD skill `minimal-zdon-ui` (in `.opencode/skills/minimal-zdon-ui`)
  for every UI change: neutral monochrome surfaces, single blue accent, flat rows
  + hairline dividers, quiet outlined chips, no decorative color.