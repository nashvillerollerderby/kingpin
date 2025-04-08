package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.event.*
import com.nashvillerollerderby.scoreboard.rules.Rule
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging
import com.nashvillerollerderby.scoreboard.utils.ScoreBoardClock

class JamImpl(p: ScoreBoardEventProvider?, j: Int) : NumberedScoreBoardEventProviderImpl<Jam?>(p, j, Period.JAM),
    Jam {

    private val logger = Log4j2Logging.getLogger(this)

    constructor(parent: ScoreBoardEventProvider?, prev: Jam) : this(parent, prev.number + 1) {
        next = prev.next
        previous = prev
    }

    override fun setParent(p: ScoreBoardEventProvider) {
        if (parent === p) {
            return
        }
        parent.remove(ownType, this)
        parent.removeScoreBoardListener(periodNumberListener)
        providers.remove(periodNumberListener)
        parent = p
        periodNumberListener =
            setCopy(Jam.PERIOD_NUMBER, parent, if (parent is Game) Game.CURRENT_PERIOD_NUMBER else NUMBER, true)
        parent.add(ownType, this)
    }

    override fun delete(source: ScoreBoardEventProvider.Source) {
        if (source != ScoreBoardEventProvider.Source.UNLINK) {
            for (p in getAll(Jam.PENALTY)) {
                p.set(Penalty.JAM, if (hasNext()) next else previous)
            }
            for (t in getAll(Jam.TIMEOUTS_AFTER)) {
                t.set(Timeout.PRECEDING_JAM, previous)
            }
        }
        super.delete(source)
    }

    override fun computeValue(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ): Any? {
        if (prop === Jam.STAR_PASS) {
            return getTeamJam(Team.ID_1).isStarPass || getTeamJam(Team.ID_2).isStarPass
        }
        return value
    }

    override fun valueChanged(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ) {
        if (prop === Jam.INJURY_CONTINUATION && value as Boolean == true && source.isFile) {
            for (tj in getAll(Jam.TEAM_JAM)) {
                val first = tj.getFirst(TeamJam.SCORING_TRIP)
                if (previous!!.isImmediateScoring || first.hasNext() && first.next.number > 2) {
                    // team started on a scoring trip but constructor added initial - remove again
                    tj.remove(TeamJam.SCORING_TRIP, first)
                }
            }
        }
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        synchronized(coreLock) {
            if (prop === Jam.DELETE) {
                if (game.isInJam && (parent === game.currentPeriod) &&
                    (this === (parent as Period).currentJam)
                ) {
                    logger.warn("Refusing to delete running Jam.")
                    return
                }
                if (getTeamJam(Team.ID_1).jamScore + getTeamJam(Team.ID_1).osOffset != 0 ||
                    getTeamJam(Team.ID_2).jamScore + getTeamJam(Team.ID_2).osOffset != 0
                ) {
                    logger.warn("Refusing to delete Jam with points. Remove points first.")
                    return
                }

                delete(source)

                if (parent is Period && this === (parent as Period).currentJam) {
                    (parent as Period).set(
                        Period.CURRENT_JAM,
                        previous
                    )
                } else {
                }
            } else if (prop === Jam.INSERT_BEFORE) {
                if (parent is Period) {
                    parent.add(ownType, JamImpl(parent, number))
                } else if (!game.isInJam) {
                    val currentPeriod = game.currentPeriod
                    val newJam: Jam = JamImpl(currentPeriod, number)
                    currentPeriod.add(ownType, newJam)
                    set(NUMBER, 1, ScoreBoardEventProvider.Source.RENUMBER, ScoreBoardEventProvider.Flag.CHANGE)
                } else {
                }
            } else if (prop === Jam.INSERT_TIMEOUT_AFTER) {
                val newTo: Timeout = TimeoutImpl(this)
                newTo.set(Timeout.RUNNING, false)
                newTo.set(Timeout.WALLTIME_START, get(Jam.WALLTIME_END))
                newTo.set(Timeout.WALLTIME_END, get(Jam.WALLTIME_END))
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_START, next!!.get(Jam.PERIOD_CLOCK_ELAPSED_START))
                newTo.set(Timeout.PERIOD_CLOCK_ELAPSED_END, get(Jam.PERIOD_CLOCK_ELAPSED_START))
                newTo.set(
                    Timeout.PERIOD_CLOCK_END,
                    period.game.getClock(Clock.ID_PERIOD).get(Clock.MAXIMUM_TIME) -
                            get(Jam.PERIOD_CLOCK_ELAPSED_END)
                )
                period.add(Period.TIMEOUT, newTo)
            } else {
            }
        }
    }

    override fun getPeriod(): Period {
        return if (parent is Game) game.currentPeriod else (parent as Period)
    }

    override fun getGame(): Game {
        return if (parent is Game) parent as Game else (parent as Period).game
    }

    override fun isOvertimeJam(): Boolean {
        return get(Jam.OVERTIME)
    }

    override fun isInjuryContinuation(): Boolean {
        return get(Jam.INJURY_CONTINUATION)
    }

    override fun isImmediateScoring(): Boolean {
        return isOvertimeJam || period.isSuddenScoring
    }

    override fun getDuration(): Long {
        return get(Jam.DURATION)
    }

    fun setDuration(t: Long) {
        set(Jam.DURATION, t)
    }

    override fun getPeriodClockElapsedStart(): Long {
        return get(Jam.PERIOD_CLOCK_ELAPSED_START)
    }

    fun setPeriodClockElapsedStart(t: Long) {
        set(Jam.PERIOD_CLOCK_ELAPSED_START, t)
    }

    override fun getPeriodClockElapsedEnd(): Long {
        return get(Jam.PERIOD_CLOCK_ELAPSED_END)
    }

    fun setPeriodClockElapsedEnd(t: Long) {
        set(Jam.PERIOD_CLOCK_ELAPSED_END, t)
    }

    override fun getWalltimeStart(): Long {
        return get(Jam.WALLTIME_START)
    }

    fun setWalltimeStart(t: Long) {
        set(Jam.WALLTIME_START, t)
    }

    override fun getWalltimeEnd(): Long {
        return get(Jam.WALLTIME_END)
    }

    fun setWalltimeEnd(t: Long) {
        set(Jam.WALLTIME_END, t)
    }

    override fun getTeamJam(id: String): TeamJam {
        return get(Jam.TEAM_JAM, id)
    }

    override fun start() {
        synchronized(coreLock) {
            periodClockElapsedStart =
                game.getClock(Clock.ID_PERIOD).timeElapsed
            walltimeStart = ScoreBoardClock.getInstance().currentWalltime
            if (isInjuryContinuation) {
                if (previous!!.isOvertimeJam) {
                    set(Jam.OVERTIME, true)
                }
                game.getClock(Clock.ID_JAM)
                    .set(Clock.MAXIMUM_TIME, -previous!!.duration, ScoreBoardEventProvider.Flag.CHANGE)
                getTeamJam(Team.ID_1).setupInjuryContinuation()
                getTeamJam(Team.ID_2).setupInjuryContinuation()
            } else if (previous!!.isInjuryContinuation) {
                game.getClock(Clock.ID_JAM)
                    .set(
                        Clock.MAXIMUM_TIME,
                        game.getLong(Rule.JAM_DURATION),
                        ScoreBoardEventProvider.Source.RECALCULATE
                    )
            }
        }
    }

    override fun stop() {
        synchronized(coreLock) {
            set(Jam.DURATION, game.getClock(Clock.ID_JAM).timeElapsed)
            set(Jam.PERIOD_CLOCK_ELAPSED_END, game.getClock(Clock.ID_PERIOD).timeElapsed)
            set(Jam.PERIOD_CLOCK_DISPLAY_END, game.getClock(Clock.ID_PERIOD).time)
            set(Jam.WALLTIME_END, ScoreBoardClock.getInstance().currentWalltime)
            for (tj in getAll(Jam.TEAM_JAM)) {
                tj.currentScoringTrip.set(ScoringTrip.CURRENT, false)
            }
        }
    }

    private val game: Game

    private var periodNumberListener: ScoreBoardListener

    init {
        addProperties(Jam.props)
        setInverseReference(Jam.PENALTY, Penalty.JAM)
        setInverseReference(Jam.TIMEOUTS_AFTER, Timeout.PRECEDING_JAM)
        periodNumberListener =
            setCopy(Jam.PERIOD_NUMBER, parent, if (parent is Game) Game.CURRENT_PERIOD_NUMBER else NUMBER, true)
        game = if (parent is Game) parent as Game else (parent as Period).game
        add(Jam.TEAM_JAM, TeamJamImpl(this, Team.ID_1))
        add(Jam.TEAM_JAM, TeamJamImpl(this, Team.ID_2))
        addWriteProtection(Jam.TEAM_JAM)
        setRecalculated(Jam.STAR_PASS)
            .addSource(getTeamJam(Team.ID_1), TeamJam.STAR_PASS)
            .addSource(getTeamJam(Team.ID_2), TeamJam.STAR_PASS)
    }
}
