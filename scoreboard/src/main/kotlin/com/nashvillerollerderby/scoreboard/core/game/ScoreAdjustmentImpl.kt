package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.event.Command
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl
import com.nashvillerollerderby.scoreboard.event.Value
import java.util.*

class ScoreAdjustmentImpl(t: Team, id: String?) :
    ScoreBoardEventProviderImpl<ScoreAdjustment?>(t, id, Team.SCORE_ADJUSTMENT),
    ScoreAdjustment {
    private fun initReferences() {
        addProperties(ScoreAdjustment.props)
        set(ScoreAdjustment.JAM_RECORDED, game.currentPeriod.currentJam)
        set(ScoreAdjustment.RECORDED_DURING_JAM, game.isInJam)
        set(ScoreAdjustment.LAST_TWO_MINUTES, game.isLastTwoMinutes)
        addWriteProtectionOverride(ScoreAdjustment.JAM_RECORDED, ScoreBoardEventProvider.Source.ANY_FILE)
        addWriteProtectionOverride(ScoreAdjustment.RECORDED_DURING_JAM, ScoreBoardEventProvider.Source.ANY_FILE)
        addWriteProtectionOverride(ScoreAdjustment.LAST_TWO_MINUTES, ScoreBoardEventProvider.Source.ANY_FILE)
        setCopy(ScoreAdjustment.JAM_NUMBER_RECORDED, this, ScoreAdjustment.JAM_RECORDED, Jam.NUMBER, true)
        setCopy(ScoreAdjustment.PERIOD_NUMBER_RECORDED, this, ScoreAdjustment.JAM_RECORDED, Jam.PERIOD_NUMBER, true)
    }

    public override fun valueChanged(
        prop: Value<*>,
        value: Any?,
        last: Any,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag
    ) {
        if (prop === ScoreAdjustment.OPEN && value as Boolean? == false) {
            addWriteProtection(ScoreAdjustment.OPEN)
            val team = (parent as Team)
            if (team.get(Team.ACTIVE_SCORE_ADJUSTMENT) === this) {
                team.set(Team.ACTIVE_SCORE_ADJUSTMENT, null)
            }
        }
        if (prop === ScoreAdjustment.APPLIED_TO && value != null) {
            (parent as Team).applyScoreAdjustment(this)
        }
        if (prop === ScoreAdjustment.AMOUNT && source == ScoreBoardEventProvider.Source.WS) {
            if ((value as Int?) == 0) {
                delete(source)
            } else {
                closeTimerTask.cancel()
                closeTimer.purge()
                closeTimerTask = object : TimerTask() {
                    override fun run() {
                        scoreBoard.runInBatch { set(ScoreAdjustment.OPEN, false) }
                    }
                }
                closeTimer.schedule(closeTimerTask, 4000)
            }
        }
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        synchronized(coreLock) {
            if (prop === ScoreAdjustment.DISCARD) {
                jamRecorded.getTeamJam(parent.providerId).possiblyChangeOsOffset(amount)
                delete(source)
            }
        }
    }

    override fun getAmount(): Int {
        return get(ScoreAdjustment.AMOUNT)
    }

    override fun getJamRecorded(): Jam {
        return get(ScoreAdjustment.JAM_RECORDED)
    }

    override fun isRecordedInJam(): Boolean {
        return get(ScoreAdjustment.RECORDED_DURING_JAM)
    }

    override fun isRecordedLastTwoMins(): Boolean {
        return get(ScoreAdjustment.LAST_TWO_MINUTES)
    }

    override fun getTripAppliedTo(): ScoringTrip {
        return get(ScoreAdjustment.APPLIED_TO)
    }

    private val game: Game = t.game

    private val closeTimer = Timer()
    private var closeTimerTask: TimerTask = object : TimerTask() {
        override fun run() {} // dummy, so the variable is not
        // null at the first score entry
    }

    init {
        initReferences()
    }
}
