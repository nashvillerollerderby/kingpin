package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface ScoringTrip extends NumberedScoreBoardEventProvider<ScoringTrip> {
    int getScore();

    boolean isAfterSP();

    String getAnnotation();

    int tryApplyScoreAdjustment(ScoreAdjustment adjustment);

    Collection<Property<?>> props = new ArrayList<>();

    Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0, props);
    Value<Boolean> AFTER_S_P = new Value<>(Boolean.class, "AfterSP", false, props);
    Value<Boolean> CURRENT = new Value<>(Boolean.class, "Current", false, props);
    Value<Long> DURATION = new Value<>(Long.class, "Duration", 0L, props);
    Value<Long> JAM_CLOCK_START = new Value<>(Long.class, "JamClockStart", 0L, props);
    Value<Long> JAM_CLOCK_END = new Value<>(Long.class, "JamClockEnd", 0L, props);
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);

    Command INSERT_BEFORE = new Command("InsertBefore", props);
    Command REMOVE = new Command("Remove", props);
}
