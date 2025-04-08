package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Clock extends ScoreBoardEventProvider {
    ClockSnapshot snapshot();

    void restoreSnapshot(ClockSnapshot s);

    void start();

    void stop();

    void restart();

    String getName();

    int getNumber();

    void setNumber(int n);

    void changeNumber(int n);

    /**
     * @return The time displayed on the clock (in ms)
     */
    long getTime();

    void setTime(long ms);

    /**
     * Add time to the clock.
     *
     * @param ms The amount of change (can be negative)
     */
    void changeTime(long ms);

    /**
     * @return The clock's maximum time minus the time displayed on the clock (in
     * ms)
     */
    long getInvertedTime();

    /**
     * @return The time the clock has run (in ms). This is either the time or
     * inverted time depending on the direction of the clock
     */
    long getTimeElapsed();

    /**
     * Change the clock in the direction it is running. This function is the inverse
     * of changeTime(), when the clock counts down.
     *
     * @param ms The amount of change (can be negative)
     */
    void elapseTime(long ms);

    void resetTime();

    /**
     * @return The time until the clock reaches its maximum or zero (in ms). This is
     * the inverse of getTimeElapsed.
     */
    long getTimeRemaining();

    long getMaximumTime();

    void setMaximumTime(long ms);

    void changeMaximumTime(long ms);

    boolean isTimeAtStart(long time);

    boolean isTimeAtStart();

    boolean isTimeAtEnd(long time);

    boolean isTimeAtEnd();

    boolean isRunning();

    boolean isCountDirectionDown();

    void setCountDirectionDown(boolean down);

    long getCurrentIntermissionTime();

    interface ClockSnapshot {
        String getId();

        int getNumber();

        long getTime();

        boolean isRunning();
    }

    Collection<Property<?>> props = new ArrayList<>();

    Value<String> NAME = new Value<>(String.class, "Name", "", props);
    Value<Integer> NUMBER = new Value<>(Integer.class, "Number", 0, props);
    Value<Long> TIME = new Value<>(Long.class, "Time", 0L, props);
    Value<Long> INVERTED_TIME = new Value<>(Long.class, "InvertedTime", 0L, props);
    Value<Long> MAXIMUM_TIME = new Value<>(Long.class, "MaximumTime", 0L, props);
    Value<Boolean> DIRECTION = new Value<>(Boolean.class, "Direction", false, props);
    Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", false, props);

    Command START = new Command("Start", props);
    Command STOP = new Command("Stop", props);
    Command RESET_TIME = new Command("ResetTime", props);

    String SETTING_SYNC = "ScoreBoard.Clock.Sync";

    String ID_PERIOD = "Period";
    String ID_JAM = "Jam";
    String ID_LINEUP = "Lineup";
    String ID_TIMEOUT = "Timeout";
    String ID_INTERMISSION = "Intermission";
}
