package dev.widgetlab.core

enum class Collection(val title: String, val subtitle: String) {
    RACING("Motorsport", "The grid, the circuit, the weekend."),
    MUSIC("Listening room", "Eight ways to see your sound."),
    CLOCKS("Time studies", "Fifteen expressions of the present."),
    PHOTO("Personal gallery", "Your photographs, thoughtfully framed."),
    ESSENTIALS("Daily instruments", "Quietly useful. Entirely on device."),
    LAUNCHERS("Home architecture", "Give your home screen a structure.")
}

/** IDs are stable persistence keys. Provider mapping is explicit, never ordinal based. */
enum class WidgetKind(val title: String, val collection: Collection, val size: String, val height: Int, val description: String) {
    DRIVER("Driver / editorial",Collection.RACING,"4 × 2–4",260,"Portrait-led, asymmetric driver identity with season-specific points and position."),
    MINIMAL_DRIVER("Driver / paper",Collection.RACING,"2 × 2–4 × 3",240,"A huge racing number, a quiet portrait inset and a clean paper composition."),
    CONSTRUCTOR("Constructor / works",Collection.RACING,"4 × 2–4",260,"A team-colored works panel with personal vehicle artwork and season-aware participants."),
    DRIVER_PODIUM("Drivers / podium",Collection.RACING,"3 × 2–4 × 4",260,"Three sculpted championship columns with a deliberate first-place hierarchy."),
    DRIVER_STANDINGS("Drivers / classification",Collection.RACING,"3 × 2–4 × 5",340,"A precise classification table; visible rows adapt to available height."),
    TEAM_PODIUM("Constructors / podium",Collection.RACING,"3 × 2–4 × 4",260,"A proportional, team-colored championship comparison of the leading three teams."),
    CONSTRUCTOR_STANDINGS("Constructors / classification",Collection.RACING,"3 × 2–4 × 5",340,"Constructor standings with aligned points and season-specific team identity."),
    WEEKEND("Monte Carlo / calendar",Collection.RACING,"3 × 2–4 × 5",370,"Warm paper, oversized location, national flag, available circuit outline and local session times."),
    WEEKEND_EDITORIAL("Weekend / race control",Collection.RACING,"3 × 2–4 × 5",340,"A dark timing-board composition with a highlighted race and structured sprint sessions."),
    COUNTDOWN("Next / lights out",Collection.RACING,"3 × 1–4 × 3",220,"Oversized days and hours until the next race. Approximate periodic refresh, without seconds."),
    RESULTS("Results / chequered",Collection.RACING,"3 × 2–4 × 4",290,"The latest completed Grand Prix, led by its winner and a compact podium."),
    CIRCUIT("Circuit / round file",Collection.RACING,"2 × 2–4 × 4",300,"A technical circuit identity with round, country and race date. Unknown maps are never invented."),
    COMPACT("Now / compact",Collection.MUSIC,"3 × 1–4 × 2",112,"A practical artwork strip with genuine session controls."),
    EDITORIAL("Record / editorial",Collection.MUSIC,"3 × 2–4 × 4",300,"Large cover artwork and condensed track typography in an editorial grid."),
    GLASS("Glass / listening",Collection.MUSIC,"3 × 2–4 × 3",230,"Artwork-tinted translucent layers and precise transport controls. No live wallpaper blur."),
    VINYL("Vinyl / studio",Collection.MUSIC,"3 × 2–4 × 4",350,"A precisely machined record, circular cover label and minimal playback strip."),
    BRUTALIST("Audio / brutal",Collection.MUSIC,"3 × 2–4 × 3",240,"Hard divisions, extreme scale and a black-and-white audio composition."),
    MATRIX("Signal / dot matrix",Collection.MUSIC,"3 × 2–4 × 3",220,"Dotted display typography, small cover art and restrained technical metadata."),
    CASSETTE("Tape / archive",Collection.MUSIC,"3 × 2–4 × 4",300,"A precision cassette label and reel window, with genuine transport controls."),
    ARTWORK("Cover / full bleed",Collection.MUSIC,"2 × 2–4 × 4",370,"An artwork-first player with a dark contrast scrim and compact controls."),
    CLOCK("Time / air",Collection.CLOCKS,"2 × 1–4 × 3",190,"Floating contemporary numerals. Transparent by default."),
    THIN("Time / hairline",Collection.CLOCKS,"2 × 1–4 × 3",220,"Light-weight editorial time with a restrained dateline."),
    SWISS("Time / Swiss",Collection.CLOCKS,"2 × 2–4 × 4",250,"A confident Swiss composition with an accent rule and oversized numerals."),
    CINEMATIC("Time / cinema",Collection.CLOCKS,"3 × 1–4 × 3",210,"Condensed title-scale numerals, generous tracking and a quiet date."),
    GLASS_CLOCK("Time / glass",Collection.CLOCKS,"2 × 2–4 × 3",240,"An optically quiet glass-inspired surface with large floating time."),
    BRUTAL_CLOCK("Time / block",Collection.CLOCKS,"2 × 2–4 × 4",260,"A hard-edged two-zone clock with heavy stacked type."),
    DIGITAL("Time / quartz",Collection.CLOCKS,"2 × 1–4 × 3",180,"Technical digital numerals with small clock information."),
    DOT_CLOCK("Time / pixels",Collection.CLOCKS,"2 × 1–4 × 3",180,"A true dotted font and restrained monochrome styling."),
    SPLIT("Time / diptych",Collection.CLOCKS,"3 × 2–4 × 4",250,"Time and calendar occupy separate zones of a divided composition."),
    VERTICAL("Time / vertical",Collection.CLOCKS,"2 × 3–3 × 5",460,"Hours over minutes, designed for tall spaces."),
    CORNER("Time / corner",Collection.CLOCKS,"2 × 1–3 × 2",120,"Small, aligned wallpaper typography with a compact date."),
    DUAL("Time / elsewhere",Collection.CLOCKS,"3 × 2–4 × 3",220,"Two independently selected IANA timezones in a balanced twin display."),
    ANALOG("Time / dial",Collection.CLOCKS,"2 × 2–4 × 4",360,"A native Android analog clock with original dial and hand vectors."),
    SERIF("Time / maison",Collection.CLOCKS,"2 × 2–4 × 4",250,"Luxury serif numerals paired with small sans-serif calendar type."),
    TERMINAL("Time / terminal",Collection.CLOCKS,"3 × 1–4 × 3",210,"Monospaced numerals and a restrained system-terminal structure."),
    PHOTO("Photo / edge",Collection.PHOTO,"2 × 2–4 × 4",360,"An image-only frame with precise focal cropping."),
    FRAME("Photo / aperture",Collection.PHOTO,"2 × 2–4 × 4",360,"A rounded photograph, inset frame and optional caption."),
    POLAROID("Photo / print",Collection.PHOTO,"2 × 2–4 × 4",410,"A warm photographic print with an optional handwritten-era serif caption."),
    FILMSTRIP("Photo / sequence",Collection.PHOTO,"3 × 1–4 × 3",200,"Multiple chosen photographs in a cinematic, sprocket-edged strip."),
    COLLAGE("Photo / contact",Collection.PHOTO,"3 × 2–4 × 4",350,"A large anchor photograph with two supporting images; reflows when tall."),
    BATTERY("Battery / air",Collection.ESSENTIALS,"2 × 1–3 × 2",145,"Transparent charge percentage and a minimal real battery indicator."),
    BATTERY_EDITORIAL("Battery / reserve",Collection.ESSENTIALS,"2 × 2–4 × 3",250,"A precise energy gauge, actual percentage and charging state."),
    FOCUS("Focus / interruption",Collection.ESSENTIALS,"2 × 1–4 × 2",175,"Actual Android interruption-filter status. Opens Android DND settings."),
    DATE("Date / agenda",Collection.ESSENTIALS,"2 × 2–4 × 4",270,"A native date display with optional upcoming local calendar events."),
    TODAY("Today / briefing",Collection.ESSENTIALS,"3 × 2–4 × 4",320,"Native time, date and a private calendar briefing."),
    LABEL("Label / folder",Collection.LAUNCHERS,"2 × 1–4 × 2",105,"A configurable typographic folder label. This does not create a native launcher folder."),
    STRIP("Launch / strip",Collection.LAUNCHERS,"3 × 1–4 × 2",115,"A transparent row of chosen installed applications with real launch targets."),
    LAUNCHER("Launch / editorial",Collection.LAUNCHERS,"3 × 2–4 × 3",240,"A large section name and a grid of chosen application shortcuts."),
    GRID("Launch / index",Collection.LAUNCHERS,"2 × 2–4 × 4",260,"A deliberately spaced icon grid with configurable captions."),
    SECTION("Label / section",Collection.LAUNCHERS,"2 × 1–4 × 2",115,"A compact editorial divider with a customizable section heading.");
    val isMusic get() = collection==Collection.MUSIC
    val isRacing get() = collection==Collection.RACING
    val isClock get() = collection==Collection.CLOCKS
    val isPhoto get() = collection==Collection.PHOTO
    val hasAgenda get() = this==DATE || this==TODAY
    val hasApps get() = this==STRIP || this==LAUNCHER || this==GRID
    val hasLabel get() = this==LABEL || this==SECTION || this==LAUNCHER
    val hasPortrait get() = this==DRIVER || this==MINIMAL_DRIVER || this==CONSTRUCTOR
    val defaultLight get() = this in setOf(MINIMAL_DRIVER,WEEKEND,VINYL,POLAROID,FRAME)
    val transparentDefault get() = (isClock && this !in setOf(GLASS_CLOCK,BRUTAL_CLOCK,TERMINAL)) || this in setOf(PHOTO,BATTERY,FOCUS,LABEL,SECTION,STRIP,GRID)
}
