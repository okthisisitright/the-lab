# Architecture

One Android application module, with package boundaries ready for extraction when there is a real second consumer:

- `core`: design catalogue, immutable per-instance profiles, defensive JSON migration/defaults, app DataStore preferences, private artwork paths.
- `data`: series-aware racing models, provider interface, Jolpica transport/parser, per-feed atomic snapshot storage, historical examples, read-only calendar and battery providers.
- `media`: notification-listener lifecycle, active-session discovery, metadata/capability snapshots and transport dispatch. Only session metadata is kept, in memory.
- `widgets`: 19 explicit providers, shared Canvas design renderer, native RemoteViews controls/TextClocks, pin callback, ID migration, WorkManager refresh and system-event recovery.
- `ui`: Compose gallery, detail, customization, installed manager, permissions and diagnostics.

## Rendering

Canvas uses a 420-unit design grid; the same renderer produces Compose preview images and provider surfaces. Native transport targets overlay the music artwork; native TextClocks overlay clock/date backgrounds. Views have stable IDs and content descriptions. Clock/date preview text uses equivalent data but native TextClock glyph metrics can differ slightly. No WebViews, image-only fake transport controls, permanent background animation, services for second ticks or simulated progress.

Bitmap output is capped at 720 px wide and 420,000 pixels (1.68 MB raw ARGB per instance). Imported images are decoded to 1024 px maximum; session art is bounded at 512 px. Widget updates are serialized and media-triggered updates conflated. Provider-specific broadcasts redraw only the affected IDs, avoiding repeated full-library redraws when several widgets bind together. The launcher’s current dimensions determine bitmap resolution; the composition scales to fit a declared rectangle. Text uses measured ellipsis, row height adapts to entry count, long labels cannot cross their columns. Artwork keeps its crop position. No full-resolution photographs cross a widget binder boundary.

## Persistence and lifecycle

Profiles use durable, synchronous SharedPreferences commits before returning configuration success. The relatively small profile records do not justify a database. Preferences use DataStore for the application’s automatic-sync setting. Structured feed payloads use Android AtomicFile to survive interrupted writes. Corrupt/missing caches fall back to clearly labeled samples, with per-feed errors retained.

Configuration starts with `RESULT_CANCELED`; returning without Save does not install a half-configured picker widget. Native `requestPinAppWidget` uses a short-lived stored profile and an explicit mutable callback because the system fills in `EXTRA_APPWIDGET_ID`. The callback validates the provider kind and preserves configuration already saved by a launcher configure flow. The pending profile expires after five minutes for configure reuse. New pin requests replace canceled ones. `onRestored` migrates old/new IDs. Deletion removes the associated profile. Backup is disabled to avoid restoring stale IDs/private files onto a different device.

WorkManager owns durable periodic jobs. Widget update broadcasts and boot/package/timezone/power events redraw local state. The notification listener reconnects through Android’s supported lifecycle. Music permission revocation clears session state; controls recheck access. Widget providers render cached data without network dependency. Provider update failures are isolated so another instance can still update.

## Data semantics

Every standings group preserves provider season/round. Driver records can reference multiple teams in one season; those are represented as a list. Constructor driver abbreviations require matching seasons and matching sample/live provenance. Results are separate completed-event objects; an upcoming event never becomes a fabricated result. Calendar sessions hold nullable instants, retaining TBA information instead of assuming midnight. `series` is explicit and the provider is replaceable. No advanced multi-series rules are speculatively implemented.

## Boundaries and expansion

A future motorsport app can extract `data` and the racing design models. A future Room store is appropriate if full multi-season querying appears; this release needs only four latest snapshots. Additional motorsport series need their own providers/session names rather than modifying API-specific parsing in UI. There is no advertising, monetization, login, cloud storage or separate motorsport application.
