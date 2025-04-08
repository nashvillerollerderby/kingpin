package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Timeout extends ScoreBoardEventProvider {
    int compareTo(Timeout other);

    void stop();

    TimeoutOwner getOwner();

    boolean isReview();

    boolean isRetained();

    boolean isRunning();

    Collection<Property<?>> props = new ArrayList<>();

    Value<TimeoutOwner> OWNER = new Value<>(TimeoutOwner.class, "Owner", null, props);
    Value<Boolean> REVIEW = new Value<>(Boolean.class, "Review", false, props);
    Value<Boolean> RETAINED_REVIEW = new Value<>(Boolean.class, "RetainedReview", false, props);
    Value<String> OR_REQUEST = new Value<>(String.class, "OrRequest", "", props);
    Value<String> OR_RESULT = new Value<>(String.class, "OrResult", "", props);
    Value<Boolean> RUNNING = new Value<>(Boolean.class, "Running", true, props);
    Value<Jam> PRECEDING_JAM = new Value<>(Jam.class, "PrecedingJam", null, props);
    Value<Integer> PRECEDING_JAM_NUMBER =
            new Value<>(Integer.class, "PrecedingJamNumber", 0, props);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    Value<Long> PERIOD_CLOCK_ELAPSED_START =
            new Value<>(Long.class, "PeriodClockElapsedStart", 0L, props);
    Value<Long> PERIOD_CLOCK_ELAPSED_END =
            new Value<>(Long.class, "PeriodClockElapsedEnd", 0L, props);
    Value<Long> PERIOD_CLOCK_END = new Value<>(Long.class, "PeriodClockEnd", 0L, props);
    Value<Long> WALLTIME_START = new Value<>(Long.class, "WalltimeStart", 0L, props);
    Value<Long> WALLTIME_END = new Value<>(Long.class, "WalltimeEnd", 0L, props);

    Command DELETE = new Command("Delete", props);
    Command INSERT_AFTER = new Command("InsertAfter", props);

    enum Owners implements TimeoutOwner {
        NONE(""),
        OTO("O");

        Owners(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }

        private final String id;
    }
}
