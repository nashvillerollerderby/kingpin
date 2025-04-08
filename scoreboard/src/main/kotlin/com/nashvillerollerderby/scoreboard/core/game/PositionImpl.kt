package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.event.Command
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl

class PositionImpl(t: Team, fp: FloorPosition) :
    ScoreBoardEventProviderImpl<Position?>(t, t.id + "_" + fp.toString(), Team.POSITION),
    Position {
    override fun getProviderId(): String {
        return floorPosition.toString()
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        if (prop === Position.CLEAR) {
            set(Position.SKATER, null)
        }
        if (prop === Position.UNEND_BOX_TRIP) {
            currentFielding.execute(Fielding.UNEND_BOX_TRIP, source)
        }
        if (prop === Position.START_BOX_CLOCK) {
            val g = team.game
            val bt: BoxTrip = BoxTripImpl(g)
            g.add(Team.BOX_TRIP, bt)
            bt.add(BoxTrip.FIELDING, currentFielding)
        }
    }

    override fun getTeam(): Team {
        return parent as Team
    }

    override fun getFloorPosition(): FloorPosition {
        return _floorPosition
    }

    override fun updateCurrentFielding() {
        synchronized(coreLock) {
            currentFielding = team.runningOrUpcomingTeamJam.getFielding(floorPosition)
        }
    }

    override fun getSkater(): Skater? {
        return get(Position.SKATER)
    }

    override fun setSkater(s: Skater) {
        set(Position.SKATER, s)
    }

    override fun getCurrentFielding(): Fielding {
        return get(Position.CURRENT_FIELDING)
    }

    override fun setCurrentFielding(f: Fielding) {
        set(Position.CURRENT_FIELDING, f)
    }

    override fun isPenaltyBox(): Boolean {
        return get(Position.PENALTY_BOX)
    }

    override fun setPenaltyBox(box: Boolean) {
        set(Position.PENALTY_BOX, box)
    }

    private var _floorPosition: FloorPosition

    init {
        addProperties(Position.props)
        _floorPosition = fp
        setCopy(
            Position.NAME,
            this, Position.SKATER, Skater.NAME, true
        )
        setCopy(
            Position.ROSTER_NUMBER,
            this, Position.SKATER, Skater.ROSTER_NUMBER, true
        )
        setCopy(
            Position.FLAGS,
            this, Position.SKATER, Skater.FLAGS, true
        )
        setCopy(
            Position.SKATER,
            this, Position.CURRENT_FIELDING, Fielding.SKATER, false
        )
        setCopy(
            Position.PENALTY_BOX,
            this, Position.CURRENT_FIELDING, Fielding.PENALTY_BOX, false
        )
        setCopy(
            Position.PENALTY_TIME,
            this, Position.CURRENT_FIELDING, Fielding.PENALTY_TIME, true
        )
        setCopy(
            Position.CURRENT_BOX_SYMBOLS,
            this, Position.CURRENT_FIELDING, Fielding.BOX_TRIP_SYMBOLS, true
        )
        setCopy(
            Position.CURRENT_PENALTIES,
            this, Position.SKATER, Skater.CURRENT_PENALTIES, true
        )
        setCopy(
            Position.HAS_UNSERVED,
            this, Position.SKATER, Skater.HAS_UNSERVED, true
        )
        setCopy(
            Position.PENALTY_COUNT,
            this, Position.SKATER, Skater.PENALTY_COUNT, true
        )
        setCopy(
            Position.ANNOTATION,
            this, Position.CURRENT_FIELDING, Fielding.ANNOTATION, false
        )
        setCopy(
            Position.PENALTY_DETAILS,
            this, Position.SKATER, Skater.PENALTY_DETAILS, true
        )
        setCopy(
            Position.EXTRA_PENALTY_TIME,
            this, Position.SKATER, Skater.EXTRA_PENALTY_TIME, false
        )
        addWriteProtectionOverride(Position.CURRENT_FIELDING, ScoreBoardEventProvider.Source.NON_WS)
    }
}
