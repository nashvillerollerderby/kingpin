package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Official extends ScoreBoardEventProvider {
    int compareTo(Official other);

    Child<Official> getType();

    Collection<Property<?>> props = new ArrayList<>();
    Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedOfficial

    Value<String> ROLE = new Value<>(String.class, "Role", "", props);
    Value<String> NAME = new Value<>(String.class, "Name", "", preparedProps);
    Value<String> LEAGUE = new Value<>(String.class, "League", "", preparedProps);
    Value<String> CERT = new Value<>(String.class, "Cert", "", preparedProps);
    Value<Team> P1_TEAM = new Value<>(Team.class, "P1Team", null, props);
    Value<Boolean> SWAP = new Value<>(Boolean.class, "Swap", false, props);
    Value<PreparedOfficial> PREPARED_OFFICIAL =
            new Value<>(PreparedOfficial.class, "PreparedOfficial", null, props);

    Command STORE = new Command("Store", props);

    String ROLE_HNSO = "Head Non-Skating Official";
    String ROLE_PLT = "Penalty Lineup Tracker";
    String ROLE_PT = "Penalty Tracker";
    String ROLE_PW = "Penalty Wrangler";
    String ROLE_WB = "Inside Whiteboard Operator";
    String ROLE_JT = "Jam Timer";
    String ROLE_SK = "Scorekeeper";
    String ROLE_SBO = "ScoreBoard Operator";
    String ROLE_PBM = "Penalty Box Manager";
    String ROLE_PBT = "Penalty Box Timer";
    String ROLE_LT = "Lineup Tracker";
    String ROLE_ALTN = "Non-Skating Official Alternate";

    String ROLE_HR = "Head Referee";
    String ROLE_IPR = "Inside Pack Referee";
    String ROLE_JR = "Jammer Referee";
    String ROLE_OPR = "Outside Pack Referee";
    String ROLE_ALTR = "Referee Alternate";
}
