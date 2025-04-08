package com.nashvillerollerderby.scoreboard.event;

import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;

import java.util.Collection;

public interface ScoreBoardEventProvider extends ValueWithId, Comparable<ScoreBoardEventProvider> {
    /**
     * This should be the frontend string for the Child enum value corresponding to
     * this type in its parent element
     */
    String getProviderName();

    /**
     * This should return the class or interface that this type will be accessed
     * through by event receivers
     */
    Class<? extends ScoreBoardEventProvider> getProviderClass();

    /**
     * Id to be used in order to identify this element amongst its siblings. (Could
     * e.g. be a Period/Jam/etc number or a UUID.)
     */
    String getProviderId();

    /**
     * The parent element.
     */
    ScoreBoardEventProvider getParent();

    boolean isAncestorOf(ScoreBoardEventProvider other);

    /**
     * remove all references to this element
     */
    void delete();

    /**
     * remove all references to this element
     */
    void delete(Source source);

    /**
     * This should return all the values, children, or commands that can be accessed
     * from the frontend
     */
    Collection<Property<?>> getProperties();

    Property<?> getProperty(String jsonName);

    void addScoreBoardListener(ScoreBoardListener listener);

    void removeScoreBoardListener(ScoreBoardListener listener);

    <T> T valueFromString(Value<T> prop, String sValue);

    <T> T get(Value<T> prop);

    // return value indicates if value was changed
    <T> boolean set(Value<T> prop, T value);

    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    <T> boolean set(Value<T> prop, T value, Flag flag);

    // return value indicates if value was changed
    <T> boolean set(Value<T> prop, T value, Source source);

    /*
     * return value indicates if value was changed Change flag for Integer and Long
     * values is implemented to add the given value to the previous one. Other flags
     * need to be implemented in overrides.
     */
    <T> boolean set(Value<T> prop, T value, Source source, Flag flag);

    /**
     * Run the given function inside a batch, to combine any resultant events.
     */
    void runInBatch(Runnable r);

    /**
     * If create is implemented for the respective type, this function will resort
     * to that, ignoring sValue. Otherwise it will create a ValWithId from id and
     * sValue.
     */
    <T extends ValueWithId> T childFromString(Child<T> prop, String id, String sValue);

    /*
     * Will return null if no such child is found
     */
    <T extends ValueWithId> T get(Child<T> prop, String id);

    <T extends OrderedScoreBoardEventProvider<T>> T get(NumberedChild<T> prop, Integer num);

    <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id);

    <T extends ScoreBoardEventProvider> T getOrCreate(Child<T> prop, String id, Source source);

    <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num);

    <T extends OrderedScoreBoardEventProvider<T>> T getOrCreate(NumberedChild<T> prop, Integer num,
                                                                Source source);

    <T extends ValueWithId> Collection<T> getAll(Child<T> prop);

    <T extends OrderedScoreBoardEventProvider<T>> T getFirst(NumberedChild<T> prop);

    <T extends OrderedScoreBoardEventProvider<T>> T getLast(NumberedChild<T> prop);

    int numberOf(Child<?> prop);

    // returns true, if a value was either changed or added
    <T extends ValueWithId> boolean add(Child<T> prop, T item);

    <T extends ValueWithId> boolean add(Child<T> prop, T item, Source source);

    // returns true, if a value was removed
    <T extends ValueWithId> boolean remove(Child<T> prop, String id);

    <T extends ValueWithId> boolean remove(Child<T> prop, String id, Source source);

    <T extends ValueWithId> boolean remove(Child<T> prop, T item);

    <T extends ValueWithId> boolean remove(Child<T> prop, T item, Source source);

    <T extends ValueWithId> void removeAll(Child<T> prop);

    <T extends ValueWithId> void removeAll(Child<T> prop, Source source);

    /**
     * Must call an appropriate constructor for all children that are themselves a
     * ScoreBoardEventProvider and can be created from the frontend or autosave
     */
    ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source);

    Integer getMinNumber(NumberedChild<?> prop);

    Integer getMaxNumber(NumberedChild<?> prop);

    void execute(Command prop);

    /**
     * Defaults to doing nothing. Should be overridden in classes that have frontend
     * commands.
     */
    void execute(Command prop, Source source);

    ScoreBoard getScoreBoard();

    <T extends ValueWithId> T getElement(Class<T> type, String id);

    void checkProperty(Property<?> prop);

    void cleanupAliases();

    Value<String> ID = new Value<>(String.class, "Id", "", null);
    Value<Boolean> READONLY = new Value<>(Boolean.class, "Readonly", false, null);

    enum Source {
        WS(false, false),
        AUTOSAVE(false, true),
        JSON(false, true),
        INVERSE_REFERENCE(true, false),
        COPY(true, false),
        RECALCULATE(true, false),
        UNLINK(true, false),
        RENUMBER(true, false),
        OTHER(true, false),

        // the following are intended for use as writeProtection Override only;
        ANY_INTERNAL(true, false),
        ANY_FILE(false, true),
        NON_WS(true, true);

        Source(boolean i, boolean f) {
            internal = i;
            file = f;
        }

        private final boolean internal;
        private final boolean file;

        public boolean isInternal() {
            return internal;
        }

        public boolean isFile() {
            return file;
        }
    }

    enum Flag {
        CHANGE,
        RESET,
        SPECIAL_CASE
    }
}
