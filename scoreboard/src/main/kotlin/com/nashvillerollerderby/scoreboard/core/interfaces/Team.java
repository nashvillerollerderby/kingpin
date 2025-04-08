package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Collection;

// Managemnt of currently playing teams.
public interface Team extends ScoreBoardEventProvider, TimeoutOwner {
    Game getGame();

    String getName();

    void setName(String name);

    void startJam();

    void stopJam();

    TeamSnapshot snapshot();

    void restoreSnapshot(TeamSnapshot s);

    String getAlternateName(String id);

    String getAlternateName(AlternateNameId id);

    void setAlternateName(String id, String name);

    void removeAlternateName(String id);

    String getColor(String id);

    void setColor(String id, String color);

    void removeColor(String id);

    String getLogo();

    void setLogo(String logo);

    void loadPreparedTeam(PreparedTeam pt);

    void timeout();

    void officialReview();

    TeamJam getRunningOrUpcomingTeamJam();

    TeamJam getRunningOrEndedTeamJam();

    void updateTeamJams();

    int getScore();

    void applyScoreAdjustment(ScoreAdjustment adjustment);

    ScoringTrip getCurrentTrip();

    int getTimeouts();

    int getOfficialReviews();

    boolean inTimeout();

    boolean inOfficialReview();

    boolean retainedOfficialReview();

    void setRetainedOfficialReview(boolean retained_official_review);

    void recountTimeouts();

    Skater getSkater(String id);

    void addSkater(Skater skater);

    void removeSkater(String id);

    Position getPosition(FloorPosition fp);

    void field(Skater s, Role r);

    void field(Skater s, Role r, TeamJam tj);

    boolean hasFieldingAdvancePending();

    boolean isLost();

    boolean isLead();

    boolean isOnInitial();

    boolean isCalloff();

    boolean isInjury();

    boolean isDisplayLead();

    boolean isStarPass();

    boolean hasNoPivot();

    Team getOtherTeam();

    String ID_1 = "1";
    String ID_2 = "2";
    String SETTING_DISPLAY_NAME = "ScoreBoard.Teams.DisplayName";
    String SETTING_FILE_NAME = "ScoreBoard.Teams.FileName";
    String OPTION_TEAM_NAME = "Team";
    String OPTION_LEAGUE_NAME = "League";
    String OPTION_FULL_NAME = "Full";

    Collection<Property<?>> props = new ArrayList<>();
    Collection<Property<?>> preparedProps = new ArrayList<>(); // also present on PreparedTeam

    Value<String> DISPLAY_NAME = new Value<>(String.class, "Name", "", preparedProps);
    Value<String> FULL_NAME = new Value<>(String.class, "FullName", "", preparedProps);
    Value<String> LEAGUE_NAME = new Value<>(String.class, "LeagueName", "", preparedProps);
    Value<String> TEAM_NAME = new Value<>(String.class, "TeamName", "", preparedProps);
    Value<String> FILE_NAME = new Value<>(String.class, "FileName", "", props);
    Value<String> INITIALS = new Value<>(String.class, "Initials", "", props);
    Value<String> UNIFORM_COLOR = new Value<>(String.class, "UniformColor", null, props);
    Value<String> LOGO = new Value<>(String.class, "Logo", "", preparedProps);
    Value<TeamJam> RUNNING_OR_UPCOMING_TEAM_JAM =
            new Value<>(TeamJam.class, "RunningOrUpcomingTeamJam", null, props);
    Value<TeamJam> RUNNING_OR_ENDED_TEAM_JAM =
            new Value<>(TeamJam.class, "RunningOrEndedTeamJam", null, props);
    Value<Boolean> FIELDING_ADVANCE_PENDING =
            new Value<>(Boolean.class, "FieldingAdvancePending", false, props);
    Value<ScoringTrip> CURRENT_TRIP = new Value<>(ScoringTrip.class, "CurrentTrip", null, props);
    Value<Integer> SCORE = new Value<>(Integer.class, "Score", 0, props);
    Value<Integer> JAM_SCORE = new Value<>(Integer.class, "JamScore", 0, props);
    Value<Integer> TRIP_SCORE = new Value<>(Integer.class, "TripScore", 0, props);
    Value<Integer> LAST_SCORE = new Value<>(Integer.class, "LastScore", 0, props);
    Value<Integer> TIMEOUTS = new Value<>(Integer.class, "Timeouts", 0, props);
    Value<Integer> OFFICIAL_REVIEWS = new Value<>(Integer.class, "OfficialReviews", 0, props);
    Value<Timeout> LAST_REVIEW = new Value<>(Timeout.class, "LastReview", null, props);
    Value<Boolean> IN_TIMEOUT = new Value<>(Boolean.class, "InTimeout", false, props);
    Value<Boolean> IN_OFFICIAL_REVIEW =
            new Value<>(Boolean.class, "InOfficialReview", false, props);
    Value<Boolean> NO_PIVOT = new Value<>(Boolean.class, "NoPivot", false, props);
    Value<Boolean> RETAINED_OFFICIAL_REVIEW =
            new Value<>(Boolean.class, "RetainedOfficialReview", false, props);
    Value<Boolean> LOST = new Value<>(Boolean.class, "Lost", false, props);
    Value<Boolean> LEAD = new Value<>(Boolean.class, "Lead", false, props);
    Value<Boolean> CALLOFF = new Value<>(Boolean.class, "Calloff", false, props);
    Value<Boolean> INJURY = new Value<>(Boolean.class, "Injury", false, props);
    Value<Boolean> NO_INITIAL = new Value<>(Boolean.class, "NoInitial", true, props);
    Value<Boolean> DISPLAY_LEAD = new Value<>(Boolean.class, "DisplayLead", false, props);
    Value<Boolean> STAR_PASS = new Value<>(Boolean.class, "StarPass", false, props);
    Value<ScoringTrip> STAR_PASS_TRIP = new Value<>(ScoringTrip.class, "StarPassTrip", null, props);
    Value<PreparedTeam> PREPARED_TEAM =
            new Value<>(PreparedTeam.class, "PreparedTeam", null, props);
    Value<Boolean> PREPARED_TEAM_CONNECTED =
            new Value<>(Boolean.class, "PreparedTeamConnected", false, props);
    Value<Skater> CAPTAIN = new Value<>(Skater.class, "Captain", null, props);
    Value<ScoreAdjustment> ACTIVE_SCORE_ADJUSTMENT =
            new Value<>(ScoreAdjustment.class, "ActiveScoreAdjustment", null, props);
    Value<Integer> ACTIVE_SCORE_ADJUSTMENT_AMOUNT =
            new Value<>(Integer.class, "ActiveScoreAdjustmentAmount", 0, props);
    Value<Integer> TOTAL_PENALTIES = new Value<>(Integer.class, "TotalPenalties", 0, props);
    Value<Boolean> ALL_BLOCKERS_SET = new Value<>(Boolean.class, "AllBlockersSet", false, props);

    Child<ValWithId> ALTERNATE_NAME = new Child<>(ValWithId.class, "AlternateName", preparedProps);
    Child<ValWithId> COLOR = new Child<>(ValWithId.class, "Color", preparedProps);
    Child<Skater> SKATER = new Child<>(Skater.class, "Skater", props);
    Child<Position> POSITION = new Child<>(Position.class, "Position", props);
    Child<Timeout> TIME_OUT = new Child<>(Timeout.class, "TimeOut", props);
    Child<BoxTrip> BOX_TRIP = new Child<>(BoxTrip.class, "BoxTrip", props);
    Child<ScoreAdjustment> SCORE_ADJUSTMENT =
            new Child<>(ScoreAdjustment.class, "ScoreAdjustment", props);

    Command ADD_TRIP = new Command("AddTrip", props);
    Command REMOVE_TRIP = new Command("RemoveTrip", props);
    Command ADVANCE_FIELDINGS = new Command("AdvanceFieldings", props);
    Command TIMEOUT = new Command("Timeout", props);
    Command OFFICIAL_REVIEW = new Command("OfficialReview", props);
    Command CLEAR_SKATERS = new Command("ClearSkaters", preparedProps);

    enum AlternateNameId {
        SCOREBOARD("scoreboard"),
        WHITEBOARD("whiteboard"),
        OPERATOR("operator"),
        PLT("plt"),
        BOX("box"),
        OVERLAY("overlay"),
        TWITTER("twitter");

        AlternateNameId(String i) {
            id = i;
        }

        @Override
        public String toString() {
            return id;
        }

        private final String id;
    }

    interface TeamSnapshot {
        String getId();

        boolean getFieldingAdvancePending();
    }
}
