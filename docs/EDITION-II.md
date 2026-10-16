# Edition II / Widget Lab 0.2.0

This rebuild replaces the fixed-size stretching renderer and prior app interface. The catalog contains exactly 50 registered Android providers: 12 motorsport, 8 music, 15 clocks, 5 photography, 5 essentials, 5 launcher/label designs.

## Visual and rendering system

Canvas compositions are calculated in the actual widget width/height, with a single uniform raster scale. Compact layouts reduce secondary information; portrait layouts relocate images; standings limit rows to readable spacing. Native TextClock layers use the same ClockDesign geometry and bundled fonts as previews. Text clocks use Android's clock lifecycle; the analog design uses Android AnalogClock. The latter supports a selected timezone on API 31+, device timezone on API 29–30. Continuous bitmap animation and seconds are intentionally absent.

Android 12+ supplies responsive RemoteViews size maps from launcher-provided sizes. Older Android uses separately rendered portrait and landscape RemoteViews. Each update shares a 420,000-pixel bitmap budget across the size variants. Media transport and app targets remain native interactive views, not painted buttons. Media changes only request music-widget redraws. Fonts and artwork are cached within bounded process memory.

Six surface modes: transparent, solid, translucent, tint, illustrated glass, outline. Most clocks and home organization designs default to transparency. Glass is a supported visual approximation, not live wallpaper refraction. Preview wallpaper is separate from the widget bitmap.

Inter, Barlow Condensed, Bodoni Moda, Rajdhani, IBM Plex Mono and Doto provide six distinct typographic languages. Font-specific RemoteViews children preserve the actual font in the launcher process. Full OFL licenses are bundled. Font, background and artwork controls are offered in design settings; locked physical frame/music artwork elements remain intentional parts of those designs.

## Images and personal organization

Android's single/multiple photo picker grants access to selected images. ImageDecoder handles orientation and bounds imports to 1024 pixels; copies are stored privately, so they survive restart without temporary URI permissions. Center, top, bottom, left and right focal alignments are supported. Filmstrip/contact layouts retain up to six selections, displaying a size-appropriate composition; collage uses the first three. No broad storage permission is requested.

Gallery-only photographic studies are explicitly labeled. NASA Apollo photographs illustrate music and photo layouts when no personal content/session exists; sample track metadata is labeled. Installed music widgets always receive actual media-session data. Photo widgets always use chosen private images or an honest empty state. These examples are not fake playback, and their controls are disabled.

Launcher designs query only MAIN/LAUNCHER-visible applications. Selected activity components, labels and icons persist per widget. Targets use explicit launch intents; missing apps open configuration. Small layouts show as many targets as fit, with extra selections visible after expansion. Labels and shortcut widgets do not create native launcher folders.

Focus reads Android's actual notification interruption filter, including unknown/unavailable state; it opens Android DND settings with a sound-settings fallback. It does not invent a proprietary Vivo focus state and does not request unrelated permissions.

## Racing and media

The existing provider abstraction, independent feed caches, last-successful timestamps, structured sprint sessions, IANA timezone conversion and labeled 2024 samples are retained. Public Jolpica coverage is checked again during the rebuild. Driver/constructor pairings are limited to matching season/data provenance. User-supplied portraits and vehicle art are supported; the bundled Hamilton portrait is explicitly archival.

Music controls target the chosen compatible session and respect published capability flags. Artwork/metadata/playback changes are event-driven. Empty, missing-art, revoked-access and disconnected states remain explicit. Third-party player compatibility requires testing on that actual player.

## Upgrade and acceptance

VersionCode is incremented to 2. Existing provider class names are retained where their design continues; the previous separate Agenda provider is replaced by Date / agenda and Today / briefing. Re-add an old agenda instance if your launcher removes it. Existing profiles retain their old settings unless edited; a new instance adopts Edition II transparency defaults.

Physical Vivo/Android 15 resize, restart, permission and music-player testing remains necessary even when compilation and automated host tests pass. The catalog, image pipeline, native layouts and multi-size rendering are covered by local/runtime tests; GitHub device tests separately exercise Android's widget host, media session callbacks and studio navigation. Validation results are recorded after execution, not inferred from compilation.
