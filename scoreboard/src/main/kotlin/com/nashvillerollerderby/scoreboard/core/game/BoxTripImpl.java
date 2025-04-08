package com.nashvillerollerderby.scoreboard.core.game;

import com.nashvillerollerderby.scoreboard.core.interfaces.BoxTrip;
import com.nashvillerollerderby.scoreboard.core.interfaces.Clock;
import com.nashvillerollerderby.scoreboard.core.interfaces.Fielding;
import com.nashvillerollerderby.scoreboard.core.interfaces.FloorPosition;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Jam;
import com.nashvillerollerderby.scoreboard.core.interfaces.Penalty;
import com.nashvillerollerderby.scoreboard.core.interfaces.Position;
import com.nashvillerollerderby.scoreboard.core.interfaces.Role;
import com.nashvillerollerderby.scoreboard.core.interfaces.Skater;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;
import com.nashvillerollerderby.scoreboard.rules.Rule;
import com.nashvillerollerderby.scoreboard.utils.ScoreBoardClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BoxTripImpl extends ScoreBoardEventProviderImpl<BoxTrip> implements BoxTrip {
    public BoxTripImpl(Game g, String id) {
        super(g, id, Team.BOX_TRIP);
        game = g;
        addProperties(props);
        initReferences();
    }

    public BoxTripImpl(Game g) {
        super(g, UUID.randomUUID().toString(), Team.BOX_TRIP);
        game = g;
        addProperties(props);
        set(WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(START_AFTER_S_P, game.isInJam() && game.getCurrentPeriod().getCurrentJam().get(Jam.STAR_PASS));
        set(START_BETWEEN_JAMS, !game.isInJam());
        set(JAM_CLOCK_START, startedBetweenJams() ? 0L : game.getClock(Clock.ID_JAM).getTimeElapsed());
        set(IS_CURRENT, true);
        add(CLOCK, new ClockImpl(this));
        if (game.isInJam()) {
            getClock().start();
        }
        initReferences();
    }

    public BoxTripImpl(Team t, String id) {
        super(t, id, Team.BOX_TRIP);
        game = t.getGame();
        addProperties(props);
        initReferences();
    }

    public BoxTripImpl(Fielding f) {
        super(f.getTeamJam().getTeam(), UUID.randomUUID().toString(), Team.BOX_TRIP);
        game = f.getTeamJam().getTeam().getGame();
        addProperties(props);
        set(WALLTIME_START, ScoreBoardClock.getInstance().getCurrentWalltime());
        set(START_AFTER_S_P, f.getTeamJam().isStarPass() && f.isCurrent());
        set(START_BETWEEN_JAMS, !game.isInJam() && !getTeam().hasFieldingAdvancePending() && f.isCurrent());
        set(JAM_CLOCK_START, startedBetweenJams() ? 0L : game.getClock(Clock.ID_JAM).getTimeElapsed());
        set(IS_CURRENT, true);
        initReferences();
        add(FIELDING, f);
    }

    private void initReferences() {
        setInverseReference(FIELDING, Fielding.BOX_TRIP);
        setInverseReference(PENALTY, Penalty.BOX_TRIP);
        setRecalculated(DURATION).addSource(this, JAM_CLOCK_START).addSource(this, JAM_CLOCK_END);
        setRecalculated(PENALTY_CODES).addSource(this, PENALTY);
        setRecalculated(PENALTY_DETAILS).addSource(this, PENALTY_CODES);
        setCopy(START_JAM_NUMBER, this, START_FIELDING, Fielding.NUMBER, true);
        setCopy(END_JAM_NUMBER, this, END_FIELDING, Fielding.NUMBER, true);
        setCopy(ROSTER_NUMBER, this, CURRENT_FIELDING, Fielding.SKATER_NUMBER, true);
        setCopy(CURRENT_SKATER, this, CURRENT_FIELDING, Fielding.SKATER, true);
        setCopy(TOTAL_PENALTIES, this, CURRENT_SKATER, Skater.PENALTY_COUNT, true);
    }

    @Override
    public int compareTo(BoxTrip other) {
        if (other == null) {
            return -1;
        }
        if (getStartFielding() == other.getStartFielding()) {
            if (getEndFielding() == other.getEndFielding()) {
                return (int) (get(BoxTrip.WALLTIME_START) - other.get(BoxTrip.WALLTIME_START));
            }
            if (getEndFielding() == null) {
                return 1;
            }
            return getEndFielding().compareTo(other.getEndFielding());
        }
        if (getStartFielding() == null) {
            return 1;
        }
        return getStartFielding().compareTo(other.getStartFielding());
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == DURATION) {
            long time = 0;
            if (!isCurrent() && get(JAM_CLOCK_START) != null && get(JAM_CLOCK_END) != null) {
                for (Fielding f : getAll(FIELDING)) {
                    if (f == getEndFielding()) {
                        time += get(JAM_CLOCK_END);
                    } else {
                        time += f.getTeamJam().getJam().getDuration();
                    }
                    if (f == getStartFielding()) {
                        time -= get(JAM_CLOCK_START);
                    }
                }
            }
            value = time;
        }
        if (prop == CURRENT_FIELDING && source == Source.WS) {
            Fielding f = (Fielding) value;
            Fielding newStart = f;
            Fielding old = (Fielding) last;
            if (old != null && f != null && old.getPosition() != f.getPosition()) {
                newStart = get(START_FIELDING)
                        .getTeamJam()
                        .getJam()
                        .getTeamJam(f.getTeamJam().getTeam().getProviderId())
                        .getFielding(f.get(Fielding.POSITION).getFloorPosition());
                Clock clock = storedClock != null ? storedClock : getClock();
                if (initialTimeAdjusted && clock != null) {
                    clock.changeMaximumTime(game.getLong(Rule.PENALTY_DURATION) - extraTimeAdded);
                }
                old.getSkater().set(Skater.EXTRA_PENALTY_TIME, extraTimeAdded);
                initialTimeAdjusted = false;
                extraTimeAdded = 0L;
                if (penaltyAdded != null && "?".equals(penaltyAdded.getCode())) {
                    penaltyAdded.set(Penalty.CODE, null);
                }
                penaltyAdded = null;
                removeAll(PENALTY);
                removeAll(FIELDING);
            } else if (last == null && getClock() != null &&
                    ((!game.isInJam() && getClock().getTimeElapsed() > 0L) ||
                            (getClock().getTimeElapsed() > game.getClock(Clock.ID_JAM).getTimeElapsed()))) {
                newStart = f.getPrevious();
            }
            for (Fielding cur = newStart; cur != f.getNext(); cur = cur.getNext()) {
                add(FIELDING, cur);
            }
            return last;
        }
        if (prop == PENALTY_CODES) {
            List<Penalty> penalties = new ArrayList<>(getAll(PENALTY));
            Collections.sort(penalties);
            value = penalties.stream()
                    .filter(p -> !p.getProviderId().equals(Skater.FO_EXP_ID))
                    .map(Penalty::getCode)
                    .collect(Collectors.joining(" "));
        }
        if (prop == PENALTY_DETAILS) {
            List<Penalty> penalties = new ArrayList<>(getAll(PENALTY));
            Collections.sort(penalties);
            value = penalties.stream()
                    .filter(p -> !p.getProviderId().equals(Skater.FO_EXP_ID))
                    .map(Penalty::getDetails)
                    .collect(Collectors.joining(","));
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == IS_CURRENT) {
            if (!(Boolean) value) {
                if (getEndFielding() == null) {
                    end();
                } else if (!game.isInJam() && getEndFielding().getTeamJam().isRunningOrUpcoming()) {
                    remove(FIELDING, getEndFielding());
                }
                storedClock = getClock();
                remove(CLOCK, "");
            }
            if ((Boolean) value && getEndFielding() != null) {
                unend();
            }
            for (Fielding f : getAll(FIELDING)) {
                f.set(Fielding.CURRENT_BOX_TRIP, this);
                f.set(Fielding.PENALTY_BOX, (Boolean) value);
            }
        }
        if ((prop == END_FIELDING || prop == END_AFTER_S_P || prop == END_BETWEEN_JAMS) && getEndFielding() != null) {
            getEndFielding().updateBoxTripSymbols();
        }
        if ((prop == START_FIELDING || prop == START_AFTER_S_P || prop == START_BETWEEN_JAMS) &&
                getStartFielding() != null) {
            getStartFielding().updateBoxTripSymbols();
        }
        if (prop == CURRENT_FIELDING && value != null) {
            Fielding f = (Fielding) value;
            if (last == null || ((Fielding) last).getTeamJam().getProviderId() != f.getTeamJam().getProviderId() &&
                    !f.getTeamJam().isStarPass()) {
                set(START_AFTER_S_P, false);
            }
            f.set(Fielding.CURRENT_BOX_TRIP, this);
            f.set(Fielding.PENALTY_BOX, get(IS_CURRENT));
        }
        if (prop == TIMING_STOPPED && getClock() != null) {
            if ((Boolean) value) {
                getClock().stop();
            } else if (game.isInJam()) {
                getClock().start();
            }
        }
        if (prop == TIME && getClock() != null) {
            if (getClock().isTimeAtEnd() && getEndFielding() == null && (game.isInJam() || get(SHORTENED) == 0)) {
                end();
            } else if (!getClock().isTimeAtEnd() && getEndFielding() != null) {
                unend();
            }
        }
    }

    @Override
    public ScoreBoardEventProvider create(Child<? extends ScoreBoardEventProvider> prop, String id, Source source) {
        synchronized (coreLock) {
            if (prop == CLOCK && source.isFile()) {
                return new ClockImpl(this);
            }
            return null;
        }
    }

    @Override
    protected void itemAdded(Child<?> prop, ValueWithId item, Source source) {
        if (prop == FIELDING) {
            Fielding f = (Fielding) item;
            if (parent != f.getTeamJam().getTeam()) {
                parent.remove(Team.BOX_TRIP, this);
                parent = f.getTeamJam().getTeam();
                parent.add(Team.BOX_TRIP, this);
            }
            if (getStartFielding() == null || f.compareTo(getStartFielding()) < 0) {
                set(START_FIELDING, f);
            }
            if (getCurrentFielding() == null || f.compareTo(getCurrentFielding()) > 0) {
                set(CURRENT_FIELDING, f);
            }
            if (getEndFielding() != null && f.compareTo(getEndFielding()) > 0) {
                set(END_FIELDING, f);
            }
            if (getAll(FIELDING).size() == 1) {
                f.updateBoxTripSymbols();
                Skater s = f.getSkater();
                if (s != null && !source.isFile()) {
                    if (getClock() != null && !s.hasUnservedPenalties()) {
                        penaltyAdded = new PenaltyImpl(
                                s, s.numberOf(Skater.PENALTY) == 0 ? 1 : s.getMaxNumber(Skater.PENALTY) + 1);
                        s.add(Skater.PENALTY, penaltyAdded);
                    }
                    for (Penalty p : s.getUnservedPenalties()) {
                        add(PENALTY, p);
                    }
                    if (getClock() != null) {
                        if (initialTimeAdjusted) {
                            getClock().changeMaximumTime(s.getExtraPenaltyTime());
                        } else {
                            getClock().changeMaximumTime(s.getExtraPenaltyTime() - game.getLong(Rule.PENALTY_DURATION));
                            initialTimeAdjusted = true;
                        }
                        extraTimeAdded = s.getExtraPenaltyTime();
                        s.set(Skater.EXTRA_PENALTY_TIME, 0L);
                    }
                }
            }
        }
        if (prop == PENALTY) {
            Clock clock = storedClock != null ? storedClock : getClock();
            if (clock != null && !((Penalty) item).getProviderId().equals(Skater.FO_EXP_ID)) {
                if (initialTimeAdjusted && !source.isFile()) {
                    clock.changeMaximumTime(game.getLong(Rule.PENALTY_DURATION));
                } else {
                    initialTimeAdjusted = true;
                }
                if (!source.isFile() && getCurrentFielding().getCurrentRole() == Role.JAMMER &&
                        numberOf(PENALTY) > get(SHORTENED)) {
                    Position otherPos = getTeam().getOtherTeam().getPosition(
                            getTeam().getOtherTeam().isStarPass() ? FloorPosition.PIVOT : FloorPosition.JAMMER);
                    if (otherPos.isPenaltyBox()) {
                        // Both jammers in box
                        BoxTrip other = otherPos.getCurrentFielding().getCurrentBoxTrip();
                        if (other.numberOf(PENALTY) > other.get(SHORTENED) && other.getClock().getTimeRemaining() > 0) {
                            // other Jammer has a penalty that can be shortened
                            long shorteningAmount =
                                    Math.min(Math.min(other.getClock().getTimeRemaining(), clock.getTimeRemaining()),
                                            game.getLong(Rule.PENALTY_DURATION));
                            other.set(SHORTENED,
                                    Math.max(other.get(SHORTENED) + 1,
                                            other.numberOf(PENALTY) - (int) (other.getClock().getTimeRemaining() /
                                                    game.getLong(Rule.PENALTY_DURATION))));
                            set(SHORTENED, numberOf(PENALTY));
                            other.getClock().changeMaximumTime(-shorteningAmount);
                            clock.changeMaximumTime(-shorteningAmount);
                        }
                    }
                }
            }
        }
        if (prop == CLOCK && storedClock == null) {
            setCopy(TIME, getClock(), Clock.TIME, true);
        }
    }

    @Override
    protected void itemRemoved(Child<?> prop, ValueWithId item, Source source) {
        if (prop == FIELDING) {
            if (getStartFielding() == item) {
                Fielding first = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (first == null || first.compareTo(f) > 0) {
                        first = f;
                    }
                }
                set(START_FIELDING, first);
            }
            if (getCurrentFielding() == item) {
                Fielding last = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (last == null || last.compareTo(f) < 0) {
                        last = f;
                    }
                }
                set(CURRENT_FIELDING, last);
            }
            if (getEndFielding() == item) {
                Fielding last = null;
                for (Fielding f : getAll(FIELDING)) {
                    if (last == null || last.compareTo(f) < 0) {
                        last = f;
                    }
                }
                set(END_FIELDING, last);
            }
        }
        if (prop == PENALTY) {
            Clock clock = storedClock != null ? storedClock : getClock();
            if (clock != null && !((Penalty) item).getProviderId().equals(Skater.FO_EXP_ID)) {
                clock.changeMaximumTime(-game.getLong(Rule.PENALTY_DURATION));
            }
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == DELETE) {
            delete(source);
        } else if (prop == START_EARLIER) {
            if (getStartFielding() == null) {
                return;
            }
            if (startedAfterSP()) {
                set(START_AFTER_S_P, false);
            } else if (!startedBetweenJams()) {
                set(START_BETWEEN_JAMS, true);
            } else if (add(FIELDING,
                    getStartFielding().getSkater().getFielding(getStartFielding().getTeamJam().getPrevious()))) {
                set(START_BETWEEN_JAMS, false);
                if (getStartFielding().getTeamJam().isStarPass()) {
                    set(START_AFTER_S_P, true);
                }
            }
        } else if (prop == START_LATER) {
            if (getStartFielding() == null) {
                return;
            }
            if (getStartFielding().getTeamJam().isRunningOrUpcoming() && !game.isInJam()) {
                return;
            }
            if (startedBetweenJams()) {
                set(START_BETWEEN_JAMS, false);
            } else if (getStartFielding() == getEndFielding()) {
                if (endedAfterSP() && !startedAfterSP()) {
                    set(START_AFTER_S_P, true);
                }
            } else if (!startedAfterSP() && getStartFielding().getTeamJam().isStarPass()) {
                set(START_AFTER_S_P, true);
            } else if (getStartFielding() != getCurrentFielding()) {
                remove(FIELDING, getStartFielding());
                set(START_AFTER_S_P, false);
                set(START_BETWEEN_JAMS, true);
            }
        } else if (prop == END_EARLIER) {
            if (isCurrent()) {
                set(IS_CURRENT, false);
            } else if (endedBetweenJams()) {
                set(END_BETWEEN_JAMS, false);
            } else if (getStartFielding() == getEndFielding()) {
                if (endedAfterSP() && !startedAfterSP()) {
                    set(END_AFTER_S_P, false);
                }
            } else if (endedAfterSP()) {
                set(END_AFTER_S_P, false);
            } else {
                remove(FIELDING, getEndFielding());
                set(END_BETWEEN_JAMS, true);
                if (getEndFielding().getTeamJam().isStarPass()) {
                    set(END_AFTER_S_P, true);
                }
            }
        } else if (prop == END_LATER) {
            if (getEndFielding() == null) {
                return;
            }
            if (!endedAfterSP() && getEndFielding().getTeamJam().isStarPass()) {
                set(END_AFTER_S_P, true);
            } else if (!endedBetweenJams()) {
                set(END_BETWEEN_JAMS, true);
            } else {
                if (getEndFielding().getTeamJam().isRunningOrEnded() && !game.isInJam()) {
                    // user is attempting to extend trip into upcoming jam - field skater if necessary
                    getTeam().field(getEndFielding().getSkater(), getEndFielding().getCurrentRole(),
                            getEndFielding().getTeamJam().getNext());
                }
                if (add(FIELDING, getEndFielding().getSkater().getFielding(getEndFielding().getTeamJam().getNext()))) {
                    set(END_AFTER_S_P, false);
                    set(END_BETWEEN_JAMS, false);
                }
            }
            if (getEndFielding().getTeamJam().isRunningOrUpcoming() && (endedBetweenJams() || !game.isInJam())) {
                // moved end past the present point in the game -> make ongoing
                unend();
            }
        }
    }

    @Override
    public void end() {
        set(WALLTIME_END, ScoreBoardClock.getInstance().getCurrentWalltime());
        Skater s = get(CURRENT_SKATER);
        if (!game.isInJam() && getCurrentFielding().getTeamJam().isRunningOrUpcoming()) {
            if (getTeam().hasFieldingAdvancePending()) {
                getCurrentFielding().setSkater(null);
            }
            remove(FIELDING, getCurrentFielding());
        }
        if (getCurrentFielding() == null) {
            // trip ended in the same interjam as it started -> ignore it
            if (s != null) {
                s.set(Skater.EXTRA_PENALTY_TIME, extraTimeAdded);
            }
            delete();
        } else {
            set(END_FIELDING, get(CURRENT_FIELDING));
            set(END_BETWEEN_JAMS, !game.isInJam() && !getTeam().hasFieldingAdvancePending() &&
                    getEndFielding().getTeamJam().isRunningOrEnded());
            set(END_AFTER_S_P, getEndFielding().getTeamJam().isStarPass() && getEndFielding().isCurrent());
            set(JAM_CLOCK_END, endedBetweenJams() ? 0L : game.getClock(Clock.ID_JAM).getTimeElapsed());
            getCurrentFielding().updateBoxTripSymbols();
        }
        if (getClock() != null && getEndFielding() != null && getEndFielding().getSkater() != null) {
            getEndFielding().getSkater().set(Skater.EXTRA_PENALTY_TIME, getClock().getTimeRemaining());
        }
    }

    @Override
    public void unend() {
        set(WALLTIME_END, 0L);
        set(END_FIELDING, null);
        set(END_BETWEEN_JAMS, false);
        set(END_AFTER_S_P, false);
        set(IS_CURRENT, true);
        getCurrentFielding().updateBoxTripSymbols();
        if (storedClock != null) {
            add(CLOCK, storedClock);
            storedClock = null;
            getCurrentFielding().getSkater().set(Skater.EXTRA_PENALTY_TIME, 0L);
        }
    }

    @Override
    public void startJam() {
        if (getClock() != null && !get(TIMING_STOPPED)) {
            getClock().start();
            if (getClock().isTimeAtEnd() && getEndFielding() == null) {
                end();
            }
        }
        if (storedClock != null) {
            storedClock.delete();
            storedClock = null;
        }
    }

    @Override
    public void stopJam() {
        if (getClock() != null) {
            getClock().stop();
        }
        if (storedClock != null) {
            storedClock.stop();
        }
    }

    @Override
    public Team getTeam() {
        return (Team) getParent();
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public Clock getClock() {
        return get(CLOCK, "");
    }

    @Override
    public boolean isCurrent() {
        return get(IS_CURRENT);
    }

    @Override
    public Fielding getCurrentFielding() {
        return get(CURRENT_FIELDING);
    }

    @Override
    public Fielding getStartFielding() {
        return get(START_FIELDING);
    }

    @Override
    public boolean startedBetweenJams() {
        return get(START_BETWEEN_JAMS);
    }

    @Override
    public boolean startedAfterSP() {
        return get(START_AFTER_S_P);
    }

    @Override
    public Fielding getEndFielding() {
        return get(END_FIELDING);
    }

    @Override
    public boolean endedBetweenJams() {
        return get(END_BETWEEN_JAMS);
    }

    @Override
    public boolean endedAfterSP() {
        return get(END_AFTER_S_P);
    }

    @Override
    public Clock.ClockSnapshot snapshot() {
        if (getClock() != null) {
            return getClock().snapshot();
        } else return null;
    }

    @Override
    public void restoreSnapshot(Clock.ClockSnapshot s) {
        if (getClock() != null) {
            if (s != null) {
                getClock().restoreSnapshot(s);
            } else {
                // Box Trip was started during reverted time
                getClock().resetTime();
                getClock().set(Clock.RUNNING, game.isInJam());
            }
        }
    }

    private final Game game;
    private Clock storedClock = null;
    private boolean initialTimeAdjusted = false;
    private Penalty penaltyAdded = null;
    private long extraTimeAdded = 0L;
}
