# Workshop Manual Organiser

Scan, import and organise workshop manuals; decode VINs offline; browse a wiring
reference; and auto-classify a library. Everything is stored on the device —
there is no account, no analytics, no advertising and no tracker.

`applicationId: com.workshop.manualorganiser` · minSdk 24 · targetSdk 35 ·
Kotlin 2.0 · Jetpack Compose · Material 3

## Features

| Feature | What it actually does |
|---|---|
| **Scan Manual** | Opens the system camera, imports the captured page and creates a manual from it. |
| **Upload** | Imports a PDF (rasterised page-by-page with `PdfRenderer`) or an image into a new manual. |
| **Add Manual** | Creates a manual with a title, category, notes, VIN and any number of captured/imported pages. |
| **Open / Detail** | Full-screen page gallery with a pinch-to-zoom, double-tap-to-reset viewer. |
| **Edit Structure** | Rename, re-categorise, reorder (up/down), label and delete pages. |
| **Export** | Writes a real backup ZIP (`manual.json` + every page file) through the Storage Access Framework, or shares it directly. Opening an exported ZIP restores the manual. |
| **Delete** | Removes the manual and its stored pages, with confirmation. |
| **VIN Decoder** | Offline ISO 3779/3780 decoder: region, country, manufacturer (WMI table), model year, plant, serial and check-digit validation. Attach a decoded VIN to any manual. |
| **Wiring Diagrams** | Offline reference: 10 systems with test procedures, 8 standard pinouts (OBD-II, ISO relays, 12N trailer, injector, COP, MAF, O2), DIN wire colours and DIN 72552 terminals. |
| **AI Sort & Structure** | Transparent keyword taxonomy classifier — reports the category, a confidence value, the matched evidence and a proposed section order, then applies it. |
| **AI Technical Help** | Offline knowledge base of fault codes and symptoms with cause/test ordering. Optionally enriched by an AI endpoint you configure. |

## Build

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests (VIN decoder + taxonomy)
./gradlew assembleRelease      # minified, signed release APK/AAB
```

Requires JDK 17 and Android SDK platform 35 / build-tools 35.0.0.

### Release signing

Copy `keystore.properties.example` to `keystore.properties` (git-ignored) and fill
it in. Without that file the release build falls back to the debug keystore so it
still produces an installable APK.

```properties
storeFile=keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

### Optional AI endpoint

The app is fully functional offline. To let AI Technical Help also query an LLM
endpoint, pass credentials at build time so nothing lands in source control:

```bash
./gradlew assembleDebug -Pai.endpoint=http://127.0.0.1:11434/api/chat -Pai.model=llama3.2:1b
```

Only loopback HTTP for the local Ollama endpoint is permitted by the network security config; all other cleartext HTTP is blocked.

## Architecture

```
data/    Manual, ManualPage, ManualCodec (JSON schema), ManualRepository (durable store)
ui/      WorkshopApp (navigation + overflow menu), one screen per feature, WorkshopViewModel
util/    VinDecoder, Taxonomy, WiringData, KnowledgeBase, MediaImporter, Exporter, AiClient
```

- **Persistence** — the library is written atomically to `filesDir/manuals.json`
  (temp file + rename) and exposed as a `StateFlow`. Page files live in
  `filesDir/library/`.
- **No storage permission** — content is copied out of the picker/camera URI into
  app-private storage immediately, so the app uses the system Photo Picker and
  `OpenDocument` instead of legacy storage permissions.
- **Privacy** — cloud backup excludes user documents (see `res/xml/backup_rules.xml`
  and `res/xml/data_extraction_rules.xml`); device-to-device transfer is allowed.

## Tests

`app/src/test/java/…/VinDecoderTest.kt` covers the check-digit algorithm, the
model-year cycle rollover, region/manufacturer decoding and rejection of I/O/Q.
`TaxonomyTest.kt` covers classification, confidence, fallback and page reordering.

## License

Proprietary — built by REDRUM Studios.
