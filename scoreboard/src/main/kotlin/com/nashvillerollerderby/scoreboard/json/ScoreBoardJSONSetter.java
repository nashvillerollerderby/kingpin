package com.nashvillerollerderby.scoreboard.json;

import com.nashvillerollerderby.scoreboard.core.interfaces.Clients;
import com.nashvillerollerderby.scoreboard.core.interfaces.CurrentGame;
import com.nashvillerollerderby.scoreboard.core.interfaces.Expulsion;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider.Flag;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider.Source;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.utils.Log4j2Logging;
import com.nashvillerollerderby.scoreboard.utils.ValWithId;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bulk set ScoreBoard atttributes with JSON paths.
 */
public class ScoreBoardJSONSetter {

    private final Logger logger = Log4j2Logging.getLogger(this);

    // check the version of the incoming update and update if necessary
    public static void updateToCurrentVersion(Map<String, Object> state) {
        Logger logger = Log4j2Logging.getLogger(ScoreBoardJSONSetter.class);
        String version = (String) state.get("ScoreBoard.Version(release)");
        if (version == null) {
            version = getVersionFromKeys(state.keySet());
        }

        if (version.startsWith("v2025")) {
            return;
        } // no update needed

        logger.info("Updating import from version {}", version);

        // When updating to v5 we need to move stuff to a newly created game which needs an id
        String newGameId = UUID.randomUUID().toString();

        for (String oldKey : new HashSet<>(state.keySet())) {
            String newKey = oldKey;
            String keyVersion = version;

            if (keyVersion.startsWith("v4.0")) {
                if ((newKey.startsWith("ScoreBoard.Clock(") && newKey.endsWith(".MinimumTime"))) {
                    newKey = "";
                } else if (newKey.startsWith("ScoreBoard.Team(") && newKey.endsWith(".LastEndedTeamJam")) {
                    newKey = "";
                } else if (newKey.endsWith(".Number") &&
                        (newKey.contains(".Position(") || newKey.contains(".Skater(")) &&
                        !newKey.contains(".Penalty(")) {
                    newKey = newKey.replace(".Number", ".RosterNumber");
                }
                keyVersion = "v4.1";
            }

            if (keyVersion.startsWith("v4.1")) {
                if (newKey.startsWith("ScoreBoard.Rulesets.CurrentRule(")) {
                    newKey =
                            newKey.replace("ScoreBoard.Rulesets.CurrentRule(", "ScoreBoard.Game(" + newGameId + ").Rule(");
                } else if (newKey.startsWith("ScoreBoard.Rulesets.Current") ||
                        newKey.startsWith("ScoreBoard.Rulesets.RuleDefinition(")) {
                    newKey = "";
                } else if (newKey.startsWith("ScoreBoard.Rulesets.Ruleset(") && newKey.endsWith(".ParentId")) {
                    newKey = newKey.replace(".ParentId", ".Parent");
                } else if (newKey.startsWith("ScoreBoard.PenaltyCodes.Code(")) {
                    newKey = newKey.replace("ScoreBoard.PenaltyCodes.Code(",
                            "ScoreBoard.Game(" + newGameId + ").PenaltyCode(");
                } else if (newKey.equals("ScoreBoard.Team(1).Name") || newKey.equals("ScoreBoard.Team(2).Name")) {
                    newKey = newKey.replace("ScoreBoard.", "ScoreBoard.Game(" + newGameId + ").")
                            .replace(".Name", ".TeamName");
                } else if (newKey.startsWith("ScoreBoard.Clock(") || newKey.startsWith("ScoreBoard.Period(") ||
                        newKey.startsWith("ScoreBoard.Jam(") || newKey.startsWith("ScoreBoard.Team(") ||
                        newKey.equals("ScoreBoard.CurrentPeriodNumber") ||
                        newKey.equals("ScoreBoard.CurrentPeriod") || newKey.equals("ScoreBoard.UpcomingJam") ||
                        newKey.equals("ScoreBoard.UpcomingJamNumber") || newKey.equals("ScoreBoard.InPeriod") ||
                        newKey.equals("ScoreBoard.InJam") || newKey.equals("ScoreBoard.InOvertime") ||
                        newKey.equals("ScoreBoard.OfficialScore") || newKey.equals("ScoreBoard.CurrentTimeout") ||
                        newKey.equals("ScoreBoard.TimeoutOwner") || newKey.equals("ScoreBoard.OfficialReview") ||
                        newKey.equals("ScoreBoard.NoMoreJam")) {
                    newKey = newKey.replace("ScoreBoard.", "ScoreBoard.Game(" + newGameId + ").");
                } else if (newKey.startsWith("ScoreBoard.PreparedTeam") && newKey.endsWith(".Name") &&
                        !newKey.contains("Skater")) {
                    newKey = newKey.replace(".Name", ".TeamName");
                }

                // changed values
                if (newKey.contains(".Skater(") && newKey.endsWith(".Flags")) {
                    String oldValue = (String) state.get(oldKey);
                    if ("BC".equals(oldValue)) {
                        state.put(oldKey, "BA");
                    }
                    if ("AC".equals(oldValue)) {
                        state.put(oldKey, "A");
                    }
                }
                keyVersion = "v5";
            }

            if (keyVersion.startsWith("v5")) {
                if (newKey.startsWith("ScoreBoard.Twitter")) {
                    newKey = "";
                }

                keyVersion = "v2023";
            }

            if (keyVersion.startsWith("v2023")) {

                if (newKey.startsWith("ScoreBoard.Clients.Client") ||
                        (newKey.startsWith("ScoreBoard.Settings.Setting(ScoreBoard.Operator") &&
                                newKey.endsWith("StartStopButtons)")) ||
                        newKey.equals("ScoreBoard.Settings.Setting(ScoreBoard.ClockAfterTimeout)") ||
                        newKey.endsWith("FirstJam") || newKey.endsWith("FirstJamNumber")) {
                    newKey = "";
                }
                if (newKey.startsWith("ScoreBoard.Settings.Setting(ScoreBoard.Operator__")) {
                    newKey = newKey.replace("Operator__", ".");
                }
                if (newKey.contains("Jam.SuddenScoringMaxTrainingPoints")) {
                    newKey = newKey.replace("Jam.SuddenScoringMaxTrainingPoints", "Jam.SuddenScoringMaxTrailingPoints");
                }

                // changed values
                if (newKey.startsWith("ScoreBoard.Settings.Setting(Overlay.Interactive")) {
                    String oldValue = (String) state.get(oldKey);
                    if ("On".equals(oldValue)) {
                        state.put(oldKey, "true");
                    }
                    if ("Off".equals(oldValue)) {
                        state.put(oldKey, "false");
                    }
                }
                if (newKey.startsWith("ScoreBoard.Settings.Setting(" + ScoreBoard.SETTING_AUTO_END_JAM)) {
                    state.put(oldKey, "false");
                }
                if (newKey.contains("Color(") && newKey.contains("_fg)")) {
                    newKey = newKey.replace("_fg)", ".fg)");
                }
                if (newKey.contains("Color(") && newKey.contains("_bg)")) {
                    newKey = newKey.replace("_bg)", ".bg)");
                }
                if (newKey.contains("Color(") && newKey.contains("_glow)")) {
                    newKey = newKey.replace("_glow)", ".glow)");
                }
                if (newKey.startsWith("ScoreBoard.PreparedTeam") && newKey.contains("UniformColor(")) {
                    String teamId = newKey.substring("ScoreBoard.PreparedTeam(".length(), newKey.indexOf(")"));
                    newKey = "ScoreBoard.PreparedTeam(" + teamId + ").UniformColor(" + state.get(oldKey) + ")";
                }

                keyVersion = "v2025";
            }

            if (!newKey.equals(oldKey)) {
                if (!newKey.equals("")) {
                    state.put(newKey, state.get(oldKey));
                }
                state.remove(oldKey);
            }
        }
    }

    private static String getVersionFromKeys(Set<String> keys) {
        String minVersion = "v4.0";  // lowest version possible from the keys seen so far
        String maxVersion = "v2025"; // highest version possible from the keys seen so far

        for (String key : keys) {
            minVersion = minVersionWith(key, minVersion);
            maxVersion = maxVersionWith(key, maxVersion);
            if (minVersion.equals(maxVersion)) return minVersion;
        }
        // return highest possible version so unapplicable updates are skippped
        return maxVersion;
    }

    private static String minVersionWith(String key, String priorLimit) {
        if (priorLimit.equals("v2025") || key.equals("ScoreBoard.Settings.Setting(Overlay.Interactive.ShowNames)") ||
                key.equals("ScoreBoard.Settings.Setting(Overlay.Interactive.ShowPenaltyClocks)") ||
                key.equals("ScoreBoard.Settings.Setting(ScoreBoard.Preview_HidePenaltyClocks)") ||
                key.equals("ScoreBoard.Settings.Setting(ScoreBoard.View_HidePenaltyClocks)") ||
                key.equals("ScoreBoard.Settings.Setting(ScoreBoard.Preview_HideBanners)") ||
                key.equals("ScoreBoard.Settings.Setting(ScoreBoard.View_HideBanners)") ||
                key.equals("ScoreBoard.Settings.Setting(ScoreBoard.UsePBT)") ||
                key.contains("Jam.SuddenScoringMaxTrailingPoints") || key.endsWith("PenaltyDetails") ||
                (key.contains("BoxTrip(") && key.endsWith("CurrentSkater")) ||
                (key.contains("BoxTrip(") && key.endsWith("RosterNumber")) ||
                (key.contains("BoxTrip(") && key.endsWith("PenaltyCodes")) ||
                (key.contains("BoxTrip(") && key.endsWith("TotalPenalties")) ||
                (key.contains("BoxTrip(") && key.endsWith("TimingStopped")) ||
                (key.contains("BoxTrip(") && key.endsWith("Time")) ||
                (key.contains("BoxTrip(") && key.endsWith("Shortened")) ||
                (key.contains("BoxTrip(") && key.contains("Clock(")) ||
                (key.contains("Fielding(") && key.endsWith("PenaltyTime")) ||
                (key.contains("Position(") && key.endsWith("HasUnserved")) ||
                (key.contains("Position(") && key.endsWith("PenaltyTime")) ||
                (key.contains("Position(") && key.endsWith("PenaltyCount")) ||
                (key.contains("Skater(") && key.endsWith("PenaltyCount")) ||
                (key.contains("Skater(") && key.endsWith("HasUnserved")) ||
                (key.contains("Timeout(") && key.endsWith("OrRequest")) ||
                (key.contains("Timeout(") && key.endsWith("OrResult")) ||
                (key.contains("Team(") && key.endsWith("AllBlockersSet")) || key.contains("(Timeout.JamDuring)") ||
                key.contains("PreparedOfficial") || key.endsWith("ExtraPenaltyTime")) {
            return "v2025";
        }
        if (priorLimit.equals("v2023") || key.endsWith("ExportBlockedBy") || key.contains("ScoreAdjustment") ||
                key.endsWith("Team(1).TotalPenalties") || key.endsWith("Team(2).TotalPenalties") ||
                (key.contains("Skater(") && key.endsWith("Pronouns")) ||
                (key.contains("Skater(") && key.endsWith("Color")) ||
                (key.contains("Period(") && key.endsWith("PenaltyCount")) ||
                (key.contains("Period(") && key.endsWith("Points"))) {
            return "v2023";
        }
        if (priorLimit.equals("v5") || key.startsWith("ScoreBoard.Game(") ||
                key.startsWith("ScoreBoard.CurrentGame.") || key.equals("ScoreBoard.BlankStatsbookFound") ||
                key.equals("ScoreBoard.ImportsInProgress")) {
            return "v5";
        }
        if (priorLimit.equals("v4.1") || key.startsWith("ScoreBoard.Clients.") || key.endsWith(".RosterNumber") ||
                key.endsWith(".ReadOnly") || (key.endsWith(".Annotation") && key.contains(".ScoringTrip("))) {
            return "v4.1";
        }
        return priorLimit;
    }

    private static String maxVersionWith(String key, String priorLimit) {
        if (priorLimit.equals("v4.0") || (key.startsWith("ScoreBoard.Clock(") && key.endsWith(".MinimumTime")) ||
                (key.startsWith("ScoreBoard.Team(") && key.endsWith(".LastEndedTeamJam")) ||
                (key.endsWith(".Number") && (key.contains(".Position(") || key.contains(".Skater(")))) {
            return "v4.0";
        }
        if (priorLimit.equals("v4.1") || key.startsWith("ScoreBoard.Clock(") || key.startsWith("ScoreBoard.Period(") ||
                key.startsWith("ScoreBoard.Jam(") || key.startsWith("ScoreBoard.Team(") ||
                key.startsWith("ScoreBoard.PenaltyCodes.") || key.startsWith("ScoreBoard.Rulesets.Current") ||
                (key.startsWith("ScoreBoard.Rulesets.Ruleset(") && key.endsWith(".ParentId")) ||
                key.equals("ScoreBoard.CurrentPeriodNumber") || key.equals("ScoreBoard.CurrentPeriod") ||
                key.equals("ScoreBoard.UpcomingJam") || key.equals("ScoreBoard.UpcomingJamNumber") ||
                key.equals("ScoreBoard.InPeriod") || key.equals("ScoreBoard.InJam") ||
                key.equals("ScoreBoard.InOvertime") || key.equals("ScoreBoard.OfficialScore") ||
                key.equals("ScoreBoard.CurrentTimeout") || key.equals("ScoreBoard.TimeoutOwner") ||
                key.equals("ScoreBoard.OfficialReview") || key.equals("ScoreBoard.NoMoreJam")) {
            return "v4.1";
        }
        if (priorLimit.equals("v5") || key.startsWith("ScoreBoard.Twitter")) {
            return "v5";
        }
        if (priorLimit.equals("v2023") || key.equals("ScoreBoard.Settings.Setting(Overlay.Interactive.ShowAllNames)") ||
                (key.startsWith("ScoreBoard.Settings.Setting(ScoreBoard.Operator") && key.endsWith("StartStopButtons")) ||
                key.contains("Jam.SuddenScoringMaxTrainingPoints") || key.contains("ScoreBoard.ClockAfterTimeout") ||
                key.startsWith("ScoreBoard.Clients.Client(") || key.endsWith("FirstJam") ||
                key.endsWith("FirstJamNumber")) {
            return "v2023";
        }

        return priorLimit;
    }

    // Make a list of sets to a scoreboard, with JSON paths to fields.
    public static void set(ScoreBoard sb, Map<String, Object> state, Source source) {
        List<JSONSet> jsl = new ArrayList<>();
        for (String key : state.keySet()) {
            Object value = state.get(key);
            String v;
            if (value == null) {
                v = null;
            } else {
                v = value.toString();
            }
            jsl.add(new JSONSet(key, v, null));
        }
        ScoreBoardJSONSetter.set(sb, jsl, source);
    }

    public static void set(ScoreBoard sb, List<JSONSet> jsl, Source source) {
        Logger logger = Log4j2Logging.getLogger(ScoreBoardJSONSetter.class);
        List<PropertySet> postponedSets = new ArrayList<>();
        for (JSONSet s : jsl) {
            Matcher m = pathElementPattern.matcher(s.path);
            if (m.matches() && m.group("name").equals("ScoreBoard") && m.group("id") == null &&
                    m.group("remainder") != null) {
                set(sb, m.group("remainder"), s.value, source, s.flag, postponedSets);
            } else {
                logger.error("Illegal path: {}", s.path);
            }
        }
        for (PropertySet vs : postponedSets) {
            vs.process();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void set(ScoreBoardEventProvider p, String path, String value, Source source, Flag flag,
                            List<PropertySet> postponedSets) {
        Logger logger = Log4j2Logging.getLogger(ScoreBoardJSONSetter.class);
        Matcher m = pathElementPattern.matcher(path);
        if (m.matches()) {
            String name = m.group("name");
            String elementId = m.group("id");
            String remainder = m.group("remainder");
            if (elementId == null) {
                elementId = "";
            }
            String readable = p.getProviderName() + "(" + p.getProviderId() + ")." + name + "(" + elementId + ")";
            try {
                Property prop = p.getProperty(name);
                if (prop == null) {
                    logger.warn("Unknown property {}", readable);
                    return;
                }

                if (prop == ScoreBoardEventProvider.ID) {
                    p.set((Value) prop, p.valueFromString((Value) prop, value), source, flag);
                } else if (prop instanceof Value) {
                    // postpone setting PermanentProperties except ID, as they may reference
                    // elements not yet created when restoring from autosave
                    postponedSets.add(new ValueSet(p, (Value) prop, value, source, flag));
                } else if (prop instanceof Command) {
                    if (Boolean.parseBoolean(value)) {
                        p.execute((Command) prop, source);
                    }
                } else if (remainder != null) {
                    @SuppressWarnings("unchecked")
                    ScoreBoardEventProvider o =
                            p.getOrCreate((Child<? extends ScoreBoardEventProvider>) prop, elementId, source);
                    if (o == null) {
                        if (source.isFile()) {
                            // Expulsion data can only be set after the corresponding penalty has been added
                            if (prop == Game.EXPULSION) {
                                postponedSets.add(new ExpulsionSet(p, (Child<Expulsion>) prop, elementId, source, flag,
                                        remainder, value));
                                return;
                            }
                            // filter out elements that we intentionally drop
                            if (p.getProviderClass() == CurrentGame.class) {
                                return;
                            }
                            if (prop == Clients.Device.CLIENT) {
                                return;
                            }
                        }
                        logger.warn("Could not get or create property {}", readable);
                        return;
                    }
                    set(o, remainder, value, source, flag, postponedSets);
                } else if (value == null) {
                    p.remove((Child<?>) prop, elementId, source);
                } else if (prop.getType() == ValWithId.class) {
                    Child aprop = (Child) prop;
                    p.add(aprop, p.childFromString(aprop, elementId, value), source);
                } else {
                    postponedSets.add(new ChildSet(p, (Child) prop, elementId, value, source));
                }
            } catch (Exception e) {
                logger.error("Exception handling update for {} - {}: {}", readable, value, e);
            }
        } else {
            logger.error("Illegal path element: {}", path);
        }
    }

    public static class JSONSet {
        public JSONSet(String path, String value, Flag flag) {
            this.path = path;
            this.value = value;
            this.flag = flag;
        }

        public final String path;
        public final String value;
        public final Flag flag;
    }

    protected interface PropertySet {
        void process();
    }

    protected static class ValueSet<T> implements PropertySet {
        protected ValueSet(ScoreBoardEventProvider sbe, Value<T> prop, String value, Source source, Flag flag) {
            this.sbe = sbe;
            this.prop = prop;
            this.value = value;
            this.source = source;
            this.flag = flag;
        }

        @Override
        public void process() {
            sbe.set(prop, sbe.valueFromString(prop, value), source, flag);
        }

        private final ScoreBoardEventProvider sbe;
        private final Value<T> prop;
        private final String value;
        private final Source source;
        private final Flag flag;
    }

    protected static class ChildSet<T extends ScoreBoardEventProvider> implements PropertySet {
        protected ChildSet(ScoreBoardEventProvider sbe, Child<T> prop, String id, String value, Source source) {
            this.sbe = sbe;
            this.prop = prop;
            this.id = id;
            this.value = value;
            this.source = source;
        }

        @Override
        public void process() {
            sbe.add(prop, sbe.childFromString(prop, id, value), source);
        }

        private final ScoreBoardEventProvider sbe;
        private final Child<T> prop;
        private final String id;
        private final String value;
        private final Source source;
    }

    protected static class ExpulsionSet implements PropertySet {
        protected ExpulsionSet(ScoreBoardEventProvider parent, Child<Expulsion> prop, String id, Source source,
                               Flag flag, String remainder, String value) {
            this.parent = parent;
            this.prop = prop;
            this.id = id;
            this.source = source;
            this.flag = flag;
            this.remainder = remainder;
            this.value = value;
        }

        @Override
        public void process() {
            Logger logger = Log4j2Logging.getLogger(ExpulsionSet.class);
            Expulsion e = parent.getOrCreate(prop, id, source);
            if (e == null) {
                logger.error("Failed to import data for expulsion {}", id);
                return;
            }
            List<PropertySet> postponedSets = new ArrayList<>();
            set(e, remainder, value, source, flag, postponedSets);
            for (PropertySet s : postponedSets) {
                s.process();
            }
        }

        private final ScoreBoardEventProvider parent;
        private final Child<Expulsion> prop;
        private final String id;
        private final Source source;
        private final Flag flag;
        private final String remainder;
        private final String value;
    }

    private static final Pattern pathElementPattern =
            Pattern.compile("^(?<name>\\w+)(\\((?<id>[^\\)]*)\\))?(\\.(?<remainder>.*))?$");
}
