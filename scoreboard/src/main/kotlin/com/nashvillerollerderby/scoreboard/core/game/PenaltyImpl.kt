package com.nashvillerollerderby.scoreboard.core.game

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.event.Command
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProviderImpl
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.Value
import com.nashvillerollerderby.scoreboard.rules.Rule
import com.nashvillerollerderby.scoreboard.utils.ScoreBoardClock

class PenaltyImpl(s: Skater, n: Int) : NumberedScoreBoardEventProviderImpl<Penalty?>(s, n, Skater.PENALTY), Penalty {
    override fun compareTo(other: Penalty?): Int {
        if (other == null) {
            return -1
        }
        if (jam === other.jam) {
            return (get(Penalty.TIME) - other.get(Penalty.TIME)).toInt()
        }
        if (jam == null) {
            return 1
        }
        return jam?.compareTo(other.jam) ?: -1
    }

    override fun computeValue(
        prop: Value<*>,
        value: Any?,
        last: Any,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag
    ): Any? {
        if (prop === NEXT && number == 0) {
            return null
        }
        if (prop === PREVIOUS && value != null && (value as Penalty).number == 0) {
            return null
        }
        if (prop === Penalty.SERVED) {
            return (get(Penalty.BOX_TRIP) != null || get(Penalty.FORCE_SERVED))
        }
        return value
    }

    override fun valueChanged(
        prop: Value<*>,
        value: Any?,
        last: Any,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag
    ) {
        if (prop === Penalty.JAM && Skater.FO_EXP_ID != providerId && flag != ScoreBoardEventProvider.Flag.SPECIAL_CASE && !source.isFile) {
            var newPos = number
            if (value == null || (value as Jam).compareTo(last as Jam) > 0) {
                var comp = next!!
                while (compareTo(comp) > 0) { // will be false if comp == null
                    newPos = comp.number
                    comp = comp.next
                }
            } else {
                var comp = previous
                while (comp != null && compareTo(comp) < 0) {
                    newPos = comp.number
                    comp = comp.previous
                }
            }
            moveToNumber(newPos)

            if (newPos == game.getInt(Rule.FO_LIMIT)) {
                val fo = parent.get(Skater.PENALTY, Skater.FO_EXP_ID)
                if (fo != null && "FO" == fo.get(Penalty.CODE)) {
                    fo.set(Penalty.JAM, value as Jam?)
                }
            }
        }
        if (prop === Penalty.CODE || prop === Penalty.SERVED || prop === Penalty.BOX_TRIP) {
            possiblyUpdateSkater()
        }
        if (prop === Penalty.CODE) {
            if (value == null) {
                game.remove(Game.EXPULSION, id)
                delete(source)
            } else if ("FO" == value) {
                game.remove(Game.EXPULSION, id)
            } else if (Skater.FO_EXP_ID == providerId && game.get(Game.EXPULSION, id) == null) {
                game.add(Game.EXPULSION, ExpulsionImpl(game, this))
            }
        }
        if (prop === Penalty.SERVED) {
            parent.set(Skater.HAS_UNSERVED, value as Boolean?)
        }
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        if (prop === Penalty.REMOVE) {
            delete(source)
        }
    }

    private fun possiblyUpdateSkater() {
        if ("" == code || Skater.FO_EXP_ID == id) {
            return
        }
        if (get(Penalty.BOX_TRIP) != null) {
            get(Penalty.BOX_TRIP).set(
                BoxTrip.PENALTY_CODES,
                get(BoxTrip.PENALTY_CODES),
                ScoreBoardEventProvider.Source.RECALCULATE
            )
        }
        skater.set(
            Skater.CURRENT_PENALTIES,
            skater.get(Skater.CURRENT_PENALTIES),
            ScoreBoardEventProvider.Source.RECALCULATE
        )
    }

    override fun getPeriodNumber(): Int {
        return get(Penalty.PERIOD_NUMBER)
    }

    override fun getJamNumber(): Int {
        return get(Penalty.JAM_NUMBER)
    }

    override fun getJam(): Jam? {
        return get(Penalty.JAM)
    }

    override fun getCode(): String {
        return get(Penalty.CODE)
    }

    override fun isServed(): Boolean {
        return get(Penalty.SERVED)
    }

    override fun getDetails(): String {
        return skater.id + "_" + providerId
    }

    private val game: Game = s.team.game
    private val skater = parent as Skater

    init {
        addProperties(Penalty.props)
        set(Penalty.TIME, ScoreBoardClock.getInstance().currentWalltime)
        setInverseReference(Penalty.JAM, Jam.PENALTY)
        setInverseReference(Penalty.BOX_TRIP, BoxTrip.PENALTY)
        addWriteProtectionOverride(Penalty.TIME, ScoreBoardEventProvider.Source.ANY_FILE)
        setRecalculated(Penalty.SERVED).addSource(this, Penalty.BOX_TRIP).addSource(this, Penalty.FORCE_SERVED)
        setCopy(Penalty.SERVING, this, Penalty.BOX_TRIP, BoxTrip.IS_CURRENT, true)
        setCopy(Penalty.JAM_NUMBER, this, Penalty.JAM, NUMBER, true)
        setCopy(Penalty.PERIOD_NUMBER, this, Penalty.JAM, Jam.PERIOD_NUMBER, true)
        set(
            Penalty.FORCE_SERVED, !scoreBoard.settings[ScoreBoard.SETTING_USE_LT].toBoolean() ||
                    Skater.FO_EXP_ID == providerId
        )
        if (s.isPenaltyBox(true)) {
            set(Penalty.BOX_TRIP, s.currentBoxTrip)
        }
        set(Penalty.SERVED, get(Penalty.BOX_TRIP) != null)
        set(Penalty.CODE, "?")
        set(Penalty.JAM, game.currentPeriod.currentJam, ScoreBoardEventProvider.Flag.SPECIAL_CASE)
    }
}
