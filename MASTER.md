# Workshop Manual Master

This repository is the canonical **Master / Workshop Manual** product.

## Source repos reviewed

- `WorkshopManualOrganiser` - canonical base. This is the most complete and technically honest implementation.
- `Workshop-manual-organiser-` - older AI Studio-style prototype. It contains useful UI ideas, but also hard-coded demo/manual data, fake VIN results, mock diagram hotspots, and simulated AI responses. Those parts are deliberately **not** copied into Master.
- `Workshop-Manuals` - lightweight/empty organiser prototype. No implementation was preferred over the canonical base.

## Master rule

Existing repositories are source/salvage material. A feature is only considered part of Master when it is implemented in the canonical app and verified by build/runtime tests.

## Current real functionality

- Local persistent manual library
- Camera capture/import
- PDF and image import
- Manual/page editing
- Page gallery and zoom viewer
- Export/import backup ZIP
- Offline VIN decoding
- Offline wiring reference
- Offline technical knowledge base
- Real local Ollama HTTP client
- AI sort/classification
- Android unit tests
- Android instrumentation smoke tests

## Verification bar

A green Gradle build alone is not enough. Master is considered finished only after:

1. debug APK builds successfully;
2. unit tests pass;
3. instrumentation tests actually execute, rather than reporting zero tests;
4. the APK is installed and exercised on a real Android phone;
5. core workflows are verified on-device;
6. no advertised feature is implemented with a placeholder, canned result, or fake server.

## Known verification history

The previous CI run successfully completed unit tests and debug APK assembly, but the Android instrumentation job failed before executing the tests because the emulator/instrumentation process crashed. The emulator log included a ColorBuffer failure and reported **0 tests executed**. The CI emulator configuration has therefore been hardened before the next verification run.

## Core product scope

The Workshop Manual Master is intended to provide:

- phone-first manual library;
- manual scanning/import;
- durable offline library storage;
- cross-library search/indexing;
- source/page/section-aware technical answers;
- wiring references;
- VIN decoding;
- local Ollama assistance;
- optional voice navigation/read-aloud with explicit user control;
- safe external opening warnings;
- reliable navigation, back/resume and state persistence.

Anything not actually wired and tested remains unfinished.
