package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.json.JSONStateManager;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.util.ArrayList;
import java.util.Collection;

public interface ScoreBoard extends ScoreBoardEventProvider {
    /**
     * Update state after restoring from autosave
     */
    void postAutosaveUpdate();

    // convert the id into a timeoutOwner object
    TimeoutOwner getTimeoutOwner(String id);

    Settings getSettings();

    Rulesets getRulesets();

    Media getMedia();

    Clients getClients();

    Game getGame(String id);

    PreparedTeam getPreparedTeam(String id);

    CurrentGame getCurrentGame();

    JSONStateManager getJsm();

    boolean useMetrics();

    boolean isInitialLoadDone();

    Collection<Property<?>> props = new ArrayList<>();

    Value<String> BLANK_STATSBOOK_FOUND =
            new Value<>(String.class, "BlankStatsbookFound", "none", props);
    Value<Integer> IMPORTS_IN_PROGRESS = new Value<>(Integer.class, "ImportsInProgress", 0, props);

    Child<ValWithId> VERSION = new Child<>(ValWithId.class, "Version", props);
    Child<Settings> SETTINGS = new Child<>(Settings.class, "Settings", props);
    Child<Media> MEDIA = new Child<>(Media.class, "Media", props);
    Child<Clients> CLIENTS = new Child<>(Clients.class, "Clients", props);
    Child<Rulesets> RULESETS = new Child<>(Rulesets.class, "Rulesets", props);
    Child<Game> GAME = new Child<>(Game.class, "Game", props);
    Child<PreparedTeam> PREPARED_TEAM = new Child<>(PreparedTeam.class, "PreparedTeam", props);
    Child<PreparedOfficial> PREPARED_OFFICIAL =
            new Child<>(PreparedOfficial.class, "PreparedOfficial", props);
    Child<CurrentGame> CURRENT_GAME = new Child<>(CurrentGame.class, "CurrentGame", props);

    String SETTING_AUTO_START = "ScoreBoard.AutoStart";
    String SETTING_AUTO_START_BUFFER = "ScoreBoard.AutoStartBuffer";
    String SETTING_AUTO_END_JAM = "ScoreBoard.AutoEndJam";
    String SETTING_AUTO_END_TTO = "ScoreBoard.AutoEndTTO";
    String SETTING_USE_LT = "ScoreBoard.Penalties.UseLT";
    String SETTING_USE_PBT = "ScoreBoard.Penalties.UsePBT";
    String SETTING_STATSBOOK_INPUT = "ScoreBoard.Stats.InputFile";
}
