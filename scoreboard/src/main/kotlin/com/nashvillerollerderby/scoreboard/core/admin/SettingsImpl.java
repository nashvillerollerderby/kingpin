package com.nashvillerollerderby.scoreboard.core.admin;

import com.nashvillerollerderby.scoreboard.core.interfaces.Clock;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.core.interfaces.Settings;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;
import com.nashvillerollerderby.scoreboard.utils.StatsbookExporter;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;

import java.nio.file.Paths;

public class SettingsImpl extends ScoreBoardEventProviderImpl<Settings> implements Settings {
    public SettingsImpl(ScoreBoard s) {
        super(s, "", ScoreBoard.SETTINGS);
        addProperties(props);
        setDefaults();
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (item != null) {
            if (ScoreBoard.SETTING_STATSBOOK_INPUT.equals(item.getId())) {
                boolean found = Paths.get(item.getValue()).toFile().canRead();
                if (found && scoreBoard.isInitialLoadDone()) {
                    StatsbookExporter.preload(item.getValue(), getScoreBoard());
                    for (Game g : scoreBoard.getAll(ScoreBoard.GAME)) {
                        g.clearStatsbookError();
                    }
                } else if (!found) {
                    getScoreBoard().set(ScoreBoard.BLANK_STATSBOOK_FOUND, "none");
                }
            } else if (ScoreBoard.SETTING_USE_PBT.equals(item.getId()) && "true".equals(item.getValue())) {
                set(ScoreBoard.SETTING_USE_LT, "true");
            } else if (ScoreBoard.SETTING_USE_LT.equals(item.getId()) && "false".equals(item.getValue())) {
                set(ScoreBoard.SETTING_USE_PBT, "false");
            }
        }
    }

    private void setDefaults() {
        set("Overlay.Interactive.Clock", "true");
        set("Overlay.Interactive.Scaling", "100");
        set("Overlay.Interactive.TeamNameBoxWidth", "12");
        set("Overlay.Interactive.Score", "true");
        set("Overlay.Interactive.ShowJammers", "true");
        set("Overlay.Interactive.ShowLineups", "true");
        set("Overlay.Interactive.ShowNames", "true");
        set("Overlay.Interactive.ShowPenaltyClocks", "true");
        set("ScoreBoard.Operator_Default.TabBar", "true");
        set("ScoreBoard.Operator_Default.ReplaceButton", "false");
        set("ScoreBoard.Operator_Default.ScoreAdjustments", "false");
        set(ScoreBoard.SETTING_USE_LT, "false");
        set(ScoreBoard.SETTING_USE_PBT, "false");
        set(ScoreBoard.SETTING_STATSBOOK_INPUT, "");
        set(ScoreBoard.SETTING_AUTO_START, "");
        set(ScoreBoard.SETTING_AUTO_START_BUFFER, "0:02");
        set(ScoreBoard.SETTING_AUTO_END_JAM, "false");
        set(ScoreBoard.SETTING_AUTO_END_TTO, "false");
        set(Clock.SETTING_SYNC, "true");
        set(Team.SETTING_DISPLAY_NAME, Team.OPTION_LEAGUE_NAME);
        set(Game.SETTING_DEFAULT_NAME_FORMAT, "%d %G %1 vs. %2 (%s: %S)");
        set("ScoreBoard.Intermission.PreGame", "Time To Derby");
        set("ScoreBoard.Intermission.Intermission", "Intermission");
        set("ScoreBoard.Intermission.Unofficial", "Unofficial Score");
        set("ScoreBoard.Intermission.Official", "Final Score");
        set("ScoreBoard.Intermission.OfficialWithClock", "Final Score");

        setBothViews("BoxStyle", "box_flat_bright");
        setBothViews("CurrentView", "scoreboard");
        setBothViews("CustomHtml", "/customhtml/fullscreen/example.html");
        setBothViews("Image", "/images/fullscreen/test-image.png");
        setBothViews("ImageScaling", "contain");
        setBothViews("HideBanners", "false");
        setBothViews("HideLogos", "false");
        setBothViews("HidePenaltyClocks", "false");
        setBothViews("SidePadding", "");
        setBothViews("SwapTeams", "false");
        setBothViews("Video", "/videos/fullscreen/test-video.webm");
        setBothViews("VideoScaling", "contain");
    }

    @Override
    public String get(String k) {
        synchronized (coreLock) {
            if (get(SETTING, k) == null) {
                return null;
            }
            return get(SETTING, k).getValue();
        }
    }

    @Override
    public void set(String k, String v) {
        synchronized (coreLock) {
            if (v == null) {
                remove(SETTING, k);
            } else {
                add(SETTING, new ValWithId(k, v));
            }
        }
    }

    private void setBothViews(String key, String value) {
        set("ScoreBoard.Preview_" + key, value);
        set("ScoreBoard.View_" + key, value);
    }
}
