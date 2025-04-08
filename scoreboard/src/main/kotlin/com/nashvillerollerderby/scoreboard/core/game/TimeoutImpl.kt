package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.core.interfaces.Timeout.Owners
import com.nashvillerollerderby.scoreboard.event.Command
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl
import com.nashvillerollerderby.scoreboard.event.Value
import com.nashvillerollerderby.scoreboard.utils.ScoreBoardClock
import java.util.*

class TimeoutImpl : ScoreBoardEventProviderImpl<Timeout?>, Timeout {
    constructor(p: Period, id: String) : super(p, id, Period.TIMEOUT) {
        game = p.game
        initReferences()
        if ("noTimeout" == id) {
            set(Timeout.RUNNING, false)
            set(READONLY, true)
        }
    }

    constructor(precedingJam: Jam) : super(precedingJam.parent, UUID.randomUUID().toString(), Period.TIMEOUT) {
        game = precedingJam.period.game
        initReferences()
        set(Timeout.PRECEDING_JAM, precedingJam)
        set(Timeout.WALLTIME_START, ScoreBoardClock.getInstance().currentWalltime)
        set(Timeout.PERIOD_CLOCK_ELAPSED_START, game.getClock(Clock.ID_PERIOD).timeElapsed)
    }

    private fun initReferences() {
        addProperties(Timeout.props)
        set(Timeout.OWNER, Owners.NONE)
        setInverseReference(Timeout.PRECEDING_JAM, Jam.TIMEOUTS_AFTER)
        setCopy(
            Timeout.PRECEDING_JAM_NUMBER,
            this, Timeout.PRECEDING_JAM, Jam.NUMBER, true
        )
    }

    override fun compareTo(other: Timeout): Int {
        var result = 0
        if (get<Jam?>(Timeout.PRECEDING_JAM) != null && other.get<Jam?>(Timeout.PRECEDING_JAM) != null) {
            result = get(Timeout.PRECEDING_JAM).compareTo(other.get(Timeout.PRECEDING_JAM))
        }
        if (result == 0) {
            result = (get(Timeout.WALLTIME_START) - other.get(Timeout.WALLTIME_START)).toInt()
        }
        return result
    }

    override fun valueChanged(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ) {
        if (prop === Timeout.OWNER) {
            if (last is Team) {
                last.remove(Team.TIME_OUT, this)
            }
            if (value is Team) {
                value.add(Team.TIME_OUT, this)
            }
            if (get(Timeout.PRECEDING_JAM) === game.currentPeriod.currentJam) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), ScoreBoardEventProvider.Source.RECALCULATE)
            }
        }
        if (prop === Timeout.REVIEW && owner is Team) {
            (owner as Team).recountTimeouts()
            if (get(Timeout.PRECEDING_JAM) === game.currentPeriod.currentJam) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), ScoreBoardEventProvider.Source.RECALCULATE)
            }
        }
        if (prop === Timeout.RETAINED_REVIEW && owner is Team) {
            (owner as Team).recountTimeouts()
        }
        if (prop === Timeout.PRECEDING_JAM) {
            if (value != null && (value as Jam).parent !== getParent()) {
                getParent().remove(Period.TIMEOUT, this)
                parent = (value as Jam).parent
                getParent().add(Period.TIMEOUT, this)
            }
            if (owner is Team) {
                (owner as Team).recountTimeouts()
            }
            if (game.currentPeriod != null &&
                (value === game.currentPeriod.currentJam || last === game.currentPeriod.currentJam)
            ) {
                game.set(Game.NO_MORE_JAM, game.get(Game.NO_MORE_JAM), ScoreBoardEventProvider.Source.RECALCULATE)
            }
        }
    }

    override fun delete(source: ScoreBoardEventProvider.Source) {
        if (get(Timeout.OWNER) is Team) {
            (get(Timeout.OWNER) as Team).remove(Team.TIME_OUT, this)
        }
        super.delete(source)
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        synchronized(coreLock) {
            if (prop === Timeout.DELETE) {
                if (!isRunning) {
                    delete(source)
                }
            } else if (prop === Timeout.INSERT_AFTER) {
                val newTo: Timeout = TimeoutImpl(get(Timeout.PRECEDING_JAM))
                newTo.set(Timeout.RUNNING, false)
                newTo.set(Timeout.WALLTIME_START, get(Timeout.WALLTIME_END))
                newTo.set(Timeout.WALLTIME_END, get(Timeout.WALLTIME_END))
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_START, get(Timeout.PERIOD_CLOCK_ELAPSED_END))
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_END, get(Timeout.PERIOD_CLOCK_ELAPSED_END))
                newTo.set(Timeout.PERIOD_CLOCK_END, get(Timeout.PERIOD_CLOCK_END))
                getParent().add(Period.TIMEOUT, newTo)
            }
        }
    }

    override fun stop() {
        set(Timeout.RUNNING, false)
        if (owner === Owners.NONE) {
            set(Timeout.OWNER, Owners.OTO)
        }
        set(Timeout.WALLTIME_END, ScoreBoardClock.getInstance().currentWalltime)
        if (!game.getClock(Clock.ID_PERIOD).isRunning) {
            // don't record duration when ended late
            set(Timeout.DURATION, game.getClock(Clock.ID_TIMEOUT).timeElapsed)
            set(Timeout.PERIOD_CLOCK_ELAPSED_END, game.getClock(Clock.ID_PERIOD).timeElapsed)
            set(Timeout.PERIOD_CLOCK_END, game.getClock(Clock.ID_PERIOD).time)
        }
    }

    override fun getOwner(): TimeoutOwner {
        return get(Timeout.OWNER)
    }

    override fun isReview(): Boolean {
        return get(Timeout.REVIEW)
    }

    override fun isRetained(): Boolean {
        return get(Timeout.RETAINED_REVIEW)
    }

    override fun isRunning(): Boolean {
        return get(Timeout.RUNNING)
    }

    private var game: Game
}
