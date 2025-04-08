package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface BoxTrip extends ScoreBoardEventProvider {
    int compareTo(BoxTrip other);

    void end();

    void unend();

    void startJam();

    void stopJam();

    Team getTeam();

    Game getGame();

    Clock getClock();

    boolean isCurrent();

    Fielding getCurrentFielding();

    Fielding getStartFielding();

    boolean startedBetweenJams();

    boolean startedAfterSP();

    Fielding getEndFielding();

    boolean endedBetweenJams();

    boolean endedAfterSP();

    Clock.ClockSnapshot snapshot();

    void restoreSnapshot(Clock.ClockSnapshot s);

    Collection<Property<?>> props = new ArrayList<>();

    Value<Boolean> IS_CURRENT = new Value<>(Boolean.class, "IsCurrent", false, props);
    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    Value<Fielding> START_FIELDING = new Value<>(Fielding.class, "StartFielding", null, props);
    Value<Integer> START_JAM_NUMBER = new Value<>(Integer.class, "StartJamNumber", 0, props);
    Value<Boolean> START_BETWEEN_JAMS =
            new Value<>(Boolean.class, "StartBetweenJams", false, props);
    Value<Boolean> START_AFTER_S_P = new Value<>(Boolean.class, "StartAfterSP", false, props);
    Value<Fielding> END_FIELDING = new Value<>(Fielding.class, "EndFielding", null, props);
    Value<Integer> END_JAM_NUMBER = new Value<>(Integer.class, "EndJamNumber", 0, props);
    Value<Boolean> END_BETWEEN_JAMS = new Value<>(Boolean.class, "EndBetweenJams", false, props);
    Value<Boolean> END_AFTER_S_P = new Value<>(Boolean.class, "EndAfterSP", false, props);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);
    Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L, props);
    Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L, props);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    Value<Skater> CURRENT_SKATER = new Value<>(Skater.class, "CurrentSkater", null, props);
    Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", props);
    Value<String> PENALTY_CODES = new Value<>(String.class, "PenaltyCodes", "", props);
    Value<Integer> TOTAL_PENALTIES = new Value<>(Integer.class, "TotalPenalties", null, props);
    Value<Boolean> TIMING_STOPPED = new Value<>(Boolean.class, "TimingStopped", false, props);
    Value<Long> TIME = new Value<>(Long.class, "Time", null, props);
    Value<Integer> SHORTENED = new Value<>(Integer.class, "Shortened", 0, props);
    Value<String> PENALTY_DETAILS = new Value<>(String.class, "PenaltyDetails", "", props);

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);
    Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty", props);
    Child<Clock> CLOCK = new Child<>(Clock.class, "Clock", props);

    Command START_EARLIER = new Command("StartEarlier", props);
    Command START_LATER = new Command("StartLater", props);
    Command END_EARLIER = new Command("EndEarlier", props);
    Command END_LATER = new Command("EndLater", props);
    Command DELETE = new Command("Delete", props);
}
