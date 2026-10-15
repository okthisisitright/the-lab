package dev.widgetlab.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate

/** Series is explicit so a future endurance provider does not inherit F1 assumptions. */
data class Competitor(val id: String, val code: String, val given: String, val family: String, val number: String, val nationality: String, val teams: List<String>, val position: Int, val points: String) {
    val name get() = "$given $family"
}
data class Team(val id: String, val name: String, val position: Int, val points: String)
data class Session(val name: String, val start: Instant?)
data class Race(val season: String, val round: Int, val name: String, val circuitId: String, val circuit: String, val location: String, val country: String, val date: LocalDate, val sessions: List<Session>) {
    val start get() = sessions.firstOrNull { it.name == "Race" }?.start
}
data class Finisher(val position: String, val code: String, val name: String, val team: String, val status: String)
data class RaceResult(val race: Race, val finishers: List<Finisher>)
data class StandingTable<T>(val season: String, val round: Int, val entries: List<T>)
data class Section<T>(val value: T, val refreshed: Instant? = null, val sample: Boolean = false, val error: String? = null) {
    fun stale(now: Instant = Instant.now()) = refreshed == null || now.epochSecond - refreshed.epochSecond > 86400
}
data class RacingSnapshot(val drivers: Section<StandingTable<Competitor>>, val teams: Section<StandingTable<Team>>, val schedule: Section<List<Race>>, val result: Section<RaceResult?>, val series: String = "Formula 1")

object RacingParser {
    private fun JSONObject.array(name: String) = optJSONArray(name) ?: JSONArray()
    private fun JSONArray.objects() = (0 until length()).mapNotNull { optJSONObject(it) }
    private fun root(raw: String) = JSONObject(raw).getJSONObject("MRData")
    fun drivers(raw: String): StandingTable<Competitor> {
        val table = root(raw).getJSONObject("StandingsTable")
        val list = table.array("StandingsLists").optJSONObject(0)
        return StandingTable(list?.optString("season") ?: table.optString("season"), list?.optInt("round") ?: 0,
            list?.array("DriverStandings")?.objects()?.map { j ->
                val d = j.getJSONObject("Driver")
                Competitor(d.getString("driverId"), d.optString("code", d.optString("familyName").take(3).uppercase()), d.optString("givenName"), d.optString("familyName"), d.optString("permanentNumber", "—"), d.optString("nationality"), j.array("Constructors").objects().map { it.getString("constructorId") }, j.optInt("position"), j.optString("points", "—"))
            } ?: emptyList())
    }
    fun teams(raw: String): StandingTable<Team> {
        val table = root(raw).getJSONObject("StandingsTable"); val list = table.array("StandingsLists").optJSONObject(0)
        return StandingTable(list?.optString("season") ?: table.optString("season"), list?.optInt("round") ?: 0,
            list?.array("ConstructorStandings")?.objects()?.map { j -> val t = j.getJSONObject("Constructor"); Team(t.getString("constructorId"), t.getString("name"), j.optInt("position"), j.optString("points", "—")) } ?: emptyList())
    }
    private fun instant(date: String, time: String): Instant? = if (time.isBlank()) null else runCatching { Instant.parse("${date}T$time") }.getOrNull()
    fun race(j: JSONObject): Race {
        val c = j.getJSONObject("Circuit"); val l = c.getJSONObject("Location")
        val date = j.getString("date")
        val sessions = listOf("FirstPractice" to "FP1", "SecondPractice" to "FP2", "ThirdPractice" to "FP3", "SprintQualifying" to "Sprint Q", "SprintShootout" to "Sprint Q", "Sprint" to "Sprint", "Qualifying" to "Qualifying")
            .mapNotNull { (key, label) -> j.optJSONObject(key)?.let { Session(label, instant(it.optString("date"), it.optString("time"))) } }
            .distinctBy { it.name }.plus(Session("Race", instant(date, j.optString("time"))))
            .sortedWith(compareBy<Session> { it.start ?: Instant.MAX })
        return Race(j.optString("season"), j.optInt("round"), j.getString("raceName"), c.optString("circuitId"), c.optString("circuitName"), l.optString("locality"), l.optString("country"), LocalDate.parse(date), sessions)
    }
    fun schedule(raw: String): List<Race> = root(raw).getJSONObject("RaceTable").array("Races").objects().map(::race)
    fun result(raw: String): RaceResult? {
        val j = root(raw).getJSONObject("RaceTable").array("Races").optJSONObject(0) ?: return null
        val entries = j.array("Results").objects().map { f -> val d = f.getJSONObject("Driver"); Finisher(f.optString("positionText", f.optString("position")), d.optString("code", d.optString("familyName").take(3).uppercase()), "${d.optString("givenName")} ${d.optString("familyName")}", f.getJSONObject("Constructor").optString("constructorId"), f.optString("status")) }
        return RaceResult(race(j), entries)
    }
}

interface RacingProvider { suspend fun fetch(path: String): String }
class JolpicaProvider : RacingProvider {
    override suspend fun fetch(path: String): String = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.jolpi.ca/ergast/f1/$path?limit=100").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 15000; setRequestProperty("Accept", "application/json"); setRequestProperty("User-Agent", "WidgetLab/0.1 (private Android widgets)")
        }
        try {
            check(connection.responseCode == 200) { "Provider returned HTTP ${connection.responseCode}" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var count = input.read(buffer)
                while(count != -1) {
                    check(output.size() + count <= 1_000_000) { "Provider response too large" }
                    output.write(buffer,0,count)
                    count = input.read(buffer)
                }
                output.toByteArray()
            }
            check(bytes.size <= 1_000_000) { "Provider response too large" }
            bytes.toString(Charsets.UTF_8).also { JSONObject(it).getJSONObject("MRData") }
        } finally { connection.disconnect() }
    }
}

class RacingRepository(private val context: Context, private val provider: RacingProvider = JolpicaProvider()) {
    private val directory = File(context.filesDir, "racing-v1").apply { mkdirs() }
    companion object { private val mutex = Mutex() }
    private fun file(key: String) = AtomicFile(File(directory, "$key.json"))
    private fun <T> read(key: String, parser: (String) -> T, fallback: T): Section<T> = runCatching {
        val j = JSONObject(file(key).readFully().toString(Charsets.UTF_8))
        Section(parser(j.getString("payload")), Instant.parse(j.getString("refreshed")), error = context.getSharedPreferences("sync", 0).getString("error:$key", null))
    }.getOrElse { Section(fallback, sample = true, error = context.getSharedPreferences("sync", 0).getString("error:$key", null)) }
    fun snapshot(): RacingSnapshot = RacingSnapshot(read("drivers", RacingParser::drivers, SampleRacing.drivers), read("teams", RacingParser::teams, SampleRacing.teams), read("schedule", RacingParser::schedule, SampleRacing.schedule), read("results", RacingParser::result, SampleRacing.result))
    /** At most four small calls per six-hour background pass; explicit refresh has a one-minute cooldown. */
    suspend fun refresh(): List<String> = withContext(Dispatchers.IO) { mutex.withLock {
        val prefs = context.getSharedPreferences("sync", 0)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("attempt", 0) < 60_000) return@withLock listOf("Please wait one minute between refreshes.")
        prefs.edit().putLong("attempt", now).commit()
        val errors = mutableListOf<String>()
        for ((key, path) in listOf("drivers" to "current/driverstandings/", "teams" to "current/constructorstandings/", "schedule" to "current/", "results" to "current/last/results/")) {
            try {
                val raw = provider.fetch(path)
                when (key) { "drivers" -> RacingParser.drivers(raw); "teams" -> RacingParser.teams(raw); "schedule" -> RacingParser.schedule(raw); else -> RacingParser.result(raw) }
                val data = JSONObject().put("payload", raw).put("refreshed", Instant.now().toString()).toString().toByteArray()
                val atomic = file(key); val stream = atomic.startWrite()
                try { stream.write(data); atomic.finishWrite(stream) } catch (e: Exception) { atomic.failWrite(stream); throw e }
                prefs.edit().remove("error:$key").commit()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                val message = "$key: ${e.message?.take(140) ?: "Refresh unavailable"}"
                prefs.edit().putString("error:$key", message).commit(); errors += message
            }
        }
        errors
    } }
}

object SampleRacing {
    // Historical values after Monaco 2024. Always rendered with SAMPLE / 2024.
    val drivers = StandingTable("2024", 8, listOf(
        Competitor("max_verstappen", "VER", "Max", "Verstappen", "1", "Dutch", listOf("red_bull"), 1, "169"),
        Competitor("leclerc", "LEC", "Charles", "Leclerc", "16", "Monegasque", listOf("ferrari"), 2, "138"),
        Competitor("norris", "NOR", "Lando", "Norris", "4", "British", listOf("mclaren"), 3, "113"),
        Competitor("sainz", "SAI", "Carlos", "Sainz", "55", "Spanish", listOf("ferrari"), 4, "108"),
        Competitor("perez", "PER", "Sergio", "Pérez", "11", "Mexican", listOf("red_bull"), 5, "107"),
        Competitor("piastri", "PIA", "Oscar", "Piastri", "81", "Australian", listOf("mclaren"), 6, "71"),
        Competitor("russell", "RUS", "George", "Russell", "63", "British", listOf("mercedes"), 7, "54"),
        Competitor("hamilton", "HAM", "Lewis", "Hamilton", "44", "British", listOf("mercedes"), 8, "42")
    ))
    val teams = StandingTable("2024", 8, listOf(Team("red_bull", "Red Bull", 1, "276"), Team("ferrari", "Ferrari", 2, "252"), Team("mclaren", "McLaren", 3, "184"), Team("mercedes", "Mercedes", 4, "96")))
    val monaco = Race("2024", 8, "Monaco Grand Prix", "monaco", "Circuit de Monaco", "Monte Carlo", "Monaco", LocalDate.parse("2024-05-26"), listOf(
        Session("FP1", Instant.parse("2024-05-24T11:30:00Z")), Session("FP2", Instant.parse("2024-05-24T15:00:00Z")), Session("FP3", Instant.parse("2024-05-25T10:30:00Z")), Session("Qualifying", Instant.parse("2024-05-25T14:00:00Z")), Session("Race", Instant.parse("2024-05-26T13:00:00Z"))))
    val schedule = listOf(monaco)
    val result = RaceResult(monaco, listOf(Finisher("1", "LEC", "Charles Leclerc", "ferrari", "Finished"), Finisher("2", "PIA", "Oscar Piastri", "mclaren", "Finished"), Finisher("3", "SAI", "Carlos Sainz", "ferrari", "Finished"), Finisher("4", "NOR", "Lando Norris", "mclaren", "Finished"), Finisher("5", "RUS", "George Russell", "mercedes", "Finished")))
}

fun nextRace(races: List<Race>, now: Instant): Race? = races.filter { it.start?.isAfter(now) ?: !it.date.isBefore(now.atZone(java.time.ZoneOffset.UTC).toLocalDate()) }.minByOrNull { it.start ?: it.date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant() }
fun teamColor(id: String) = when (id) { "ferrari" -> "#F04C48"; "mercedes" -> "#48DCC4"; "mclaren" -> "#FFA24C"; "red_bull", "rb", "alphatauri" -> "#7898FF"; "aston_martin" -> "#62C9AB"; "williams" -> "#63B7FF"; "alpine" -> "#F295CE"; "haas" -> "#D5D6D8"; "sauber", "audi" -> "#A5DF52"; else -> "#B5B9BF" }
