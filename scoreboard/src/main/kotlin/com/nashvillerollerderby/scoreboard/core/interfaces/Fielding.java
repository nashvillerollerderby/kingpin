package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Fielding extends ParentOrderedScoreBoardEventProvider<Fielding> {
    TeamJam getTeamJam();

    Position getPosition();

    boolean isCurrent();

    Role getCurrentRole();

    Skater getSkater();

    void setSkater(Skater s);

    boolean isSitFor3();

    boolean isInBox();

    BoxTrip getCurrentBoxTrip();

    void updateBoxTripSymbols();

    Collection<Property<?>> props = new ArrayList<>();

    Value<Skater> SKATER = new Value<>(Skater.class, "Skater", null, props);
    Value<String> SKATER_NUMBER = new Value<>(String.class, "SkaterNumber", "?", props);
    Value<Boolean> NOT_FIELDED = new Value<>(Boolean.class, "NotFielded", false, props);
    Value<Position> POSITION = new Value<>(Position.class, "Position", null, props);
    Value<Boolean> SIT_FOR_3 = new Value<>(Boolean.class, "SitFor3", false, props);
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    Value<BoxTrip> CURRENT_BOX_TRIP = new Value<>(BoxTrip.class, "CurrentBoxTrip", null, props);
    Value<String> BOX_TRIP_SYMBOLS = new Value<>(String.class, "BoxTripSymbols", "", props);
    Value<String> BOX_TRIP_SYMBOLS_BEFORE_S_P =
            new Value<>(String.class, "BoxTripSymbolsBeforeSP", "", props);
    Value<String> BOX_TRIP_SYMBOLS_AFTER_S_P =
            new Value<>(String.class, "BoxTripSymbolsAfterSP", "", props);
    Value<Long> PENALTY_TIME = new Value<>(Long.class, "PenaltyTime", null, props);
    Value<String> ANNOTATION = new Value<>(String.class, "Annotation", "", props);

    Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip", props);

    Command ADD_BOX_TRIP = new Command("AddBoxTrip", props);
    Command UNEND_BOX_TRIP = new Command("UnendBoxTrip", props);
}
