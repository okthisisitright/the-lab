package dev.widgetlab.core

enum class Collection(val title: String, val subtitle: String) {
    RACING("Racing", "The art of the race weekend."),
    MUSIC("Music", "Your sound. A different form."),
    ESSENTIALS("Essentials", "Everyday, considered."),
    EXPERIMENTAL("Experimental", "Small studies in visual rhythm.")
}

enum class WidgetKind(val title: String, val collection: Collection, val size: String, val height: Int, val description: String) {
    DRIVER("Driver spotlight", Collection.RACING, "4 × 2", 240, "A driver, a number, a season. Select a driver from the cached championship and add your own portrait."),
    CONSTRUCTOR("Constructor spotlight", Collection.RACING, "4 × 2", 240, "Team identity, championship points and season-aware driver pairings. Optional personal car artwork."),
    DRIVER_STANDINGS("Driver standings", Collection.RACING, "4 × 3", 300, "Three, five or up to ten drivers, with team accents and the latest cached points."),
    CONSTRUCTOR_STANDINGS("Constructor standings", Collection.RACING, "4 × 3", 300, "A compact championship table with meaningful team indicators."),
    WEEKEND("Race weekend", Collection.RACING, "4 × 3", 330, "An editorial session timetable. All available practice, sprint, qualifying and race sessions in your timezone."),
    COUNTDOWN("Next lights out", Collection.RACING, "4 × 2", 240, "The next race, measured in days and hours. Refreshed periodically; never an artificial live timer."),
    RESULTS("Last chequered flag", Collection.RACING, "4 × 3", 300, "The latest completed Grand Prix and its classified finishers."),
    EDITORIAL("Editorial player", Collection.MUSIC, "4 × 2", 240, "Album art and type, with supported media-session playback controls."),
    GLASS("Soft glass", Collection.MUSIC, "4 × 2", 240, "A frosted illustration built from album colors. No live wallpaper blur is required."),
    VINYL("Vinyl deck", Collection.MUSIC, "4 × 3", 340, "Grooves, a tonearm and circular artwork. A still-life interpretation of your music."),
    BRUTALIST("Brutalist audio", Collection.MUSIC, "4 × 2", 240, "Oversized type. A hard grid. A clear signal."),
    MATRIX("Dot matrix", Collection.MUSIC, "4 × 2", 220, "A compact monochrome listening terminal with a measured dot grid."),
    CASSETTE("Tape study", Collection.MUSIC, "4 × 3", 280, "Warm tape, two reels and a handwritten-era label. Real media controls underneath."),
    COMPACT("Now playing / mini", Collection.MUSIC, "4 × 1", 110, "A practical strip of artwork, title, artist and transport controls."),
    CLOCK("Editorial clock", Collection.ESSENTIALS, "4 × 2", 220, "Android TextClock keeps the time accurate. Choose a timezone and a 12- or 24-hour display."),
    DATE("Date / essential", Collection.ESSENTIALS, "3 × 2", 210, "A strong date composition. Native TextClock keeps the home-screen date current."),
    BATTERY("Charge state", Collection.ESSENTIALS, "3 × 2", 210, "Real battery level and charge state. Periodic refresh avoids a permanent background service."),
    AGENDA("Private agenda", Collection.ESSENTIALS, "4 × 3", 300, "Upcoming device-calendar events. Read-only calendar access is requested only here; details stay on this device."),
    SWISS("Swiss / time study", Collection.EXPERIMENTAL, "4 × 2", 220, "A typographic clock in paper, ink and one considered accent.")
    ;
    val isMusic get() = collection == Collection.MUSIC
    val isRacing get() = collection == Collection.RACING
    val isClock get() = this == CLOCK || this == SWISS
}
