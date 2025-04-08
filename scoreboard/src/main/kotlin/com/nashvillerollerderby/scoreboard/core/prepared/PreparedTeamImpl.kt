package com.nashvillerollerderby.scoreboard.core.prepared

import com.nashvillerollerderby.scoreboard.core.interfaces.*
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedTeam.PreparedSkater
import com.nashvillerollerderby.scoreboard.event.*

class PreparedTeamImpl(parent: ScoreBoard?, id: String?) :
    ScoreBoardEventProviderImpl<PreparedTeam?>(parent, id, ScoreBoard.PREPARED_TEAM), PreparedTeam {
    init {
        addProperties(PreparedTeam.props)
        addProperties(Team.preparedProps)
        setRecalculated(Team.FULL_NAME).addSource(this, Team.LEAGUE_NAME).addSource(this, Team.TEAM_NAME)
        setRecalculated(Team.DISPLAY_NAME)
            .addSource(this, Team.LEAGUE_NAME)
            .addSource(this, Team.TEAM_NAME)
            .addSource(this, Team.FULL_NAME)
            .addSource(scoreBoard.settings, Settings.SETTING)
        set(Team.FULL_NAME, "")
    }

    override fun computeValue(
        prop: Value<*>,
        value: Any?,
        last: Any?,
        source: ScoreBoardEventProvider.Source,
        flag: ScoreBoardEventProvider.Flag?
    ): Any {
        if (prop === Team.FULL_NAME) {
            val league = get(Team.LEAGUE_NAME)
            val team = get(Team.TEAM_NAME)
            val `in` = if (value == null) "" else value as String
            return if ("" != league) {
                if ("" != team) {
                    if (league == team) {
                        league
                    } else {
                        "$league - $team"
                    }
                } else {
                    league
                }
            } else {
                if ("" != team) {
                    team
                } else if ("" != `in`) {
                    `in`
                } else {
                    "Unnamed Team"
                }
            }
        }
        if (prop === Team.DISPLAY_NAME) {
            val setting = scoreBoard.settings[Team.SETTING_DISPLAY_NAME]
            return if (Team.OPTION_TEAM_NAME == setting && "" != get(Team.TEAM_NAME)) {
                get(Team.TEAM_NAME)
            } else if (Team.OPTION_FULL_NAME != setting && "" != get(Team.LEAGUE_NAME)) {
                get(Team.LEAGUE_NAME)
            } else {
                get(Team.FULL_NAME)
            }
        }
        return value!!
    }

    override fun create(
        prop: Child<out ScoreBoardEventProvider?>,
        id: String,
        source: ScoreBoardEventProvider.Source
    ): ScoreBoardEventProvider? {
        synchronized(coreLock) {
            if (prop === PreparedTeam.SKATER) {
                return PreparedTeamSkaterImpl(this, id)
            }
            return null
        }
    }

    override fun execute(prop: Command, source: ScoreBoardEventProvider.Source) {
        if (prop === Team.CLEAR_SKATERS) {
            for (s in getAll(PreparedTeam.SKATER)) {
                remove(PreparedTeam.SKATER, s)
            }
        }
    }

    class PreparedTeamSkaterImpl
        (parent: PreparedTeam?, id: String?) :
        ScoreBoardEventProviderImpl<PreparedSkater?>(parent, id, PreparedTeam.SKATER),
        PreparedSkater {
        init {
            addProperties(Skater.preparedProps)
        }
    }
}
