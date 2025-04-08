package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.NumberedChild;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.penalties.PenaltyCode;
import com.nashvillerollerderby.scoreboard.rules.Rule;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public interface Game extends ScoreBoardEventProvider {
    void postAutosaveUpdate();

    Timeout getCurrentTimeout();

    TimeoutOwner getTimeoutOwner();

    void setTimeoutOwner(TimeoutOwner owner);

    boolean isOfficialReview();

    void setOfficialReview(boolean official);

    boolean isInPeriod();

    void setInPeriod(boolean inPeriod);

    Period getOrCreatePeriod(int p);

    Period getCurrentPeriod();

    int getCurrentPeriodNumber();

    boolean isInJam();

    Jam getUpcomingJam();

    // update the references to current/upcoming/just ended TeamJams
    void updateTeamJams();

    boolean isInOvertime();

    void setInOvertime(boolean inOvertime);

    void startOvertime();

    boolean isInSuddenScoring();

    boolean isLastTwoMinutes();

    boolean isOfficialScore();

    void setOfficialScore(boolean official);

    void startJam();

    void stopJamTO();

    void timeout();

    void setTimeoutType(TimeoutOwner owner, boolean review);

    void clockUndo(boolean replace);

    Clock getClock(String id);

    Team getTeam(String id);

    void setRuleset(Ruleset rs);

    // if rs is the current ruleset or an ancestor of it, refresh the current rules
    void refreshRuleset(Ruleset rs);

    // Get information from current ruleset.
    String get(Rule r);

    boolean getBoolean(Rule r);

    int getInt(Rule r);

    long getLong(Rule r);

    void set(Rule r, String v);

    // The last loaded ruleset.
    Ruleset getRuleset();

    String getRulesetName();

    String getFilename();

    void exportDone(boolean success, String failureText);

    void clearStatsbookError();

    enum State {
        PREPARED("Prepared"),
        RUNNING("Running"),
        FINISHED("Finished");

        State(String str) {
            string = str;
        }

        @Override
        public String toString() {
            return string;
        }

        public static State fromString(String s) {
            for (State r : values()) {
                if (r.toString().equals(s)) {
                    return r;
                }
            }
            return null;
        }

        private final String string;
    }

    Collection<Property<?>> props = new ArrayList<>(Arrays.asList(Period.JAM, Team.BOX_TRIP));

    Value<String> NAME = new Value<>(String.class, "Name", "", props);
    Value<String> NAME_FORMAT = new Value<>(String.class, "NameFormat", "", props);
    Value<State> STATE = new Value<>(State.class, "State", State.PREPARED, props);
    Value<Integer> CURRENT_PERIOD_NUMBER =
            new Value<>(Integer.class, "CurrentPeriodNumber", 0, props);
    Value<Period> CURRENT_PERIOD = new Value<>(Period.class, "CurrentPeriod", null, props);
    Value<Jam> UPCOMING_JAM = new Value<>(Jam.class, "UpcomingJam", null, props);
    Value<Integer> UPCOMING_JAM_NUMBER = new Value<>(Integer.class, "UpcomingJamNumber", 0, props);
    Value<Boolean> IN_PERIOD = new Value<>(Boolean.class, "InPeriod", false, props);
    Value<Boolean> IN_JAM = new Value<>(Boolean.class, "InJam", false, props);
    Value<Boolean> IN_OVERTIME = new Value<>(Boolean.class, "InOvertime", false, props);
    Value<Boolean> IN_SUDDEN_SCORING = new Value<>(Boolean.class, "InSuddenScoring", false, props);
    Value<Boolean> INJURY_CONTINUATION_UPCOMING =
            new Value<>(Boolean.class, "InjuryContinuationUpcoming", false, props);
    Value<Boolean> OFFICIAL_SCORE = new Value<>(Boolean.class, "OfficialScore", false, props);
    Value<String> ABORT_REASON = new Value<>(String.class, "AbortReason", "", props);
    Value<Timeout> CURRENT_TIMEOUT = new Value<>(Timeout.class, "CurrentTimeout", null, props);
    Value<TimeoutOwner> TIMEOUT_OWNER =
            new Value<>(TimeoutOwner.class, "TimeoutOwner", null, props);
    Value<Boolean> OFFICIAL_REVIEW = new Value<>(Boolean.class, "OfficialReview", false, props);
    Value<Boolean> NO_MORE_JAM = new Value<>(Boolean.class, "NoMoreJam", false, props);
    Value<Ruleset> RULESET = new Value<>(Ruleset.class, "Ruleset", null, props);
    Value<String> RULESET_NAME = new Value<>(String.class, "RulesetName", "Custom", props);
    Value<Official> HEAD_NSO = new Value<>(Official.class, "HNSO", null, props);
    Value<Official> HEAD_REF = new Value<>(Official.class, "HR", null, props);
    Value<String> SUSPENSIONS_SERVED = new Value<>(String.class, "SuspensionsServed", "", props);
    Value<String> FILENAME =
            new Value<>(String.class, "Filename", "STATS-0000-00-00_Team1_vs_Team_2", props);
    Value<String> LAST_FILE_UPDATE = new Value<>(String.class, "LastFileUpdate", "Never", props);
    Value<Boolean> UPDATE_IN_PROGRESS =
            new Value<>(Boolean.class, "UpdateInProgress", false, props);
    Value<Boolean> STATSBOOK_EXISTS = new Value<>(Boolean.class, "StatsbookExists", false, props);
    Value<Boolean> JSON_EXISTS = new Value<>(Boolean.class, "JsonExists", false, props);
    Value<Boolean> CLOCK_DURING_FINAL_SCORE =
            new Value<>(Boolean.class, "ClockDuringFinalScore", false, props);
    Value<String> EXPORT_BLOCKED_BY = new Value<>(String.class, "ExportBlockedBy", "", props);

    Child<Clock> CLOCK = new Child<>(Clock.class, "Clock", props);
    Child<Team> TEAM = new Child<>(Team.class, "Team", props);
    Child<ValWithId> RULE = new Child<>(ValWithId.class, "Rule", props);
    Child<PenaltyCode> PENALTY_CODE = new Child<>(PenaltyCode.class, "PenaltyCode", props);
    Child<ValWithId> LABEL = new Child<>(ValWithId.class, "Label", props);
    Child<ValWithId> EVENT_INFO = new Child<>(ValWithId.class, "EventInfo", props);
    Child<Official> NSO = new Child<>(Official.class, "Nso", props);
    Child<Official> REF = new Child<>(Official.class, "Ref", props);
    Child<Expulsion> EXPULSION = new Child<>(Expulsion.class, "Expulsion", props);

    NumberedChild<Period> PERIOD = new NumberedChild<>(Period.class, "Period", props);

    Command START_JAM = new Command("StartJam", props);
    Command STOP_JAM = new Command("StopJam", props);
    Command TIMEOUT = new Command("Timeout", props);
    Command CLOCK_UNDO = new Command("ClockUndo", props);
    Command CLOCK_REPLACE = new Command("ClockReplace", props);
    Command START_OVERTIME = new Command("StartOvertime", props);
    Command OFFICIAL_TIMEOUT = new Command("OfficialTimeout", props);
    Command EXPORT = new Command("Export", props);
    Command START_BOX_TRIP = new Command("StartBoxTrip", props);
    Command START_JAMMER_BOX_TRIP = new Command("StartJammerBoxTrip", props);
    Command COPY = new Command("Copy", props);

    String SETTING_DEFAULT_NAME_FORMAT = "ScoreBoard.Game.DefaultNameFormat";

    String INFO_VENUE = "Venue";
    String INFO_CITY = "City";
    String INFO_STATE = "State";
    String INFO_TOURNAMENT = "Tournament";
    String INFO_HOST = "HostLeague";
    String INFO_GAME_NUMBER = "GameNo";
    String INFO_DATE = "Date";
    String INFO_START_TIME = "StartTime";

    String ACTION_NONE = "---";
    String ACTION_NO_REPLACE = "No Action";
    String ACTION_START_JAM = "Start Jam";
    String ACTION_STOP_JAM = "Stop Jam";
    String ACTION_STOP_TO = "End Timeout";
    String ACTION_LINEUP = "Lineup";
    String ACTION_TIMEOUT = "Timeout";
    String ACTION_RE_TIMEOUT = "New Timeout";
    String ACTION_OVERTIME = "Overtime Lineup";
    String UNDO_PREFIX = "Un-";
}
