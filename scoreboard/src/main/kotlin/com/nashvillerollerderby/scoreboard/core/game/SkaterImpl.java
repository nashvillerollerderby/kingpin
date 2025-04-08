package com.nashvillerollerderby.scoreboard.core.game;

import com.nashvillerollerderby.scoreboard.core.interfaces.BoxTrip;
import com.nashvillerollerderby.scoreboard.core.interfaces.Fielding;
import com.nashvillerollerderby.scoreboard.core.interfaces.FloorPosition;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Penalty;
import com.nashvillerollerderby.scoreboard.core.interfaces.Position;
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedTeam.PreparedSkater;
import com.nashvillerollerderby.scoreboard.core.interfaces.Role;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.core.interfaces.Skater;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.core.interfaces.TeamJam;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;
import com.nashvillerollerderby.scoreboard.rules.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SkaterImpl extends ScoreBoardEventProviderImpl<Skater> implements Skater {
    public SkaterImpl(Team t, String i) {
        super(t, i == null ? UUID.randomUUID().toString() : i, Team.SKATER);
        team = t;
        game = t.getGame();
        initialize();
    }

    public SkaterImpl(Team t, PreparedSkater ps, String i) {
        super(t, i == null ? UUID.randomUUID().toString() : i, Team.SKATER);
        team = t;
        game = t.getGame();
        initialize();
        set(PREPARED_SKATER, ps);
        setFlags(ps.get(FLAGS));
    }

    public SkaterImpl(Team t, Skater source) {
        this(t, (String) null);
        PreparedSkater ps = source.get(PREPARED_SKATER);
        set(PREPARED_SKATER, ps);
        if (ps == null || !team.get(Team.PREPARED_TEAM_CONNECTED)) {
            set(NAME, source.get(NAME));
            set(ROSTER_NUMBER, source.get(ROSTER_NUMBER));
            set(PRONOUNS, source.get(PRONOUNS));
        }
        set(FLAGS, source.get(FLAGS));
    }

    private void initialize() {
        addProperties(props);
        addProperties(preparedProps);
        setInverseReference(FIELDING, Fielding.SKATER);
        setCopy(NAME, this, PREPARED_SKATER, NAME, false, team, Team.PREPARED_TEAM_CONNECTED);
        setCopy(ROSTER_NUMBER, this, PREPARED_SKATER, ROSTER_NUMBER, false, team, Team.PREPARED_TEAM_CONNECTED);
        setCopy(PRONOUNS, this, PREPARED_SKATER, PRONOUNS, false, team, Team.PREPARED_TEAM_CONNECTED);
        set(ROLE, Role.BENCH, Flag.SPECIAL_CASE);
        set(BASE_ROLE, Role.BENCH);
        addWriteProtectionOverride(BASE_ROLE, Source.ANY_INTERNAL);
        setCopy(POSITION, this, CURRENT_FIELDING, Fielding.POSITION, true);
        setCopy(PENALTY_BOX, this, CURRENT_FIELDING, Fielding.PENALTY_BOX, false);
        setCopy(CURRENT_BOX_SYMBOLS, this, CURRENT_FIELDING, Fielding.BOX_TRIP_SYMBOLS, true);
        setRecalculated(CURRENT_PENALTIES).addSource(this, PENALTY).addSource(this, PENALTY_BOX);
        setRecalculated(PENALTY_COUNT).addSource(this, PENALTY);
        setRecalculated(HAS_UNSERVED).addSource(this, PENALTY).addSource(this, EXTRA_PENALTY_TIME);
        setRecalculated(PENALTY_DETAILS).addSource(this, PENALTY).addSource(this, PENALTY_BOX);
    }

    @Override
    public int compareTo(Skater other) {
        if (other == null) {
            return -1;
        }
        if (getRosterNumber() == other.getRosterNumber()) {
            return 0;
        }
        if (getRosterNumber() == null) {
            return 1;
        }
        if (other.getRosterNumber() == null) {
            return -1;
        }
        return getRosterNumber().compareTo(other.getRosterNumber());
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == ROLE && flag != Flag.SPECIAL_CASE && !source.isFile()) {
            team.field(this, (Role) value);
            return last;
        }
        if (prop == CURRENT_PENALTIES) {
            if (isPenaltyBox() && getCurrentBoxTrip() != null) {
                return getCurrentBoxTrip().get(BoxTrip.PENALTY_CODES);
            }
            List<Penalty> penalties = getUnservedPenalties();
            Collections.sort(penalties);
            value = penalties.stream().map(Penalty::getCode).collect(Collectors.joining(" "));
        }
        if (prop == PENALTY_DETAILS) {
            if (isPenaltyBox() && getCurrentBoxTrip() != null) {
                return getCurrentBoxTrip().get(BoxTrip.PENALTY_DETAILS);
            }
            List<Penalty> penalties = getUnservedPenalties();
            Collections.sort(penalties);
            value = penalties.stream().map(Penalty::getDetails).collect(Collectors.joining(","));
        }
        if (prop == PENALTY_COUNT) {
            int count = 0;
            for (Penalty p : getAll(PENALTY)) {
                if (!FO_EXP_ID.equals(p.getProviderId())) {
                    count++;
                }
            }
            return count;
        }
        if (prop == HAS_UNSERVED) {
            if (get(EXTRA_PENALTY_TIME) > 0L) {
                return true;
            }
            for (Penalty p : getAll(PENALTY)) {
                if (!FO_EXP_ID.equals(p.getProviderId()) && !p.get(Penalty.SERVED)) {
                    return true;
                }
            }
            return false;
        }
        if (prop == PENALTY_BOX && source == Source.WS &&
                Boolean.parseBoolean(scoreBoard.getSettings().get(ScoreBoard.SETTING_USE_PBT))) {
            return last;
        }
        if (prop == EXTRA_PENALTY_TIME) {
            return ((Long) value / 1000L) * 1000L;
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == CURRENT_FIELDING && !source.isFile()) {
            Fielding f = (Fielding) value;
            Fielding lf = (Fielding) last;
            setRole(value == null ? getBaseRole() : f.getCurrentRole());
            if (lf != null && lf.getSkater() == this) {
                if (f != null && lf.getTeamJam().getNext() == f.getTeamJam() && lf.isInBox()) {
                    f.add(Fielding.BOX_TRIP, lf.getCurrentBoxTrip());
                }
            }
            if (f != null && f.getPosition().getFloorPosition() == FloorPosition.JAMMER) {
                set(COLOR, "Jammer");
            }
            if (f != null && f.getPosition().getFloorPosition() == FloorPosition.PIVOT) {
                set(COLOR, "Pivot");
            }
        }
        if (prop == FLAGS) {
            if ("C".equals(value)) {
                team.set(Team.CAPTAIN, this);
            } else if ("C".equals(last) && team.get(Team.CAPTAIN) == this) {
                team.set(Team.CAPTAIN, null);
            }
            if ("ALT".equals(value) || "BA".equals(value) || "B".equals(value)) {
                set(BASE_ROLE, Role.NOT_IN_GAME);
            } else if (get(BASE_ROLE) == Role.NOT_IN_GAME) {
                set(BASE_ROLE, Role.BENCH);
                updateEligibility();
            }
        }
        if (prop == BASE_ROLE && get(ROLE) == last) {
            set(ROLE, (Role) value, Flag.SPECIAL_CASE);
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == PENALTY) {
                return new PenaltyImpl(this, Integer.valueOf(id));
            }
            return null;
        }
    }

    @Override
    protected <T extends ValueWithId> boolean mayAdd(Child<T> prop, T item, Source source) {
        if (prop == FIELDING) {
            Fielding f = (Fielding) item;
            Fielding lf = getFielding(f.getTeamJam());
            if (lf != null && lf != f) {
                for (BoxTrip bt : lf.getAll(Fielding.BOX_TRIP)) {
                    f.add(Fielding.BOX_TRIP, bt);
                }
                lf.removeAll(Fielding.BOX_TRIP);
                lf.setSkater(null);
            }
        }
        return true;
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == PENALTY && !source.isFile()) {
            Penalty p = (Penalty) item;
            if (FO_EXP_ID.equals(p.getProviderId())) {
                updateEligibility();
            }
            if (p.getNumber() == game.getInt(Rule.FO_LIMIT)) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo == null) {
                    fo = getOrCreate(PENALTY, 0);
                    fo.set(Penalty.CODE, "FO");
                }
                if ("FO".equals(fo.get(Penalty.CODE))) {
                    fo.set(Penalty.JAM, p.getJam());
                }
            }
            if (!p.isServed() && getRole() == Role.JAMMER && getCurrentFielding() != null &&
                    !getCurrentFielding().getTeamJam().getOtherTeam().isLead() && game.isInJam()) {
                getTeam().set(Team.LOST, true);
            }
            if (!p.isServed() && !game.isInJam() && getRole(getTeam().getRunningOrUpcomingTeamJam()) == Role.BENCH) {
                getTeam().field(this, getRole(getTeam().getRunningOrEndedTeamJam()),
                        getTeam().getRunningOrUpcomingTeamJam());
            }
        } else if (prop == FIELDING) {
            Fielding f = (Fielding) item;
            if (f.isCurrent()) {
                set(CURRENT_FIELDING, f);
            }
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == PENALTY) {
            if (FO_EXP_ID.equals(((Penalty) item).getProviderId())) {
                updateEligibility();
                game.remove(Game.EXPULSION, item.getId());
            } else if (get(PENALTY, game.getInt(Rule.FO_LIMIT)) == null) {
                Penalty fo = getPenalty(FO_EXP_ID);
                if (fo != null && "FO".equals(fo.get(Penalty.CODE))) {
                    fo.delete();
                }
            }
        } else if (prop == FIELDING && getCurrentFielding() == item) {
            set(CURRENT_FIELDING, null);
        }
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public String getName() {
        return get(NAME);
    }

    @Override
    public void setName(String n) {
        set(NAME, n);
    }

    @Override
    public String getRosterNumber() {
        return get(ROSTER_NUMBER);
    }

    @Override
    public void setRosterNumber(String n) {
        set(ROSTER_NUMBER, n);
    }

    @Override
    public Fielding getFielding(TeamJam teamJam) {
        for (FloorPosition fp : FloorPosition.values()) {
            Fielding f = get(FIELDING, teamJam.getId() + "_" + fp);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    @Override
    public Fielding getCurrentFielding() {
        return get(CURRENT_FIELDING);
    }

    @Override
    public void removeCurrentFielding() {
        if (getCurrentFielding() != null) {
            remove(FIELDING, getCurrentFielding());
        }
    }

    @Override
    public void updateFielding(TeamJam teamJam) {
        set(Skater.CURRENT_FIELDING, getFielding(teamJam));
        setRole(getRole(teamJam));
        updateEligibility();
    }

    @Override
    public Position getPosition() {
        return get(POSITION);
    }

    @Override
    public void setPosition(Position p) {
        set(POSITION, p);
    }

    @Override
    public Role getRole() {
        return get(ROLE);
    }

    @Override
    public Role getRole(TeamJam tj) {
        Fielding f = getFielding(tj);
        if (f == null) {
            return getBaseRole();
        }
        return f.getCurrentRole();
    }

    @Override
    public void setRole(Role r) {
        set(ROLE, r, Flag.SPECIAL_CASE);
    }

    @Override
    public void setRoleToBase() {
        setRole(getBaseRole());
    }

    @Override
    public Role getBaseRole() {
        return get(BASE_ROLE);
    }

    @Override
    public void setBaseRole(Role b) {
        set(BASE_ROLE, b);
    }

    @Override
    public void updateEligibility() {
        if (getBaseRole() != Role.BENCH && getBaseRole() != Role.INELIGIBLE) {
            return;
        }
        if (get(PENALTY, FO_EXP_ID) != null) {
            setBaseRole(Role.INELIGIBLE);
            return;
        }
        boolean satThisPeriod = false;
        Set<TeamJam> lastN = new HashSet<>();
        TeamJam tj = getTeam().getRunningOrUpcomingTeamJam();
        int n = getTeam().hasFieldingAdvancePending() ? 5 : 4;
        while (tj != null && lastN.size() < n) {
            lastN.add(tj);
            tj = tj.getPrevious();
        }
        for (Fielding f : getAll(FIELDING)) {
            if (f.isSitFor3()) {
                if (lastN.contains(f.getTeamJam())) {
                    setBaseRole(Role.INELIGIBLE);
                    return;
                }
                if (f.getTeamJam().getJam().getParent() == game.getCurrentPeriod()) {
                    if (satThisPeriod) {
                        setBaseRole(Role.INELIGIBLE);
                        return;
                    } else {
                        satThisPeriod = true;
                    }
                }
            }
        }
        set(BASE_ROLE, Role.BENCH);
    }

    @Override
    public boolean isPenaltyBox() {
        return isPenaltyBox(false);
    }

    @Override
    public boolean isPenaltyBox(boolean checkUpcoming) {
        if (checkUpcoming && !game.isInJam() && getFielding(getTeam().getRunningOrUpcomingTeamJam()) != null) {
            return getFielding(getTeam().getRunningOrUpcomingTeamJam()).isInBox();
        } else {
            return get(PENALTY_BOX);
        }
    }

    @Override
    public void setPenaltyBox(boolean box) {
        set(PENALTY_BOX, box);
    }

    @Override
    public String getFlags() {
        return get(FLAGS);
    }

    @Override
    public void setFlags(String f) {
        set(FLAGS, f);
    }

    @Override
    public BoxTrip getCurrentBoxTrip() {
        if (!game.isInJam() && getFielding(getTeam().getRunningOrUpcomingTeamJam()) != null) {
            return getFielding(getTeam().getRunningOrUpcomingTeamJam()).getCurrentBoxTrip();
        } else if (getCurrentFielding() != null) {
            return getCurrentFielding().getCurrentBoxTrip();
        } else {
            return null;
        }
    }

    @Override
    public Penalty getPenalty(String id) {
        return get(PENALTY, id);
    }

    @Override
    public List<Penalty> getUnservedPenalties() {
        List<Penalty> usp = new ArrayList<>();
        for (Penalty p : getAll(PENALTY)) {
            if (!p.isServed() && !p.getProviderId().equals(FO_EXP_ID)) {
                usp.add(p);
            }
        }
        return usp;
    }

    @Override
    public boolean hasUnservedPenalties() {
        return !getUnservedPenalties().isEmpty() || getExtraPenaltyTime() > 0L;
    }

    @Override
    public long getExtraPenaltyTime() {
        return get(EXTRA_PENALTY_TIME);
    }

    @Override
    public void mergeInto(PreparedSkater ps) {
        if ("".equals(ps.get(NAME))) {
            ps.set(NAME, get(NAME));
        }
        if ("".equals(ps.get(ROSTER_NUMBER))) {
            ps.set(ROSTER_NUMBER, get(ROSTER_NUMBER));
        }
        if ("".equals(ps.get(PRONOUNS))) {
            ps.set(PRONOUNS, get(PRONOUNS));
        }
        if ("".equals(get(NAME))) {
            set(NAME, ps.get(NAME));
        }
        if ("".equals(get(ROSTER_NUMBER))) {
            set(ROSTER_NUMBER, ps.get(ROSTER_NUMBER));
        }
        if ("".equals(get(PRONOUNS))) {
            set(PRONOUNS, ps.get(PRONOUNS));
        }
        set(PREPARED_SKATER, ps);
    }

    protected Team team;
    private final Game game;
}
