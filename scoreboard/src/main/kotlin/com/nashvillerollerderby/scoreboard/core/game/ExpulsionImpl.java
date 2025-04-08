package com.nashvillerollerderby.scoreboard.core.game;

import com.nashvillerollerderby.scoreboard.core.interfaces.Expulsion;
import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Penalty;
import com.nashvillerollerderby.scoreboard.core.interfaces.Skater;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Value;
import com.nashvillerollerderby.scoreboard.penalties.PenaltyCode;

public class ExpulsionImpl extends ScoreBoardEventProviderImpl<Expulsion> implements Expulsion {
    ExpulsionImpl(Game g, Penalty p) {
        super(g, p.getId(), Game.EXPULSION);
        game = g;
        penalty = p;
        addProperties(props);
        setRecalculated(INFO)
                .addSource(p, Penalty.CODE)
                .addSource(p, Penalty.JAM_NUMBER)
                .addSource(p, Penalty.PERIOD_NUMBER)
                .addSource(p.getParent(), Skater.ROSTER_NUMBER)
                .addSource(p.getParent().getParent(), Team.DISPLAY_NAME);
        set(INFO, "");
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == INFO) {
            PenaltyCode code = game.get(Game.PENALTY_CODE, penalty.getCode());
            String penaltyName = code == null ? "Unknown Penalty" : code.verbalCues.get(0);
            value = penalty.getParent().getParent().get(Team.DISPLAY_NAME) + " #" +
                    penalty.getParent().get(Skater.ROSTER_NUMBER) + " Period " + penalty.getPeriodNumber() + " Jam " +
                    penalty.getJamNumber() + " for " + penaltyName + ".";
        }
        return value;
    }

    private final Game game;
    private final Penalty penalty;
}
