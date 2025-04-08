package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Collection;

public interface Settings extends ScoreBoardEventProvider {
    String get(String k);

    // Setting to null deletes a setting.
    void set(String k, String v);

    Collection<Property<?>> props = new ArrayList<>();

    Child<ValWithId> SETTING = new Child<>(ValWithId.class, "Setting", props);
}
