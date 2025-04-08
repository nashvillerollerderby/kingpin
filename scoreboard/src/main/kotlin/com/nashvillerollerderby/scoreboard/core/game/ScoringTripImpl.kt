package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.event.Command
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProviderImpl
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.Value

class ScoringTripImpl internal constructor(parent: TeamJam, number: Int) :
    NumberedScoreBoardEventProviderImpl<ScoringTrip?>(parent, number, TeamJam.SCORING_TRIP), ScoringTrip {
    public override fun computeValue(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ): Any? {
        if (prop === ScoringTrip.SCORE && (value as Int) < 0) {
            return 0
        }
        if (prop === ScoringTrip.DURATION) {
            return if (get(ScoringTrip.JAM_CLOCK_END) > 0L) {
                get(ScoringTrip.JAM_CLOCK_END) - get(ScoringTrip.JAM_CLOCK_START)
            } else {
                0L
            }
        }
        return value
    }

    public override fun valueChanged(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ) {
        if ((prop === ScoringTrip.SCORE || (prop === ScoringTrip.CURRENT && !(value as Boolean))) && get(ScoringTrip.JAM_CLOCK_END) == 0L && game.getClock(
                Clock.ID_JAM
            ) != null
        ) {
            set(ScoringTrip.JAM_CLOCK_END, game.getClock(Clock.ID_JAM).timeElapsed)
        }
        if (prop === ScoringTrip.SCORE && source == ScoreBoardEventProvider.Source.WS) {
            (parent as TeamJam).possiblyChangeOsOffset(last as Int - value as Int)
        }
        if (prop === ScoringTrip.CURRENT && value as Boolean && get(ScoringTrip.SCORE) == 0) {
            set(ScoringTrip.JAM_CLOCK_END, 0L)
        }
        if (prop === ScoringTrip.AFTER_S_P) {
            if (value as Boolean && hasNext()) {
                next!!.set(ScoringTrip.AFTER_S_P, true)
            }
            if (!value && hasPrevious()) {
                previous!!.set(ScoringTrip.AFTER_S_P, false)
            }
            if (flag != ScoreBoardEventProvider.Flag.SPECIAL_CASE) {
                if (value && (!hasPrevious() || !previous!!.get(ScoringTrip.AFTER_S_P))) {
                    parent.set(TeamJam.STAR_PASS_TRIP, this)
                } else if (!value && (!hasNext() || next!!.get(ScoringTrip.AFTER_S_P))) {
                    parent.set(TeamJam.STAR_PASS_TRIP, next)
                }
            }
        }
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        if (prop === ScoringTrip.REMOVE) {
            if (getParent().numberOf(TeamJam.SCORING_TRIP) > 1) {
                delete(source)
            } else {
                // We cannot remove the initial trip when it is the only trip, so set its score
                // to 0.
                set(ScoringTrip.SCORE, 0)
                set(ScoringTrip.JAM_CLOCK_END, 0L)
                set(ScoringTrip.ANNOTATION, "")
            }
        } else if (prop === ScoringTrip.INSERT_BEFORE) {
            parent.add(ownType, ScoringTripImpl(parent as TeamJam, number))
        }
    }

    override fun getScore(): Int {
        return get(ScoringTrip.SCORE)
    }

    override fun isAfterSP(): Boolean {
        return get(ScoringTrip.AFTER_S_P)
    }

    override fun getAnnotation(): String {
        return get(ScoringTrip.ANNOTATION)
    }

    override fun tryApplyScoreAdjustment(adjustment: ScoreAdjustment): Int {
        var remainingAmount = 0
        var appliedAmount = adjustment.amount
        if (-appliedAmount > score) {
            appliedAmount = -score
            remainingAmount = adjustment.amount - appliedAmount
        }
        set(ScoringTrip.SCORE, appliedAmount, ScoreBoardEventProvider.Flag.CHANGE)
        if ((parent as TeamJam)
                .possiblyChangeOsOffset(
                    -appliedAmount, adjustment.jamRecorded, adjustment.isRecordedInJam,
                    adjustment.isRecordedLastTwoMins
                )
        ) {
            remainingAmount = adjustment.amount
        }
        return remainingAmount
    }

    private val game: Game = parent.team.game

    init {
        addProperties(ScoringTrip.props)
        setCopy(ScoringTrip.JAM_CLOCK_START, this, PREVIOUS, ScoringTrip.JAM_CLOCK_END, true)
        setRecalculated(ScoringTrip.DURATION).addSource(this, ScoringTrip.JAM_CLOCK_END)
            .addSource(this, ScoringTrip.JAM_CLOCK_START)
        set(ScoringTrip.AFTER_S_P, if (hasPrevious()) previous!!.get(ScoringTrip.AFTER_S_P) else false)
    }
}
