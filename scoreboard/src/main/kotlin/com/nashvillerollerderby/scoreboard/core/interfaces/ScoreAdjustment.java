package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface ScoreAdjustment extends ScoreBoardEventProvider {
    int getAmount();

    Jam getJamRecorded();

    boolean isRecordedInJam();

    boolean isRecordedLastTwoMins();

    ScoringTrip getTripAppliedTo();

    Collection<Property<?>> props = new ArrayList<>();

    Value<Integer> AMOUNT = new Value<>(Integer.class, "Amount", 0, props);
    Value<Jam> JAM_RECORDED = new Value<>(Jam.class, "JamRecorded", null, props);
    Value<Integer> PERIOD_NUMBER_RECORDED =
            new Value<>(Integer.class, "PeriodNumberRecorded", 0, props);
    Value<Integer> JAM_NUMBER_RECORDED = new Value<>(Integer.class, "JamNumberRecorded", 0, props);
    Value<Boolean> RECORDED_DURING_JAM =
            new Value<>(Boolean.class, "RecordedDuringJam", false, props);
    Value<Boolean> LAST_TWO_MINUTES = new Value<>(Boolean.class, "LastTwoMinutes", false, props);
    Value<Boolean> OPEN = new Value<>(Boolean.class, "Open", true, props);
    Value<ScoringTrip> APPLIED_TO = new Value<>(ScoringTrip.class, "AppliedTo", null, props);

    Command DISCARD = new Command("Discard", props);
}
