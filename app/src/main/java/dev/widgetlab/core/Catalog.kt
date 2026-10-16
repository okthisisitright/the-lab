package dev.widgetlab.core

enum class Collection(val title: String, val subtitle: String) {
    RACING("Racing", "The grid, the circuit, the weekend."),
    MUSIC("Music", "Your music, beautifully in view."),
    CLOCKS("Clocks", "A little more room for the present."),
    PHOTO("Photos", "Your favourite moments, close by."),
    ESSENTIALS("Essentials", "Small things that make the day easier."),
    LAUNCHERS("Shortcuts", "Everything you need, within reach.")
}

/** IDs are stable persistence keys. Provider mapping is explicit, never ordinal based. */
enum class WidgetKind(val title: String, val collection: Collection, val size: String, val height: Int, val description: String) {
    DRIVER("Driver spotlight",Collection.RACING,"4 × 2–4",260,"Portrait-led, asymmetric driver identity with season-specific points and position."),
    MINIMAL_DRIVER("Minimal driver",Collection.RACING,"2 × 2–4 × 3",240,"A huge racing number, a quiet portrait inset and a clean paper composition."),
    CONSTRUCTOR("Team spotlight",Collection.RACING,"4 × 2–4",260,"A team-colored works panel with personal vehicle artwork and season-aware participants."),
    DRIVER_PODIUM("Driver podium",Collection.RACING,"3 × 2–4 × 4",260,"Three sculpted championship columns with a deliberate first-place hierarchy."),
    DRIVER_STANDINGS("Driver standings",Collection.RACING,"3 × 2–4 × 5",340,"A precise classification table; visible rows adapt to available height."),
    TEAM_PODIUM("Team podium",Collection.RACING,"3 × 2–4 × 4",260,"A proportional, team-colored championship comparison of the leading three teams."),
    CONSTRUCTOR_STANDINGS("Team standings",Collection.RACING,"3 × 2–4 × 5",340,"Constructor standings with aligned points and season-specific team identity."),
    WEEKEND("Race calendar",Collection.RACING,"3 × 2–4 × 5",370,"Warm paper, oversized location, national flag, available circuit outline and local session times."),
    WEEKEND_EDITORIAL("Race weekend",Collection.RACING,"3 × 2–4 × 5",340,"A dark timing-board composition with a highlighted race and structured sprint sessions."),
    COUNTDOWN("Next race",Collection.RACING,"3 × 1–4 × 3",220,"Oversized days and hours until the next race. Approximate periodic refresh, without seconds."),
    RESULTS("Race results",Collection.RACING,"3 × 2–4 × 4",290,"The latest completed Grand Prix, led by its winner and a compact podium."),
    CIRCUIT("Circuit card",Collection.RACING,"2 × 2–4 × 4",300,"A technical circuit identity with round, country and race date. Unknown maps are never invented."),
    COMPACT("Glass mini",Collection.MUSIC,"3 × 1–4 × 2",150,"A small frosted player with cover artwork, track details and playback controls."),
    EDITORIAL("Glass album",Collection.MUSIC,"2 × 2–4 × 5",400,"A large glass album player with artwork, centered playback controls and a quiet track label."),
    GLASS("Soft glass",Collection.MUSIC,"3 × 2–4 × 3",230,"A soft translucent player inspired by desktop glass, with real cover art and centered controls."),
    VINYL("Circular player",Collection.MUSIC,"2 × 2–4 × 4",340,"A circular artwork player with an inset track label and genuine transport controls."),
    BRUTALIST("Panorama player",Collection.MUSIC,"3 × 1–4 × 3",190,"A panoramic artwork player with a soft contrast overlay and compact controls."),
    MATRIX("Portrait player",Collection.MUSIC,"2 × 3–3 × 5",500,"A tall immersive player with softly diffused artwork and an airy vertical composition."),
    CASSETTE("Ambient disc",Collection.MUSIC,"2 × 2–4 × 4",340,"A round ambient player with diffused artwork and a central playback button."),
    ARTWORK("Now playing",Collection.MUSIC,"3 × 1–4 × 3",220,"A calm now-playing card: large cover, actual playback state, song and artist. Tap to open your player."),
    CLOCK("Minimal clock",Collection.CLOCKS,"2 × 1–4 × 3",190,"Floating contemporary numerals. Transparent by default."),
    THIN("Thin clock",Collection.CLOCKS,"2 × 1–4 × 3",220,"Light-weight editorial time with a restrained dateline."),
    SWISS("Swiss clock",Collection.CLOCKS,"2 × 2–4 × 4",250,"A confident Swiss composition with an accent rule and oversized numerals."),
    CINEMATIC("Cinematic clock",Collection.CLOCKS,"3 × 1–4 × 3",210,"Condensed title-scale numerals, generous tracking and a quiet date."),
    GLASS_CLOCK("Glass clock",Collection.CLOCKS,"2 × 2–4 × 3",240,"An optically quiet glass-inspired surface with large floating time."),
    BRUTAL_CLOCK("Bold clock",Collection.CLOCKS,"2 × 2–4 × 4",260,"A hard-edged two-zone clock with heavy stacked type."),
    DIGITAL("Digital clock",Collection.CLOCKS,"2 × 1–4 × 3",180,"Technical digital numerals with small clock information."),
    DOT_CLOCK("Dot clock",Collection.CLOCKS,"2 × 1–4 × 3",180,"A true dotted font and restrained monochrome styling."),
    SPLIT("Time and date",Collection.CLOCKS,"3 × 2–4 × 4",250,"Time and calendar occupy separate zones of a divided composition."),
    VERTICAL("Vertical clock",Collection.CLOCKS,"2 × 3–3 × 5",460,"Hours over minutes, designed for tall spaces."),
    CORNER("Corner clock",Collection.CLOCKS,"2 × 1–3 × 2",120,"Small, aligned wallpaper typography with a compact date."),
    DUAL("Dual clock",Collection.CLOCKS,"3 × 2–4 × 3",220,"Two independently selected IANA timezones in a balanced twin display."),
    ANALOG("Analog clock",Collection.CLOCKS,"2 × 2–4 × 4",360,"A native Android analog clock with original dial and hand vectors."),
    SERIF("Serif clock",Collection.CLOCKS,"2 × 2–4 × 4",250,"Luxury serif numerals paired with small sans-serif calendar type."),
    TERMINAL("Terminal clock",Collection.CLOCKS,"3 × 1–4 × 3",210,"Monospaced numerals and a restrained system-terminal structure."),
    PHOTO("Photo square",Collection.PHOTO,"2 × 2–4 × 4",360,"An image-only frame with precise focal cropping."),
    FRAME("Photo frame",Collection.PHOTO,"2 × 2–4 × 4",360,"A rounded photograph, inset frame and optional caption."),
    POLAROID("Photo print",Collection.PHOTO,"2 × 2–4 × 4",410,"A warm photographic print with an optional handwritten-era serif caption."),
    FILMSTRIP("Filmstrip",Collection.PHOTO,"3 × 1–4 × 3",200,"Multiple chosen photographs in a cinematic, sprocket-edged strip."),
    COLLAGE("Photo collage",Collection.PHOTO,"3 × 2–4 × 4",350,"A large anchor photograph with two supporting images; reflows when tall."),
    BATTERY("Minimal battery",Collection.ESSENTIALS,"2 × 1–3 × 2",145,"Transparent charge percentage and a minimal real battery indicator."),
    BATTERY_EDITORIAL("Battery dashboard",Collection.ESSENTIALS,"2 × 2–4 × 3",250,"A precise energy gauge, actual percentage and charging state."),
    FOCUS("Do not disturb",Collection.ESSENTIALS,"2 × 1–4 × 2",175,"Actual Android interruption-filter status. Opens Android DND settings."),
    DATE("Date and agenda",Collection.ESSENTIALS,"2 × 2–4 × 4",270,"A native date display with optional upcoming local calendar events."),
    TODAY("Today",Collection.ESSENTIALS,"3 × 2–4 × 4",320,"Native time, date and a private calendar briefing."),
    LABEL("Folder label",Collection.LAUNCHERS,"2 × 1–4 × 2",105,"A configurable typographic folder label. This does not create a native launcher folder."),
    STRIP("Quick launch",Collection.LAUNCHERS,"3 × 1–4 × 2",115,"A transparent row of chosen installed applications with real launch targets."),
    LAUNCHER("App panel",Collection.LAUNCHERS,"3 × 2–4 × 3",240,"A large section name and a grid of chosen application shortcuts."),
    GRID("Icon grid",Collection.LAUNCHERS,"2 × 2–4 × 4",260,"A deliberately spaced icon grid with configurable captions."),
    SECTION("Section header",Collection.LAUNCHERS,"2 × 1–4 × 2",115,"A compact editorial divider with a customizable section heading.");
    val isMusic get() = collection==Collection.MUSIC
    val isRacing get() = collection==Collection.RACING
    val isClock get() = collection==Collection.CLOCKS
    val isPhoto get() = collection==Collection.PHOTO
    val hasAgenda get() = this==DATE || this==TODAY
    val hasApps get() = this==STRIP || this==LAUNCHER || this==GRID
    val hasLabel get() = this==LABEL || this==SECTION || this==LAUNCHER
    val hasPortrait get() = this==DRIVER || this==MINIMAL_DRIVER || this==CONSTRUCTOR
    val defaultLight get() = this in setOf(MINIMAL_DRIVER,WEEKEND,POLAROID,FRAME)
    val transparentDefault get() = (isClock && this !in setOf(GLASS_CLOCK,BRUTAL_CLOCK,TERMINAL)) || this in setOf(VINYL,CASSETTE,PHOTO,BATTERY,FOCUS,LABEL,SECTION,STRIP,GRID)
}
