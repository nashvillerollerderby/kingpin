package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedChild;
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Period extends NumberedScoreBoardEventProvider<Period> {
    Game getGame();

    PeriodSnapshot snapshot();

    void restoreSnapshot(PeriodSnapshot s);

    boolean isSuddenScoring();

    boolean isRunning();

    Jam getJam(int j);

    Jam getInitialJam();

    Jam getCurrentJam();

    int getCurrentJamNumber();

    void startJam();

    void stopJam();

    long getDuration();

    long getWalltimeStart();

    long getWalltimeEnd();

    Collection<Property<?>> props = new ArrayList<>();

    Value<Jam> CURRENT_JAM = new Value<>(Jam.class, "CurrentJam", null, props);
    Value<Integer> CURRENT_JAM_NUMBER = new Value<>(Integer.class, "CurrentJamNumber", 0, props);
    Value<Boolean> SUDDEN_SCORING = new Value<>(Boolean.class, "SuddenScoring", false, props);
    Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", false, props);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);
    Value<String> LOCAL_TIME_START = new Value<>(String.class, "LocalTimeStart", "", props);
    Value<Integer> TEAM_1_PENALTY_COUNT = new Value<>(Integer.class, "Team1PenaltyCount", 0, props);
    Value<Integer> TEAM_2_PENALTY_COUNT = new Value<>(Integer.class, "Team2PenaltyCount", 0, props);
    Value<Integer> TEAM_1_POINTS = new Value<>(Integer.class, "Team1Points", 0, props);
    Value<Integer> TEAM_2_POINTS = new Value<>(Integer.class, "Team2Points", 0, props);

    Child<Timeout> TIMEOUT = new Child<>(Timeout.class, "Timeout", props);

    NumberedChild<Jam> JAM = new NumberedChild<>(Jam.class, "Jam", props);

    Command DELETE = new Command("Delete", props);
    Command INSERT_BEFORE = new Command("InsertBefore", props);
    Command INSERT_TIMEOUT = new Command("InsertTimeout", props);
    Command ADD_INITIAL_JAM = new Command("AddInitialJam", props);

    interface PeriodSnapshot {
        String getId();

        Jam getCurrentJam();
    }
}
