package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.rules.Rule;
import com.nashvillerollerderby.scoreboard.rules.RuleDefinition;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Collection;

public interface Rulesets extends ScoreBoardEventProvider {
    RuleDefinition getRuleDefinition(String id);

    Ruleset getRuleset(String id);

    void removeRuleset(String id);

    Ruleset addRuleset(String name, Ruleset parent);

    Ruleset addRuleset(String name, Ruleset parent, String id);

    Collection<Property<?>> props = new ArrayList<>();

    Child<RuleDefinition> RULE_DEFINITION =
            new Child<>(RuleDefinition.class, "RuleDefinition", props);
    Child<Ruleset> RULESET = new Child<>(Ruleset.class, "Ruleset", props);

    String ROOT_ID = "WFTDARuleset";

    interface Ruleset extends ScoreBoardEventProvider {
        String get(Rule k);

        String getName();

        void setName(String n);

        Ruleset getParentRuleset();

        void setParentRuleset(Ruleset rs);

        boolean isAncestorOf(Ruleset rs);

        void setRule(String id, String value);

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Value<Ruleset> PARENT = new Value<>(Ruleset.class, "Parent", null, props);
        Value<String> NAME = new Value<>(String.class, "Name", "", props);

        Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule", props);
    }
}
