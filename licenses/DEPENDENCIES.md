# Dependencies and their licences

Every library that ends up in the APK, checked on 2026-09-20 against the
resolved `viwoodsReleaseRuntimeClasspath`. The rule is in `plan.md` section
4: no GPL and no AGPL. There is none.

To check again after a dependency change:

```
./gradlew :app:dependencies --configuration viwoodsReleaseRuntimeClasspath
```

## In every build

| Library | Used for | Licence |
| --- | --- | --- |
| AndroidX (core, activity, lifecycle, datastore, annotation, fragment, and what they bring) | The Android support libraries | Apache-2.0 |
| Jetpack Compose (foundation, ui, runtime). **Not** Compose Material | The screens | Apache-2.0 |
| Kotlin standard library | The language | Apache-2.0 |
| kotlinx.coroutines | Background work | Apache-2.0 |
| kotlinx.serialization and kotlinx-datetime (runtime only, brought by Readium) | Readium's own data | Apache-2.0 |
| Readium Kotlin Toolkit 3.4.0: shared, streamer, navigator | The EPUB reader | BSD-3-Clause |
| Guava, and `failureaccess` | Brought by AndroidX and Readium | Apache-2.0 |
| Okio | Brought by Readium | Apache-2.0 |
| Timber | Brought by Readium | Apache-2.0 |
| koi (`com.mcxiaoke.koi:core`) | Brought by Readium | Apache-2.0 |
| JSpecify | Annotations, brought by Guava | Apache-2.0 |
| jsoup | HTML parsing, brought by Readium | MIT |
| `desugar_jdk_libs` | Newer `java.*` classes on older Android, asked for by Readium | GPL-2.0 **with the Classpath Exception** (see below) |

### About `desugar_jdk_libs`

It is a cut of OpenJDK, so its licence is GPL-2.0 with the Classpath
Exception. The exception is the point: it says that linking this library
into a program does not put the program under the GPL. It is the same
licence the Java class library itself has on every desktop, and Google ships
it for exactly this use. It does not make this app GPL, and it is not what
plan section 4 rules out. If the owner wants none of it anyway, the way out
is to drop Readium, which is the only thing that needs it.

## Debug builds only

| Library | Used for | Licence |
| --- | --- | --- |
| Jetpack Ink 1.0.0 (authoring, brush, strokes, rendering) | The latency baseline on the device test screen | Apache-2.0 |
| Compose UI tooling and test manifest | Previews and tests | Apache-2.0 |

## Tests only, never in an APK

| Library | Licence |
| --- | --- |
| JUnit 4 | EPL-1.0 |
| Robolectric | MIT |
| Roborazzi | Apache-2.0 |
| kotlinx-coroutines-test, Compose UI test | Apache-2.0 |

## Platform parts used, with no library

- PDF pages are rendered by `android.graphics.pdf.PdfRenderer`, which is part
  of Android. MuPDF (AGPL) is not used. See
  `docs/decisions/0010-pdf-renderer.md`.
