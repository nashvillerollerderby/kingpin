package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedTeam.PreparedSkater;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.NumberedChild;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Skater extends ScoreBoardEventProvider {
    int compareTo(Skater other);

    Team getTeam();

    String getName();

    void setName(String id);

    String getRosterNumber();

    void setRosterNumber(String number);

    Fielding getFielding(TeamJam teamJam);

    Fielding getCurrentFielding();

    void removeCurrentFielding();

    void updateFielding(TeamJam teamJam);

    Position getPosition();

    void setPosition(Position position);

    Role getRole();

    Role getRole(TeamJam tj);

    void setRole(Role role);

    void setRoleToBase();

    Role getBaseRole();

    void setBaseRole(Role base);

    void updateEligibility();

    boolean isPenaltyBox();

    boolean isPenaltyBox(boolean checkUpcoming);

    void setPenaltyBox(boolean box);

    String getFlags();

    void setFlags(String flags);

    BoxTrip getCurrentBoxTrip();

    Penalty getPenalty(String num);

    List<Penalty> getUnservedPenalties();

    boolean hasUnservedPenalties();

    long getExtraPenaltyTime();

    void mergeInto(PreparedSkater preparedSkater);

    Collection<Property<?>> props = new ArrayList<>();
    Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedTeam.Skater

    Value<PreparedSkater> PREPARED_SKATER =
            new Value<>(PreparedSkater.class, "PreparedSkater", null, props);
    Value<String> NAME = new Value<>(String.class, "Name", "", preparedProps);
    Value<String> ROSTER_NUMBER = new Value<>(String.class, "RosterNumber", "", preparedProps);
    Value<Fielding> CURRENT_FIELDING = new Value<>(Fielding.class, "CurrentFielding", null, props);
    Value<String> CURRENT_BOX_SYMBOLS = new Value<>(String.class, "CurrentBoxSymbols", "", props);
    Value<String> CURRENT_PENALTIES = new Value<>(String.class, "CurrentPenalties", "", props);
    Value<Integer> PENALTY_COUNT = new Value<>(Integer.class, "PenaltyCount", 0, props);
    Value<Position> POSITION = new Value<>(Position.class, "Position", null, props);
    Value<Role> ROLE = new Value<>(Role.class, "Role", null, props);
    Value<Role> BASE_ROLE = new Value<>(Role.class, "BaseRole", null, props);
    Value<Boolean> PENALTY_BOX = new Value<>(Boolean.class, "PenaltyBox", false, props);
    Value<Boolean> HAS_UNSERVED = new Value<>(Boolean.class, "HasUnserved", false, props);
    Value<String> FLAGS = new Value<>(String.class, "Flags", "", preparedProps);
    Value<String> PRONOUNS = new Value<>(String.class, "Pronouns", "", preparedProps);
    Value<String> COLOR = new Value<>(String.class, "Color", "", props);
    Value<String> PENALTY_DETAILS = new Value<>(String.class, "PenaltyDetails", "", props);
    Value<Long> EXTRA_PENALTY_TIME = new Value<>(Long.class, "ExtraPenaltyTime", 0L, props);

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);

    NumberedChild<Penalty> PENALTY = new NumberedChild<>(Penalty.class, "Penalty", props);

    String FO_EXP_ID = "0";
}
