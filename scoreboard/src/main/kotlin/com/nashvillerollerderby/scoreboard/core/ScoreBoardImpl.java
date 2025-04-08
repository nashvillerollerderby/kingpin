package com.nashvillerollerderby.scoreboard.core;

import com.nashvillerollerderby.scoreboard.core.admin.ClientsImpl;
import com.nashvillerollerderby.scoreboard.core.admin.MediaImpl;
import com.nashvillerollerderby.scoreboard.core.admin.SettingsImpl;
import com.nashvillerollerderby.scoreboard.core.current.CurrentGameImpl;
import com.nashvillerollerderby.scoreboard.core.game.GameImpl;
import com.nashvillerollerderby.scoreboard.core.interfaces.Clients;
import com.nashvillerollerderby.scoreboard.core.interfaces.CurrentGame;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Media;
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedTeam;
import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.core.interfaces.Settings;
import com.nashvillerollerderby.scoreboard.core.interfaces.Timeout;
import com.nashvillerollerderby.scoreboard.core.interfaces.TimeoutOwner;
import com.nashvillerollerderby.scoreboard.core.prepared.PreparedOfficialImpl;
import com.nashvillerollerderby.scoreboard.core.prepared.PreparedTeamImpl;
import com.nashvillerollerderby.scoreboard.core.prepared.RulesetsImpl;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;
import com.nashvillerollerderby.scoreboard.json.JSONStateManager;
import com.nashvillerollerderby.scoreboard.utils.StatsbookExporter;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;
import com.nashvillerollerderby.scoreboard.utils.Version;

import java.util.Map;

public class ScoreBoardImpl extends ScoreBoardEventProviderImpl<ScoreBoard> implements ScoreBoard {
    public ScoreBoardImpl(boolean useMetrics) {
        super(null, "", null);
        this.useMetrics = useMetrics;
        jsm = new JSONStateManager(useMetrics);
        addProperties(props);
        setupScoreBoard();
    }

    protected void setupScoreBoard() {
        removeAll(VERSION);
        for (Map.Entry<String, String> entry : Version.getAll().entrySet()) {
            add(VERSION, new ValWithId(entry.getKey(), entry.getValue()));
        }
        addWriteProtection(VERSION);
        add(SETTINGS, new SettingsImpl(this));
        addWriteProtection(SETTINGS);
        add(RULESETS, new RulesetsImpl(this));
        addWriteProtection(RULESETS);
        add(MEDIA, new MediaImpl(this));
        addWriteProtection(MEDIA);
        add(CLIENTS, new ClientsImpl(this));
        addWriteProtection(CLIENTS);
        add(CURRENT_GAME, new CurrentGameImpl(this));
        addWriteProtection(CURRENT_GAME);
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == GAME && item == getCurrentGame().getSourceElement()) {
            getCurrentGame().set(CurrentGame.GAME, null);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PREPARED_TEAM) {
                return new PreparedTeamImpl(this, id);
            }
            if (prop == PREPARED_OFFICIAL) {
                return new PreparedOfficialImpl(this, id);
            }
            if (prop == GAME) {
                return new GameImpl(this, id);
            }
            return null;
        }
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            for (Game g : getAll(GAME)) {
                g.postAutosaveUpdate();
            }
            get(CURRENT_GAME, "").postAutosaveUpdate();
            get(CLIENTS, "").postAutosaveUpdate();
            initialLoadDone = true;
            StatsbookExporter.preload(getSettings().get(SETTING_STATSBOOK_INPUT), this);
        }
    }

    @Override
    public Settings getSettings() {
        return get(SETTINGS, "");
    }

    @Override
    public Rulesets getRulesets() {
        return get(RULESETS, "");
    }

    @Override
    public Media getMedia() {
        return get(MEDIA, "");
    }

    @Override
    public Clients getClients() {
        return get(CLIENTS, "");
    }

    @Override
    public Game getGame(String id) {
        return get(GAME, id);
    }

    @Override
    public PreparedTeam getPreparedTeam(String id) {
        return get(PREPARED_TEAM, id);
    }

    @Override
    public CurrentGame getCurrentGame() {
        return get(CURRENT_GAME, "");
    }

    @Override
    public TimeoutOwner getTimeoutOwner(String id) {
        if (id == null) {
            id = "";
        }
        for (Timeout.Owners o : Timeout.Owners.values()) {
            if (o.getId().equals(id)) {
                return o;
            }
        }
        if (id.contains("_")) { // gameId_teamId
            String[] parts = id.split("_");
            Game g = get(GAME, parts[0]);
            if (g != null && g.getTeam(parts[1]) != null) {
                return g.getTeam(parts[1]);
            }
        }
        return Timeout.Owners.NONE;
    }

    @Override
    public JSONStateManager getJsm() {
        return jsm;
    }

    @Override
    public boolean useMetrics() {
        return useMetrics;
    }

    @Override
    public boolean isInitialLoadDone() {
        return initialLoadDone;
    }

    private final JSONStateManager jsm;
    private final boolean useMetrics;
    private boolean initialLoadDone = false;
}
