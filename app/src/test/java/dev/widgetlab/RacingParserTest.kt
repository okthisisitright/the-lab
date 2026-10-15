package dev.widgetlab

import dev.widgetlab.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class RacingParserTest {
    private fun fixture(name: String)=javaClass.classLoader!!.getResource("$name.json")!!.readText()
    @Test fun verifiedProviderResponsesHaveCoherentSeasonsAndRelationships() {
        val drivers=RacingParser.drivers(fixture("drivers"));val teams=RacingParser.teams(fixture("teams"))
        assertEquals("2026",drivers.season);assertEquals(drivers.season,teams.season)
        assertTrue(drivers.entries.size>=20);assertTrue(teams.entries.size>=10)
        assertTrue(drivers.entries.all{it.id.isNotBlank() && it.position>0 && it.points.toDouble()>=0})
        assertTrue(drivers.entries.all{d->d.teams.all{t->teams.entries.any{it.id==t}}})
        assertTrue(RacingParser.schedule(fixture("schedule")).isNotEmpty())
        assertTrue(RacingParser.result(fixture("results"))!!.finishers.size>=3)
    }
    @Test fun sprintSessionsAreStructuredAndSortedByActualInstants() {
        val schedule=RacingParser.schedule(fixture("schedule"))
        val sprint=schedule.first{r->r.sessions.any{it.name=="Sprint"}}
        assertTrue(sprint.sessions.any{it.name=="Sprint Q"})
        val known=sprint.sessions.mapNotNull{it.start}
        assertEquals(known.sorted(),known)
        assertEquals(1,sprint.sessions.count{it.name=="Race"})
    }
    @Test fun unavailableTimesRemainUnknownInsteadOfMidnight() {
        val raw=JSONObject(fixture("schedule"));val race=raw.getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races").getJSONObject(0)
        race.remove("time");assertNull(RacingParser.race(race).start)
    }
    @Test fun raceTimesUseZoneRulesAcrossDaylightSavingTransitions() {
        val zone=ZoneId.of("Europe/London")
        assertEquals(13,Instant.parse("2026-03-28T13:00:00Z").atZone(zone).hour)
        assertEquals(14,Instant.parse("2026-03-29T13:00:00Z").atZone(zone).hour)
        val monaco=SampleRacing.monaco.start!!
        assertEquals(18,monaco.atZone(ZoneId.of("Asia/Kolkata")).hour)
        assertEquals(30,monaco.atZone(ZoneId.of("Asia/Kolkata")).minute)
    }
    @Test fun nextRaceExcludesCompletedRacesAndNeverCreatesNewEvents() {
        val schedule=RacingParser.schedule(fixture("schedule"))
        val now=Instant.parse("2026-10-15T00:00:00Z")
        val next=nextRace(schedule,now)
        assertNotNull(next);assertTrue(next!!.start?.isAfter(now) ?: true)
        assertNull(nextRace(schedule,Instant.parse("2030-01-01T00:00:00Z")))
        assertNull(nextRace(emptyList(),now))
    }
    @Test fun emptyStandingsAndResultsAreValidNotInvented() {
        val empty="""{"MRData":{"StandingsTable":{"season":"2027","StandingsLists":[]}}}"""
        assertTrue(RacingParser.drivers(empty).entries.isEmpty());assertEquals("2027",RacingParser.teams(empty).season)
        assertNull(RacingParser.result("""{"MRData":{"RaceTable":{"Races":[]}}}"""))
    }
}
