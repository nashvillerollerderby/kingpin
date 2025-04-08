package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Expulsion extends ScoreBoardEventProvider {

    Collection<Property<?>> props = new ArrayList<>();

    Value<String> INFO = new Value<>(String.class, "Info", "", props);
    Value<String> EXTRA_INFO = new Value<>(String.class, "ExtraInfo", "", props);
    Value<Boolean> SUSPENSION = new Value<>(Boolean.class, "Suspension", false, props);
}
