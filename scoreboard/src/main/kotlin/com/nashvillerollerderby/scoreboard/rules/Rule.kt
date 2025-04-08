package com.nashvillerollerderby.scoreboard.rules

enum class Rule(var ruleDefinition: RuleDefinition) {
    NUMBER_PERIODS(IntegerRule("Period.Number", "Number of periods", 2)),
    PERIOD_DURATION(TimeRule("Period.Duration", "Duration of a period", "30:00")),
    PERIOD_DIRECTION(
        BooleanRule(
            "Period.ClockDirection", "Which way should the period clock count?", true,
            "Count Down", "Count Up"
        )
    ),
    PERIOD_END_BETWEEN_JAMS(
        BooleanRule(
            "Period.EndBetweenJams", "When can a period end?", true,
            "Anytime outside a jam", "Only on jam end"
        )
    ),

    JAM_NUMBER_PER_PERIOD(
        BooleanRule(
            "Jam.ResetNumberEachPeriod", "How to handle Jam Numbers", true,
            "Reset each period", "Continue counting"
        )
    ),
    JAM_DURATION(TimeRule("Jam.Duration", "Maximum duration of a jam", "2:00")),
    JAM_DIRECTION(
        BooleanRule(
            "Jam.ClockDirection", "Which way should the jam clock count?", true, "Count Down",
            "Count Up"
        )
    ),
    SUDDEN_SCORING(BooleanRule("Jam.SuddenScoring", "Use JRDA Sudden Scoring rule?", false, "Enabled", "Disabled")),
    SUDDEN_SCORING_MIN_POINTS_DIFFERENCE(
        IntegerRule(
            "Jam.SuddenScoringMinPointsDifference",
            "Minimal score difference at halftime at which sudden scoring is activated.", 150
        )
    ),
    SUDDEN_SCORING_MAX_TRAILING_POINTS(
        IntegerRule(
            "Jam.SuddenScoringMaxTrailingPoints",
            "Maximum points the trailing team may have at halftime to trigger sudden scoring.", 25
        )
    ),
    SUDDEN_SCORING_JAM_DURATION(
        TimeRule(
            "Jam.SuddenScoringDuration",
            "Maximum duration of a jam when sudden scoring is in effect", "1:00"
        )
    ),
    INJURY_CONTINUATION(
        BooleanRule(
            "Jam.InjuryContinuation",
            "A jam called for injury can be followed by a continuation jam.", false,
            "Enabled", "Disabled"
        )
    ),

    LINEUP_DURATION(TimeRule("Lineup.Duration", "Duration of lineup", "0:30")),
    OVERTIME_LINEUP_DURATION(
        TimeRule(
            "Lineup.OvertimeDuration", "Duration of lineup before an overtime jam",
            "1:00"
        )
    ),
    LINEUP_DIRECTION(
        BooleanRule(
            "Lineup.ClockDirection", "Which way should the lineup clock count?", false,
            "Count Down", "Count Up"
        )
    ),

    TTO_DURATION(TimeRule("Timeout.TeamTODuration", "Duration of a team timeout", "1:00")),
    TIMEOUT_DIRECTION(
        BooleanRule(
            "Timeout.ClockDirection", "Which way should the timeout clock count?", false,
            "Count Down", "Count Up"
        )
    ),
    STOP_PC_ON_TO(
        BooleanRule(
            "Timeout.StopPeriodClockAlways",
            "Stop the period clock on every timeout? If false, the options below control the " +
                    "behaviour per type of timeout.",
            true, "True", "False"
        )
    ),
    STOP_PC_ON_OTO(
        BooleanRule(
            "Timeout.StopPeriodClockOnOTO", "Stop the period clock on official timeouts?", false,
            "True", "False"
        )
    ),
    STOP_PC_ON_TTO(
        BooleanRule(
            "Timeout.StopPeriodClockOnTTO", "Stop the period clock on team timeouts?", false,
            "True", "False"
        )
    ),
    STOP_PC_ON_OR(
        BooleanRule(
            "Timeout.StopPeriodClockOnOR", "Stop the period clock on official reviews?", false,
            "True", "False"
        )
    ),
    STOP_PC_AFTER_TO_DURATION(
        TimeRule(
            "Timeout.StopPeriodClockAfterTODuration",
            "Stop the period clock, if a timeout lasts longer than this time. Set to a high value to disable.", "60:00"
        )
    ),
    EXTRA_JAM_AFTER_OTO(
        BooleanRule(
            "Timeout.ExtraJamAfterOTO",
            "Can an OTO cause an extra Jam to be played when there wouldn't be one otherwise?",
            false,
            "True",
            "False"
        )
    ),
    TO_JAM(
        BooleanRule(
            "Timeout.JamDuring", "Allow a jam to happen with stopped period clock?", false, "True",
            "False"
        )
    ),
    NO_TO_CLOCK_STOP(
        BooleanRule(
            "Timeout.NoClockStop",
            "Should the timeout clock continue to run after the end of a timeout?", true,
            "True", "False"
        )
    ),

    INTERMISSION_DURATIONS(
        StringRule(
            "Intermission.Durations",
            "List of the duration of intermissions as they appear in the game, separated by commas.", "15:00,60:00"
        )
    ),
    INTERMISSION_DIRECTION(
        BooleanRule(
            "Intermission.ClockDirection",
            "Which way should the intermission clock count?", true, "Count Down",
            "Count Up"
        )
    ),

    NUMBER_TIMEOUTS(IntegerRule("Team.Timeouts", "How many timeouts each team is granted per game or period", 3)),
    TIMEOUTS_PER_PERIOD(
        BooleanRule(
            "Team.TimeoutsPer", "Are timeouts granted per period or per game?", false,
            "Period", "Game"
        )
    ),
    NUMBER_REVIEWS(
        IntegerRule(
            "Team.OfficialReviews",
            "How many official reviews each team is granted per game or period", 1
        )
    ),
    REVIEWS_PER_PERIOD(
        BooleanRule(
            "Team.OfficialReviewsPer",
            "Are official reviews granted per period or per game?", true, "Period", "Game"
        )
    ),
    NUMBER_RETAINS(
        IntegerRule(
            "Team.MaxRetains",
            "How many times per game or period a team can retain an official review", 1
        )
    ),
    RDCL_PER_HALF_RULES(
        BooleanRule(
            "Team.RDCLPerHalfRules",
            "Restrict one TTO to the first two periods and one to the rest of the game. " +
                    "Stretch per period ORs to first two resp. all other periods.",
            false, "Enabled", "Disabled"
        )
    ),
    WFTDA_LATE_SCORE_RULE(
        BooleanRule(
            "Score.WftdaLateChangeRule",
            "Score changes after the end of the following jam don't affect the game score. With less " +
                    "than 2 minutes left in the game this applies to changes after the next jam starts.",
            true, "Enabled", "Disabled"
        )
    ),

    PENALTIES_FILE(
        StringRule(
            "Penalties.DefinitionFile",
            "File that contains the penalty code definitions to be used",
            "/config/penalties/wftda2018.json"
        )
    ),
    FO_LIMIT(
        IntegerRule(
            "Penalties.NumberToFoulout",
            "After how many penalties a skater has fouled out of the game. Note that the software " +
                    "currently does not support more than 9 penalties per skater.",
            7
        )
    ),
    PENALTY_DURATION(TimeRule("Penalties.Duration", "How long does a penalty last.", "0:30"));

    override fun toString(): String {
        return ruleDefinition.name
    }
}
