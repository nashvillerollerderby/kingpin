package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface PreparedOfficial extends ScoreBoardEventProvider {
    boolean matches(String name, String league);

    Collection<Property<?>> props = new ArrayList<>();

    Value<String> FULL_INFO = new Value<>(String.class, "FullInfo", "", props);
}
