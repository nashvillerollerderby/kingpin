package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Position extends ScoreBoardEventProvider {
    void updateCurrentFielding();

    Team getTeam();

    FloorPosition getFloorPosition();

    Skater getSkater();

    void setSkater(Skater s);

    Fielding getCurrentFielding();

    void setCurrentFielding(Fielding f);

    boolean isPenaltyBox();

    void setPenaltyBox(boolean box);

    Collection<Property<?>> props = new ArrayList<>();

    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "", props);
    Value<String> CURRENT_PENALTIES = new Value<>(String.class, "CurrentPenalties", "", props);
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);
    Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null, props);
    Value<String> NAME = new Value<>(String.class, "Name", "", props);
    Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", props);
    Value<String> FLAGS = new Value<>(String.class, "Flags", "", props);
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    Value<Boolean> HAS_UNSERVED = new Value<>(Boolean.class, "HasUnserved", false, props);
    Value<Long> PENALTY_TIME = new Value<>(Long.class, "PenaltyTime", null, props);
    Value<Integer> PENALTY_COUNT = new Value<>(Integer.class, "PenaltyCount", 0, props);
    Value<String> PENALTY_DETAILS = new Value<>(String.class, "PenaltyDetails", "", props);
    Value<Long> EXTRA_PENALTY_TIME = new Value<>(Long.class, "ExtraPenaltyTime", 0L, props);

    Command CLEAR = new Command("Clear", props);
    Command UNEND_BOX_TRIP = new Command("UnendBoxTrip", props);
    Command START_BOX_CLOCK = new Command("StartBoxClock", props);
}
