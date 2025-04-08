package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedChild;
import com.nashvillerollerderby.scoreboard.event.ParentOrderedScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface TeamJam extends ParentOrderedScoreBoardEventProvider<TeamJam> {
    Jam getJam();

    Team getTeam();

    TeamJam getOtherTeam();

    void setupInjuryContinuation();

    boolean isRunningOrEnded();

    boolean isRunningOrUpcoming();

    int getLastScore();

    void setLastScore(int l);

    int getOsOffset();

    void setOsOffset(int o);

    void changeOsOffset(int c);

    void possiblyChangeOsOffset(int amount);

    boolean possiblyChangeOsOffset(int amount, Jam jamRecorded, boolean recordedInJam,
                                   boolean recordedInLastTwoMins);

    int getJamScore();

    int getTotalScore();

    ScoringTrip getCurrentScoringTrip();

    void addScoringTrip();

    void removeScoringTrip();

    boolean isLost();

    boolean isLead();

    boolean isCalloff();

    boolean isInjury();

    boolean isDisplayLead();

    boolean isStarPass();

    ScoringTrip getStarPassTrip();

    boolean hasNoPivot();

    void setNoPivot(boolean np);

    Fielding getFielding(FloorPosition fp);

    Collection<Property<?>> props = new ArrayList<>();

    Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null, props);
    Value<Integer> CURRENT_TRIP_NUMBER = new Value<>(Integer.class, "CurrentTripNumber", 0, props);
    Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0, props);
    Value<Integer> OS_OFFSET = new Value<>(Integer.class, "OsOffset", 0, props);
    Value<String> OS_OFFSET_REASON = new Value<>(String.class, "OsOffsetReason", "", props);
    Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0, props);
    Value<Integer> AFTER_S_P_SCORE = new Value<>(Integer.class, "AfterSPScore", 0, props);
    Value<Integer> TOTAL_SCORE = new Value<>(Integer.class, "TotalScore", 0, props);
    Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false, props);
    Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false, props);
    Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false, props);
    Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true, props);
    Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false, props);
    Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false, props);
    Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false, props);
    Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null, props);
    Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false, props);

    Child<Fielding> FIELDING = new Child<>(Fielding.class, "Fielding", props);

    NumberedChild<ScoringTrip> SCORING_TRIP =
            new NumberedChild<>(ScoringTrip.class, "ScoringTrip", props);

    Command COPY_LINEUP_TO_CURRENT = new Command("CopyLineupToCurrent", props);
}
