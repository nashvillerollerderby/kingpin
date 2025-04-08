package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.MirrorScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface CurrentGame extends MirrorScoreBoardEventProvider<Game> {
    void postAutosaveUpdate();

    void load(Game g);

    Collection<Property<?>> props = new ArrayList<>();

    Value<Game> GAME = new Value<>(Game.class, "Game", null, props);

    interface CurrentClock extends MirrorScoreBoardEventProvider<Clock> {
    }

    interface CurrentTeam extends MirrorScoreBoardEventProvider<Team> {
    }

    interface CurrentSkater extends MirrorScoreBoardEventProvider<Skater> {
    }

    interface CurrentPenalty extends MirrorScoreBoardEventProvider<Penalty> {
    }

    interface CurrentPosition extends MirrorScoreBoardEventProvider<Position> {
    }

    interface CurrentBoxTrip extends MirrorScoreBoardEventProvider<BoxTrip> {
    }

    interface CurrentPeriod extends MirrorScoreBoardEventProvider<Period> {
    }

    interface CurrentJam extends MirrorScoreBoardEventProvider<Jam> {
    }

    interface CurrentTeamJam extends MirrorScoreBoardEventProvider<TeamJam> {
    }

    interface CurrentFielding extends MirrorScoreBoardEventProvider<Fielding> {
    }

    interface CurrentScoringTrip extends MirrorScoreBoardEventProvider<ScoringTrip> {
    }

    interface CurrentTimeout extends MirrorScoreBoardEventProvider<Timeout> {
    }

    interface CurrentOfficial extends MirrorScoreBoardEventProvider<Official> {
    }

    interface CurrentExpulsion extends MirrorScoreBoardEventProvider<Expulsion> {
    }
}
