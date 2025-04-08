package com.nashvillerollerderby.scoreboard.viewer

import com.nashvillerollerderby.scoreboard.core.interfaces.Clock
import com.nashvillerollerderby.scoreboard.core.interfaces.Game
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard
import com.nashvillerollerderby.scoreboard.core.interfaces.Team
import io.prometheus.client.Collector
import io.prometheus.client.GaugeMetricFamily

class ScoreBoardMetricsCollector(private val sb: ScoreBoard) : Collector() {
    override fun collect(): List<MetricFamilySamples> {
        val mfs: MutableList<MetricFamilySamples> = mutableListOf()

        val clockTime = GaugeMetricFamily(
            "crg_scoreboard_clock_time_seconds",
            "Time on scoreboard clock.", mutableListOf("clock")
        )
        mfs.add(clockTime)
        val clockInvertedTime =
            GaugeMetricFamily(
                "crg_scoreboard_clock_inverted_time_seconds", "Time on scoreboard clock, inverted.",
                mutableListOf("clock")
            )
        mfs.add(clockInvertedTime)
        val clockRunning = GaugeMetricFamily(
            "crg_scoreboard_clock_running",
            "Is scoreboard clock running.", mutableListOf("clock")
        )
        mfs.add(clockRunning)
        val clockNumber =
            GaugeMetricFamily("crg_scoreboard_clock_number", "Number on scoreboard clock.", mutableListOf("clock"))
        mfs.add(clockNumber)
        for (c in sb.currentGame.getAllMirrors(Game.CLOCK)) {
            clockTime.addMetric(listOf(c.get(Clock.NAME)), (c.get(Clock.TIME).toFloat() / 1000).toDouble())
            clockInvertedTime.addMetric(
                listOf(c.get(Clock.NAME)),
                (c.get(Clock.INVERTED_TIME).toFloat() / 1000).toDouble()
            )
            clockRunning.addMetric(listOf(c.get(Clock.NAME)), (if (c.get(Clock.RUNNING)) 1 else 0).toDouble())
            clockNumber.addMetric(listOf(c.get(Clock.NAME)), c.get(Clock.NUMBER).toDouble())
        }

        val score =
            GaugeMetricFamily("crg_scoreboard_team_score", "Score on scoreboard.", mutableListOf("team", "name"))
        mfs.add(score)
        for (t in sb.currentGame.getAllMirrors(Game.TEAM)) {
            score.addMetric(listOf(t.get(Team.ID), t.get(Team.FULL_NAME)), t.get(Team.SCORE).toDouble())
        }

        return mfs
    }
}
