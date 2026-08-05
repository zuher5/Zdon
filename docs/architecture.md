# Architecture

Zdon is a multi-module Android application built on Clean Architecture principles.
The goal of the module structure is to keep the UI, domain, and platform concerns
independently buildable and testable, and to keep incremental builds fast.

## Module graph

```
                          ┌─────────┐
                          │   app   │
                          └────┬────┘
                               │
        ┌──────────────┬───────┴───────┬──────────────┐
        │              │               │              │
 ┌──────▼──────┐ ┌─────▼─────┐ ┌───────▼──────┐ ┌─────▼──────┐
 │feature:home │ │feature:    │ │feature:      │ │feature:    │
 │             │ │downloads   │ │history       │ │settings    │
 └──────┬──────┘ └─────┬─────┘ └───────┬──────┘ └─────┬──────┘
        │              │               │              │
        └──────────────┴───────┬───────┴──────────────┘
                               │
              ┌────────────────▼────────────────┐
              │      core:designsystem          │
              └────────────────┬────────────────┘
                               │
              ┌────────────────▼────────────────┐
              │          core:data              │
              └────┬──────────┬──────────┬──────┘
                   │          │          │
        ┌──────────▼──┐ ┌─────▼─────┐ ┌──▼───────────┐
        │core:database│ │core:      │ │core:         │
        │             │ │datastore  │ │downloader    │
        └─────────────┘ └───────────┘ └──────┬───────┘
                                             │
                                    ┌────────▼────────┐
                                    │   core:engine   │
                                    └────────┬────────┘
                                             │
                                    ┌────────▼────────┐
                                    │    yt-dlp +     │
                                    │     FFmpeg      │
                                    └─────────────────┘

  core:common  ──── shared utilities, DI qualifiers, dispatchers
  core:model   ──── pure Kotlin domain models (JVM module, no Android deps)
```

## Module responsibilities

### `app`
Single-activity host. Owns navigation, theming entry point, splash screen, and
the `Application` class with Hilt's `@HiltAndroidApp`. Contains no business logic.

### `core:model`
Pure Kotlin (JVM) module. Holds domain models such as `DownloadItem`,
`MediaInfo`, `MediaFormat`, `HistoryEntry`, and the enums persisted by Room and
DataStore.

Intentionally has **no Android dependencies**. This keeps it fast to compile and
usable from unit tests without Robolectric.

### `core:common`
Cross-cutting utilities: coroutine dispatcher qualifiers, `@ApplicationScope`,
result wrappers, and formatting helpers.

### `core:database`
Room database, DAOs, and entities. Maps entities to `core:model` types at the
DAO boundary so Room types never leak upward.

### `core:datastore`
Preferences DataStore wrapper exposing user settings as a `Flow`.

### `core:engine`
Thin wrapper around `youtubedl-android`. Responsible for:
- Lazy initialization of the bundled Python runtime
- Invoking `yt-dlp` for metadata extraction (`--dump-json`)
- Normalizing raw yt-dlp JSON into `MediaInfo` / `MediaFormat`

This is the only module that knows about yt-dlp.

### `core:downloader`
Download orchestration: queue management, progress parsing, WorkManager workers,
foreground service notifications, and retry policy.

### `core:data`
Repository implementations. Combines `core:database`, `core:datastore`,
`core:engine`, and `core:downloader` into the single API surface consumed by
feature modules.

### `core:designsystem`
Material 3 theme, color schemes, typography, reusable composables, and
responsive layout helpers (`rememberZdonWindowSizeClass`, `isExpandedWidth`).

### `feature:*`
One module per screen. Each contains its Compose UI, a `ViewModel`, and a
UI-state model. Feature modules depend on `core:data` and `core:designsystem`
but never on each other.

## Data flow

```
User action
    │
    ▼
Composable ──(event)──▶ ViewModel
                            │
                            ▼
                        Repository (core:data)
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   Room (history)     DataStore (prefs)   Engine (yt-dlp)
        │                   │                   │
        └───────────────────┴───────────────────┘
                            │
                     Flow<DomainModel>
                            │
                            ▼
                    StateFlow<UiState>
                            │
                            ▼
                       Composable
```

- Reads are exposed as cold `Flow`s from repositories and collected as
  `StateFlow` in ViewModels via `stateIn`.
- UI observes with `collectAsStateWithLifecycle()`.
- Writes are suspend functions invoked from `viewModelScope`.

## Download lifecycle

1. **Analyze** — `core:engine` runs `yt-dlp --dump-json` and returns `MediaInfo`.
2. **Enqueue** — a `DownloadItem` row is inserted into Room with status `QUEUED`.
3. **Execute** — `core:downloader` starts a WorkManager worker which invokes
   `yt-dlp` with the selected format.
4. **Progress** — stdout is parsed and the Room row is updated in place, so the
   UI, notification, and foreground service all observe one source of truth.
5. **Complete** — status becomes `COMPLETED` and a `HistoryEntry` is written.

Because progress is persisted in Room rather than held in memory, the queue
survives process death. `DownloadRepository.recoverAfterProcessDeath()` runs on
app start to reconcile interrupted downloads.

## Native payload

`yt-dlp` requires a Python interpreter. `youtubedl-android` ships a per-ABI
Python runtime and FFmpeg as native libraries, which are extracted at first run
into the app's private `no_backup` directory and executed as child processes.

Consequences for the build:

- `useLegacyPackaging = true` for `jniLibs` — the payloads must exist as real
  files on disk to be executable, so they cannot be compressed in the APK.
- ABI splits are enabled: each architecture carries its own Python and FFmpeg,
  so a universal APK is roughly four times the size of a single-ABI APK.
- R8 must not obfuscate Apache Commons Compress, which `ZipUtils` uses
  reflectively during extraction. See `app/proguard-rules.pro`.

## Dependency injection

Hilt is used throughout. Convention plugin `zdon.android.hilt` applies the
plugin and KSP processor consistently.

- `@Singleton` for repositories and the engine
- `@HiltViewModel` for all ViewModels
- `@ApplicationScope` `CoroutineScope` for work that must outlive a ViewModel
- `HiltWorkerFactory` for WorkManager workers

## Build logic

Shared Gradle configuration lives in the `build-logic` included build as
convention plugins:

| Plugin | Purpose |
| --- | --- |
| `zdon.android.application` | Application module defaults |
| `zdon.android.application.compose` | Compose for the app module |
| `zdon.android.library` | Library module defaults |
| `zdon.android.library.compose` | Compose for library modules |
| `zdon.android.feature` | Feature module conventions (Hilt + Compose + navigation) |
| `zdon.android.hilt` | Hilt plugin and KSP processor |
| `zdon.android.room` | Room plugin and schema location |
| `zdon.jvm.library` | Pure Kotlin module defaults |

Adding a new module means applying the relevant convention plugin rather than
duplicating configuration.

## Adding a new feature module

1. Create `feature/<name>/build.gradle.kts` applying `zdon.android.feature`.
2. Register it in `settings.gradle.kts`.
3. Add the dependency to `app/build.gradle.kts`.
4. Add a route to the navigation graph in `app`.

No changes to `core` modules should be required for a purely additive screen.
