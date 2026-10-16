# Third-party notices

## Space Grotesk

Space Grotesk, copyright 2018 The Space Grotesk Project Authors; design by Florian Karsten. SIL Open Font License 1.1, included in [SPACE-GROTESK-LICENSE.txt](SPACE-GROTESK-LICENSE.txt). Font obtained from https://github.com/google/fonts/tree/main/ofl/spacegrotesk . Font file is bundled unchanged.

## Hamilton portrait

“Lewis Hamilton 2021.jpg”, **Show do Esporte**, via Wikimedia Commons:
https://commons.wikimedia.org/wiki/File:Lewis_Hamilton_2021.jpg

Original: https://upload.wikimedia.org/wikipedia/commons/2/21/Lewis_Hamilton_2021.jpg

License: **Creative Commons Attribution 3.0 Unported**, https://creativecommons.org/licenses/by/3.0/ . Changes: cropped to face, resized and JPEG-compressed. This is an archival 2021 photograph, not evidence of current team affiliation. Attribution is also accessible in Settings → Credits. The photograph license does not imply endorsement by the subject, source, racing teams or sponsors.

## Racing data

Jolpica-F1, successor to the Ergast-compatible public racing API:
https://api.jolpi.ca/ergast/f1/
https://github.com/jolpica/jolpica-f1

The data is **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International**, https://creativecommons.org/licenses/by-nc-sa/4.0/ . Attribution: Jolpica-F1 / Ergast. API responses are reformatted into widget layouts and cached privately. The recorded test fixtures retain this data license. The historical sample is labeled and reformatted. This private, noncommercial app matches the stated use terms. A copy of the provider terms is included in [JOLPICA-TERMS.md](JOLPICA-TERMS.md); current terms: https://github.com/jolpica/jolpica-f1/blob/main/TERMS.md . Published limits checked during development: 4 requests/second and 500/hour; the app normally makes four sequential calls every six hours.

Source code licensing of a provider does not grant rights to unrelated racing trademarks, photography or logos. Formula 1 names, driver/team names and event names identify factual data. Widget Lab is independent and unaffiliated. No access token is required by the verified endpoints. Provider service, terms, limits and coverage can change.

## Software dependencies

Kotlin and kotlinx.coroutines (JetBrains), AndroidX Compose, Activity, Lifecycle, DataStore, WorkManager (Android Open Source Project): Apache License 2.0, https://www.apache.org/licenses/LICENSE-2.0 . JUnit: EPL 1.0 (tests only). Robolectric: MIT (tests only). AndroidX Test: Apache 2.0 (tests only). org.json: JSON License (test tooling only); Android platform JSON APIs are used in the application. Gradle: Apache 2.0.

Reference screenshots supplied in the conversation are used as art direction. They are not bundled in the application. Fallback graphics are original Canvas drawings. Circuit diagrams use credited MIT-licensed coordinate data.


## Edition II additions

Inter, Barlow Condensed, Bodoni Moda, Rajdhani, IBM Plex Mono and Doto are bundled unchanged under SIL OFL 1.1. Complete individual notices are in app/src/main/assets/notices and the app credits. Gallery-only, explicitly labeled NASA Apollo 15/17 photographic studies are U.S. government images from NASA Image and Video Library. They are not substituted for actual media artwork or personal photos in installed widgets. See the bundled THIRD-PARTY-NOTICES.txt for identifiers and usage policy.


## Circuit coordinates

33 offline outlines derived from [Tomislav Bacinger’s F1 circuits GeoJSON](https://github.com/bacinger/f1-circuits), MIT license, pinned commit 394d8fbe70ef2c0b0c8d23ff7bee61fa09606055. Full license is bundled as CIRCUITS-MIT.txt. Local projection and rotation preserve shape for illustrative diagrams; these are unofficial outlines and not live telemetry. All 23 circuits in the checked 2026 Jolpica calendar have matching IDs.

Google Sans, copyright 2025 The Google Sans Project Authors (https://github.com/googlefonts/googlesans), is bundled under SIL OFL 1.1. Full license: app/src/main/assets/notices/GOOGLE-SANS-OFL.txt. Source: https://github.com/google/fonts/tree/main/ofl/googlesans . This build subsets the original variable font to Latin, Cyrillic and common punctuation/symbols for application UI size; no reserved font names are declared in its license header. The widget collection retains its other credited display faces.
