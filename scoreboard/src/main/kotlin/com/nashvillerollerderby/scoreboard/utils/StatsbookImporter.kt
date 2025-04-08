package com.nashvillerollerderby.scoreboard.utils

import com.nashvillerollerderby.scoreboard.core.game.GameImpl
import com.nashvillerollerderby.scoreboard.core.interfaces.Game
import com.nashvillerollerderby.scoreboard.core.interfaces.Official
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.core.interfaces.Team
import com.nashvillerollerderby.scoreboard.event.Child
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl
import org.apache.poi.ss.usermodel.*
import java.io.IOException
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

class StatsbookImporter(var scoreboard: ScoreBoard) {

    private val logger = Log4j2Logging.getLogger(this)

    fun read(`in`: InputStream) {
        try {
            wb = WorkbookFactory.create(`in`)
            game = GameImpl(scoreboard, UUID.randomUUID().toString())
            readIgrf()
            scoreboard.runInBatch {
                synchronized(coreLock) {
                    scoreboard.add(
                        ScoreBoard.GAME,
                        game
                    )
                }
            }
        } catch (e: IOException) {
            logger.error(e)
        }
    }

    private fun readIgrf() {
        val igrf = wb!!.getSheet("IGRF")
        readIgrfHead(igrf)
        readTeam(igrf, Team.ID_1)
        readTeam(igrf, Team.ID_2)
        readOfficials(igrf)
        readExpulsionSuspensionInfo(igrf)
    }

    private fun readIgrfHead(igrf: Sheet) {
        var row = igrf.getRow(2)
        readEventInfoCell(row, 1, Game.INFO_VENUE)
        readEventInfoCell(row, 8, Game.INFO_CITY)
        readEventInfoCell(row, 10, Game.INFO_STATE)
        readEventInfoCell(row, 11, Game.INFO_GAME_NUMBER)
        row = igrf.getRow(4)
        readEventInfoCell(row, 1, Game.INFO_TOURNAMENT)
        readEventInfoCell(row, 8, Game.INFO_HOST)
        row = igrf.getRow(6)
        try {
            var dateString: String = readCell(row, 1)
            // fail on malformed dates
            dateString =
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).format(DateTimeFormatter.ISO_LOCAL_DATE)
            game!!.add(Game.EVENT_INFO, ValWithId(Game.INFO_DATE, dateString))
        } catch (e: DateTimeParseException) {
        }
        try {
            var timeString = readCell(row, 8)
            // convert from format used in statsbook to HH:mm
            timeString = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("h:mm a")).toString()
            game!!.add(Game.EVENT_INFO, ValWithId(Game.INFO_START_TIME, timeString))
        } catch (e: DateTimeParseException) {

        }
    }

    private fun readTeam(igrf: Sheet, teamId: String) {
        val team = game!!.getTeam(teamId)
        val col = if (Team.ID_1 == teamId) 1 else 8
        team.set(Team.LEAGUE_NAME, readCell(igrf.getRow(9), col))
        team.set(Team.TEAM_NAME, readCell(igrf.getRow(10), col))
        team.set(Team.UNIFORM_COLOR, readCell(igrf.getRow(11), col))
        val captainName = readCell(igrf.getRow(48), col)

        for (i in 13..32) {
            readSkater(igrf.getRow(i), team, captainName)
        }
    }

    private fun readSkater(row: Row, team: Team, captainName: String) {
        val col = if (Team.ID_1 == team.providerId) 1 else 8
        var number = readCell(row, col)
        val name = readCell(row, col + 1)
        if ("" == number && "" == name) {
            return
        }
        val s = team.getOrCreate(Team.SKATER, UUID.randomUUID().toString())
        if (number.endsWith("*")) {
            s.flags = "ALT"
            number = number.substring(0, number.length - 1)
        }
        s.rosterNumber = number
        s.name = name
    }

    private fun readOfficials(igrf: Sheet) {
        var type = Game.NSO
        for (i in 59..87) {
            type = readOfficial(igrf.getRow(i), type)
        }
    }

    private fun readOfficial(row: Row, lastType: Child<Official>): Child<Official> {
        val role = readCell(row, 0)
        if ("" == role) {
            return Game.REF
        }
        val type = when (role) {
            Official.ROLE_HR, Official.ROLE_IPR, Official.ROLE_JR, Official.ROLE_OPR, Official.ROLE_ALTR -> Game.REF
            Official.ROLE_HNSO, Official.ROLE_JT, Official.ROLE_PLT, Official.ROLE_PT, Official.ROLE_WB, Official.ROLE_PW, Official.ROLE_SBO, Official.ROLE_SK, Official.ROLE_PBM, Official.ROLE_PBT, Official.ROLE_LT -> Game.NSO
            else -> lastType
        }
        val name = readCell(row, 2)
        if ("" == name) {
            return type
        }
        val league = readCell(row, 7)
        var cert = readCell(row, 10)
        if (cert.endsWith("1")) {
            cert = "1"
        } else if (cert.endsWith("2")) {
            cert = "2"
        } else if (cert.endsWith("3")) {
            cert = "3"
        } else if (cert.startsWith("R")) {
            cert = "R"
        }
        var newO: Official? = null
        if (type === Game.NSO) {
            for (o in game!!.getAll<Official>(type)) {
                if (o.get<String>(Official.NAME) == name && o.get<String>(Official.LEAGUE) == league) {
                    if (Official.ROLE_HNSO == role) {
                        newO = o
                        break
                    }
                    if (Official.ROLE_HNSO == o.get<String>(Official.ROLE)) {
                        newO = o
                        o.set(Official.ROLE, role)
                        break
                    }
                    if (Official.ROLE_PT == role && Official.ROLE_LT == o.get<String>(Official.ROLE) ||
                        Official.ROLE_LT == role && Official.ROLE_PT == o.get<String>(Official.ROLE)
                    ) {
                        newO = o
                        o.set(Official.ROLE, Official.ROLE_PLT)
                        break
                    }
                }
            }
        }
        if (newO == null) {
            newO = game!!.getOrCreate(type, UUID.randomUUID().toString())
            newO.set(Official.ROLE, role)
            newO.set(Official.NAME, name)
            newO.set(Official.LEAGUE, league)
            newO.set(Official.CERT, cert)
        }
        if (Official.ROLE_HNSO == role) {
            game!!.set(Game.HEAD_NSO, newO)
        }
        return type
    }

    private fun readExpulsionSuspensionInfo(igrf: Sheet) {
        game!!.set(Game.SUSPENSIONS_SERVED, readCell(igrf.getRow(39), 4))
    }

    private fun readEventInfoCell(row: Row, col: Int, key: String) {
        game!!.add(Game.EVENT_INFO, ValWithId(key, readCell(row, col)))
    }

    private fun readCell(row: Row, col: Int): String {
        val cell = row.getCell(col)
        return formatter.formatCellValue(cell)
    }

    var game: Game? = null
    var wb: Workbook? = null
    var formatter: DataFormatter = DataFormatter()

    var coreLock: Any = ScoreBoardEventProviderImpl.getCoreLock()
}
