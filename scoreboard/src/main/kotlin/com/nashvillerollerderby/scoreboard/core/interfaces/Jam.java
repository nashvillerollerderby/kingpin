package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Jam extends NumberedScoreBoardEventProvider<Jam> {
    Period getPeriod();

    Game getGame();

    void setParent(ScoreBoardEventProvider p);

    boolean isOvertimeJam();

    boolean isInjuryContinuation();

    boolean isImmediateScoring();

    long getDuration();

    long getPeriodClockElapsedStart();

    long getPeriodClockElapsedEnd();

    long getWalltimeStart();

    long getWalltimeEnd();

    TeamJam getTeamJam(String id);

    void start();

    void stop();

    Collection<Property<?>> props = new ArrayList<>();

    Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0, props);
    Value<Boolean> STAR_PASS =
            new Value<>(Boolean.class, "StarPass", false, props); // true, if either team had an SP
    Value<Boolean> OVERTIME = new Value<>(Boolean.class, "Overtime", false, props);
    Value<Boolean> INJURY_CONTINUATION =
            new Value<>(Boolean.class, "InjuryContinuation", false, props);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    Value<Long> PERIOD_CLOCK_ELAPSED_START =
            new Value<>(Long.class, "PeriodClockElapsedStart", 0L, props);
    Value<Long> PERIOD_CLOCK_ELAPSED_END =
            new Value<>(Long.class, "PeriodClockElapsedEnd", 0L, props);
    Value<Long> PERIOD_CLOCK_DISPLAY_END =
            new Value<>(Long.class, "PeriodClockDisplayEnd", 0L, props);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);

    Child<TeamJam> TEAM_JAM = new Child<>(TeamJam.class, "TeamJam", props);
    Child<Penalty> PENALTY = new Child<>(Penalty.class, "Penalty", props);
    Child<Timeout> TIMEOUTS_AFTER = new Child<>(Timeout.class, "TimeoutsAfter", props);

    Command DELETE = new Command("Delete", props);
    Command INSERT_BEFORE = new Command("InsertBefore", props);
    Command INSERT_TIMEOUT_AFTER = new Command("InsertTimeoutAfter", props);
}
