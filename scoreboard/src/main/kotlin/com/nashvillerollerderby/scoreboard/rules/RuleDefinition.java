package com.nashvillerollerderby.scoreboard.rules;

import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public abstract class RuleDefinition extends ScoreBoardEventProviderImpl<RuleDefinition> {
    public RuleDefinition(Type type, String name, String description, Object defaultValue) {
        super(null, name, Rulesets.RULE_DEFINITION);
        addProperties(props);
        setCopy(NAME, this, ID, true);
        set(TYPE, type);
        set(DEFAULT_VALUE, defaultValue.toString());
        set(DESCRIPTION, description);
        for (Property<?> prop : getProperties()) {
            addWriteProtection(prop);
        }
    }

    public Type getType() {
        return get(TYPE);
    }

    public String getName() {
        return get(NAME);
    }

    public String getDefaultValue() {
        return get(DEFAULT_VALUE);
    }

    public String getDescription() {
        return get(DESCRIPTION);
    }

    public int getIndex() {
        return get(INDEX);
    }

    public void setIndex(Integer i) {
        values.put(INDEX, i);
    }

    public abstract boolean isValueValid(String v);

    public void setParent(Rulesets p) {
        parent = p;
        scoreBoard = parent.getScoreBoard();
    }

    public enum Type {
        BOOLEAN("Boolean"),
        INTEGER("Integer"),
        LONG("Long"),
        STRING("String"),
        TIME("Time");

        Type(String s) {
            string = s;
        }

        @Override
        public String toString() {
            return string;
        }

        String string;
    }

    public static Collection<Property<?>> props = new ArrayList<>();
    public static Collection<Property<?>> boolProps = new ArrayList<>(); // props only used on BooleanRule

    public static final Value<String> NAME = new Value<>(String.class, "Name", "", props);
    public static final Value<Type> TYPE = new Value<>(Type.class, "Type", null, props);
    public static final Value<String> DEFAULT_VALUE = new Value<>(String.class, "DefaultValue", "", props);
    public static final Value<String> DESCRIPTION = new Value<>(String.class, "Description", "", props);
    public static final Value<Integer> INDEX = new Value<>(Integer.class, "Index", 0, props);
    public static final Value<String> TRUE_VALUE = new Value<>(String.class, "TrueValue", "", boolProps);
    public static final Value<String> FALSE_VALUE = new Value<>(String.class, "FalseValue", "", boolProps);
}
