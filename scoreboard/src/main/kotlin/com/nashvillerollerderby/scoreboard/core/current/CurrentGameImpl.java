package com.nashvillerollerderby.scoreboard.core.current;

import com.nashvillerollerderby.scoreboard.core.game.GameImpl;
import com.nashvillerollerderby.scoreboard.core.interfaces.BoxTrip;
import com.nashvillerollerderby.scoreboard.core.interfaces.Clock;
import com.nashvillerollerderby.scoreboard.core.interfaces.CurrentGame;
import com.nashvillerollerderby.scoreboard.core.interfaces.Expulsion;
import com.nashvillerollerderby.scoreboard.core.interfaces.Fielding;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Jam;
import com.nashvillerollerderby.scoreboard.core.interfaces.Official;
import com.nashvillerollerderby.scoreboard.core.interfaces.Penalty;
import com.nashvillerollerderby.scoreboard.core.interfaces.Period;
import com.nashvillerollerderby.scoreboard.core.interfaces.Position;
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedTeam;
import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets;
import com.nashvillerollerderby.scoreboard.core.interfaces.Rulesets.Ruleset;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoringTrip;
import com.nashvillerollerderby.scoreboard.core.interfaces.Skater;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.core.interfaces.TeamJam;
import com.nashvillerollerderby.scoreboard.core.interfaces.Timeout;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.ConditionalScoreBoardListener;
import com.nashvillerollerderby.scoreboard.event.IndirectScoreBoardListener;
import com.nashvillerollerderby.scoreboard.event.MirrorScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.MirrorScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEvent;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardListener;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.event.ValueWithId;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class CurrentGameImpl extends MirrorScoreBoardEventProviderImpl<Game, CurrentGame> implements CurrentGame {
    @SuppressWarnings("unchecked")
    public CurrentGameImpl(ScoreBoard sb) {
        super(sb, "", ScoreBoard.CURRENT_GAME);
        addProperties(props);
        fillMaps();
        mirrorFactory = new MirrorFactoryImpl();
        for (Property<?> prop : Game.props) {
            if (reversePropertyMap.containsKey(prop)) {
                addMirrorCopy((Child<? extends ScoreBoardEventProvider>) prop);
            } else {
                addProperties(prop);
                if (prop instanceof Value<?>) {
                    setCopy((Value<?>) prop);
                }
                if (prop instanceof Child<?>) {
                    setCopy((Child<?>) prop);
                }
            }
        }
    }

    @Override
    public String getProviderId() {
        return "";
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == GAME && (!source.isFile() || value != null)) {
            if (last != null) {
                elements.get(CurrentGame.class).remove(((Game) last).getId());
            }
            if (value == null && last != null) {
                // having no current game will brake lots of things, so inhibit that
                value = new GameImpl(scoreBoard, UUID.randomUUID().toString());
                scoreBoard.add(ScoreBoard.GAME, (Game) value);
            }
            sourceElement = (Game) value;
            if (value != null) {
                elements.get(CurrentGame.class).put(((Game) value).getId(), this);
            }
        } else if (source.isFile()) {
            return last;
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == GAME && value != null) {
            Game g = (Game) value;
            if (g.get(Game.EVENT_INFO, Game.INFO_START_TIME) != null &&
                    !"".equals(g.get(Game.EVENT_INFO, Game.INFO_START_TIME).getValue())) {
                try {
                    LocalTime time = LocalTime.parse(g.get(Game.EVENT_INFO, Game.INFO_START_TIME).getValue());
                    LocalDate date = "".equals(g.get(Game.EVENT_INFO, Game.INFO_DATE).getValue())
                            ? LocalDate.now()
                            : LocalDate.parse(g.get(Game.EVENT_INFO, Game.INFO_DATE).getValue());
                    long timeToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), LocalDateTime.of(date, time));
                    if (timeToStart > 0) {
                        Clock ic = g.getClock(Clock.ID_INTERMISSION);
                        ic.setMaximumTime(timeToStart);
                        ic.resetTime();
                        ic.start();
                    }
                } catch (Exception e) {
                } // if parsing fails just set no time to derby
            }
        }
    }

    @Override
    protected <T> void setCopy(Value<T> prop) {
        setCopy(prop, this, GAME, prop, false);
    }

    @Override
    protected <T extends ValueWithId> void setCopy(Child<T> prop) {
        setCopy(prop, this, GAME, prop, false);
    }

    @Override
    protected <T extends ScoreBoardEventProvider> void addMirrorCopy(final Child<T> sourceProperty) {
        @SuppressWarnings("unchecked")
        Child<MirrorScoreBoardEventProvider<T>> targetProperty =
                (Child<MirrorScoreBoardEventProvider<T>>) reversePropertyMap.get(sourceProperty);
        propertyMap.put(targetProperty, sourceProperty);
        reversePropertyMap.put(sourceProperty, targetProperty);
        addProperties(targetProperty);
        ScoreBoardListener l = new IndirectScoreBoardListener<>(
                this, GAME, sourceProperty, new ChildToMirrorScoreBoardListener<>(this, targetProperty));
        providers.put(l, null);
        ScoreBoardListener reverseListener = new ConditionalScoreBoardListener<>(this, GAME, new ScoreBoardListener() {
            @Override
            public void scoreBoardChange(ScoreBoardEvent<?> event) {
                reverseCopyListeners.put(
                        targetProperty, new ChildFromMirrorScoreBoardListener<>((ScoreBoardEventProvider) event.getValue(),
                                sourceProperty));
            }
        });
        addScoreBoardListener(reverseListener);
        reverseListener.scoreBoardChange(new ScoreBoardEvent<>(this, GAME, get(GAME), null));
    }

    @Override
    protected void fillMaps() {
        addClassMapping(Game.class, CurrentGame.class);
        addClassMapping(Clock.class, CurrentClock.class);
        addClassMapping(Team.class, CurrentTeam.class);
        addClassMapping(Skater.class, CurrentSkater.class);
        addClassMapping(Penalty.class, CurrentPenalty.class);
        addClassMapping(Position.class, CurrentPosition.class);
        addClassMapping(BoxTrip.class, CurrentBoxTrip.class);
        addClassMapping(Period.class, CurrentPeriod.class);
        addClassMapping(Jam.class, CurrentJam.class);
        addClassMapping(TeamJam.class, CurrentTeamJam.class);
        addClassMapping(Fielding.class, CurrentFielding.class);
        addClassMapping(ScoringTrip.class, CurrentScoringTrip.class);
        addClassMapping(Timeout.class, CurrentTimeout.class);
        addClassMapping(Official.class, CurrentOfficial.class);
        addClassMapping(Expulsion.class, CurrentExpulsion.class);

        addPropertyMapping(Game.CLOCK, Game.TEAM, Game.PERIOD, Period.JAM, Team.BOX_TRIP, Game.REF, Game.NSO,
                Game.EXPULSION);
    }

    @Override
    public void postAutosaveUpdate() {
        synchronized (coreLock) {
            if (get(GAME) == null) {
                // autosave did not contain a current game - create one ad hoc
                PreparedTeam t1 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "Black");
                t1.set(Team.TEAM_NAME, "Black");
                PreparedTeam t2 = scoreBoard.getOrCreate(ScoreBoard.PREPARED_TEAM, "White");
                t2.set(Team.TEAM_NAME, "White");
                Ruleset rs = scoreBoard.getRulesets().getRuleset(Rulesets.ROOT_ID);
                Game g = new GameImpl(scoreBoard, t1, t2, rs);
                scoreBoard.add(ScoreBoard.GAME, g);
                load(g);
            }
        }
    }

    @Override
    public void load(Game g) {
        set(GAME, g);
    }

    public static class CurrentClockImpl
            extends MirrorScoreBoardEventProviderImpl<Clock, CurrentClock> implements CurrentClock {
        CurrentClockImpl(ScoreBoardEventProvider parent, Clock sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentTeamImpl
            extends MirrorScoreBoardEventProviderImpl<Team, CurrentTeam> implements CurrentTeam {
        CurrentTeamImpl(ScoreBoardEventProvider parent, Team sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(Team.SKATER, Team.POSITION, Team.BOX_TRIP);
        }
    }

    public static class CurrentSkaterImpl
            extends MirrorScoreBoardEventProviderImpl<Skater, CurrentSkater> implements CurrentSkater {
        CurrentSkaterImpl(ScoreBoardEventProvider parent, Skater sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(Skater.PENALTY);
        }
    }

    public static class CurrentPenaltyImpl
            extends MirrorScoreBoardEventProviderImpl<Penalty, CurrentPenalty> implements CurrentPenalty {
        CurrentPenaltyImpl(ScoreBoardEventProvider parent, Penalty sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentPositionImpl
            extends MirrorScoreBoardEventProviderImpl<Position, CurrentPosition> implements CurrentPosition {
        CurrentPositionImpl(ScoreBoardEventProvider parent, Position sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentBoxTripImpl
            extends MirrorScoreBoardEventProviderImpl<BoxTrip, CurrentBoxTrip> implements CurrentBoxTrip {
        CurrentBoxTripImpl(ScoreBoardEventProvider parent, BoxTrip sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(BoxTrip.CLOCK);
        }
    }

    public static class CurrentPeriodImpl
            extends MirrorScoreBoardEventProviderImpl<Period, CurrentPeriod> implements CurrentPeriod {
        CurrentPeriodImpl(ScoreBoardEventProvider parent, Period sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(Period.JAM, Period.TIMEOUT);
        }
    }

    public static class CurrentJamImpl
            extends MirrorScoreBoardEventProviderImpl<Jam, CurrentJam> implements CurrentJam {
        CurrentJamImpl(ScoreBoardEventProvider parent, Jam sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(Jam.TEAM_JAM);
        }
    }

    public static class CurrentTeamJamImpl
            extends MirrorScoreBoardEventProviderImpl<TeamJam, CurrentTeamJam> implements CurrentTeamJam {
        CurrentTeamJamImpl(ScoreBoardEventProvider parent, TeamJam sourceElement) {
            super(parent, sourceElement);
        }

        @Override
        protected void fillMaps() {
            addPropertyMapping(TeamJam.FIELDING, TeamJam.SCORING_TRIP);
        }
    }

    public static class CurrentFieldingImpl
            extends MirrorScoreBoardEventProviderImpl<Fielding, CurrentFielding> implements CurrentFielding {
        CurrentFieldingImpl(ScoreBoardEventProvider parent, Fielding sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentScoringTripImpl
            extends MirrorScoreBoardEventProviderImpl<ScoringTrip, CurrentScoringTrip> implements CurrentScoringTrip {
        CurrentScoringTripImpl(ScoreBoardEventProvider parent, ScoringTrip sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentTimeoutImpl
            extends MirrorScoreBoardEventProviderImpl<Timeout, CurrentTimeout> implements CurrentTimeout {
        CurrentTimeoutImpl(ScoreBoardEventProvider parent, Timeout sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentOfficialImpl
            extends MirrorScoreBoardEventProviderImpl<Official, CurrentOfficial> implements CurrentOfficial {
        CurrentOfficialImpl(ScoreBoardEventProvider parent, Official sourceElement) {
            super(parent, sourceElement);
        }
    }

    public static class CurrentExpulsionImpl
            extends MirrorScoreBoardEventProviderImpl<Expulsion, CurrentExpulsion> implements CurrentExpulsion {
        CurrentExpulsionImpl(ScoreBoardEventProvider parent, Expulsion sourceElement) {
            super(parent, sourceElement);
        }
    }
}
