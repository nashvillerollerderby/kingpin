package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Collection;

// Roster for teams for loading in for games.
public interface PreparedTeam extends ScoreBoardEventProvider {

    Collection<Property<?>> props = new ArrayList<>();

    Child<ValWithId> UNIFORM_COLOR = new Child<>(ValWithId.class, "UniformColor", props);
    Child<PreparedSkater> SKATER = new Child<>(PreparedSkater.class, "Skater", props);

    interface PreparedSkater extends ScoreBoardEventProvider {
    }
}
