package com.nashvillerollerderby.scoreboard.core.prepared

import com.nashvillerollerderby.scoreboard.core.interfaces.Official
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedOfficial
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl
import com.nashvillerollerderby.scoreboard.event.Value

class PreparedOfficialImpl(parent: ScoreBoard?, id: String?) :
    ScoreBoardEventProviderImpl<PreparedOfficial?>(parent, id, ScoreBoard.PREPARED_OFFICIAL), PreparedOfficial {
    init {
        addProperties(Official.preparedProps)
        addProperties(PreparedOfficial.props)
        setRecalculated(PreparedOfficial.FULL_INFO).addSource(this, Official.NAME).addSource(this, Official.LEAGUE)
    }

    override fun computeValue(
        prop: Value<*>,
        value: Any,
        last: Any,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag
    ): Any {
        if (prop === PreparedOfficial.FULL_INFO) {
            return get(Official.NAME) + " (" + get(Official.LEAGUE) + ")"
        }
        return value
    }

    override fun matches(name: String, league: String): Boolean {
        return get(Official.NAME) == name && ("" == league || get(Official.LEAGUE) == league)
    }
}
