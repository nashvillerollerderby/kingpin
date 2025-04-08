package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Penalty extends NumberedScoreBoardEventProvider<Penalty> {
    int compareTo(Penalty other);

    int getPeriodNumber();

    int getJamNumber();

    Jam getJam();

    String getCode();

    boolean isServed();

    String getDetails();

    Collection<Property<?>> props = new ArrayList<>();

    Value<Long> TIME = new Value<>(Long.class, "Time", 0L, props);
    Value<Jam> JAM = new Value<>(Jam.class, "Jam", null, props);
    Value<Integer> PERIOD_NUMBER = new Value<>(Integer.class, "PeriodNumber", 0, props);
    Value<Integer> JAM_NUMBER = new Value<>(Integer.class, "JamNumber", 0, props);
    Value<String> CODE = new Value<>(String.class, "Code", "", props);
    Value<Boolean> SERVING = new Value<>(Boolean.class, "Serving", false, props);
    Value<Boolean> SERVED = new Value<>(Boolean.class, "Served", false, props);
    Value<Boolean> FORCE_SERVED = new Value<>(Boolean.class, "ForceServed", false, props);
    Value<BoxTrip> BOX_TRIP = new Value<>(BoxTrip.class, "BoxTrip", null, props);

    Command REMOVE = new Command("Remove", props);
}
