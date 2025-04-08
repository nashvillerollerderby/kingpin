package com.nashvillerollerderby.scoreboard.core.game;

import com.nashvillerollerderby.scoreboard.core.interfaces.Game;
import com.nashvillerollerderby.scoreboard.core.interfaces.Official;
import com.nashvillerollerderby.scoreboard.core.interfaces.PreparedOfficial;
import com.nashvillerollerderby.scoreboard.core.interfaces.ScoreBoard;
import com.nashvillerollerderby.scoreboard.core.interfaces.Team;
import com.nashvillerollerderby.scoreboard.core.prepared.PreparedOfficialImpl;
import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Command;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProviderImpl;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OfficialImpl extends ScoreBoardEventProviderImpl<Official> implements Official {
    public OfficialImpl(Game g, String id, Child<Official> type) {
        super(g, id, type);
        game = g;
        addProperties(props);
        addProperties(preparedProps);
    }

    public OfficialImpl(Game g, Official source) {
        this(g, UUID.randomUUID().toString(), source.getType());
        set(ROLE, source.get(ROLE));
        set(NAME, source.get(NAME));
        set(LEAGUE, source.get(LEAGUE));
        set(CERT, source.get(CERT));
        if (source.get(P1_TEAM) != null) {
            set(P1_TEAM, g.getTeam(source.get(P1_TEAM).getProviderId()));
        }
        set(SWAP, source.get(SWAP));
    }

    @Override
    public int compareTo(Official other) {
        return roleIndex() - ((OfficialImpl) other).roleIndex();
    }

    @Override
    protected Object computeValue(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == ROLE && value != null) {
            String mapped = roleMap.get((String) value);
            return mapped != null ? mapped : value;
        }
        return value;
    }

    @Override
    protected void valueChanged(Value<?> prop, Object value, Object last, Source source, Flag flag) {
        if (prop == ROLE && value != null) {
            String role = (String) value;
            if (ROLE_HR.equals(role)) {
                game.set(Game.HEAD_REF, this);
            }
            if (ROLE_PLT.equals(role) || ROLE_LT.equals(role) || ROLE_SK.equals(role) || ROLE_JR.equals(role)) {
                for (Official other : game.getAll(ownType)) {
                    if (other != this && role.equals(other.get(ROLE))) {
                        set(SWAP, other.get(SWAP));
                        Team ot = other.get(P1_TEAM);
                        if (ot != null) {
                            set(P1_TEAM, ot.getOtherTeam());
                        }
                        return;
                    }
                }
                // first Official with this position
                set(SWAP, ROLE_SK.equals(role) || ROLE_JR.equals(role));
            }
        }
        if (prop == SWAP && get(ROLE) != null) {
            for (Official other : game.getAll(ownType)) {
                if (other != this && get(ROLE).equals(other.get(ROLE))) {
                    other.set(SWAP, (Boolean) value);
                }
            }
        }
        if (prop == P1_TEAM && value != null && !"".equals(get(ROLE))) {
            Team t = (Team) value;
            Official partner = null;
            for (Official other : game.getAll(ownType)) {
                if (other != this && get(ROLE).equals(other.get(ROLE))) {
                    if (partner == null) {
                        partner = other;
                    } else {
                        // three or more officials with this role - don't change others
                        return;
                    }
                }
            }
            if (partner != null) {
                partner.set(P1_TEAM, t.getOtherTeam());
            }
        }
        if (prop == NAME && get(PREPARED_OFFICIAL) == null && !"".equals(value) &&
                ("".equals(get(CERT)) || "".equals(get(LEAGUE)))) {
            for (PreparedOfficial p : scoreBoard.getAll(ScoreBoard.PREPARED_OFFICIAL)) {
                if (p.matches((String) value, get(LEAGUE))) {
                    set(PREPARED_OFFICIAL, p);
                    return;
                }
            }
        }
        if (prop == PREPARED_OFFICIAL && value != null) {
            PreparedOfficial po = (PreparedOfficial) value;
            setCopy(NAME, po, NAME, false);
            setCopy(LEAGUE, po, LEAGUE, false);
            setCopy(CERT, po, CERT, false);
        }
    }

    @Override
    public void execute(Command prop, Source source) {
        if (prop == STORE) {
            PreparedOfficial po = new PreparedOfficialImpl(scoreBoard, UUID.randomUUID().toString());
            po.set(NAME, get(NAME));
            po.set(LEAGUE, get(LEAGUE));
            po.set(CERT, get(CERT));
            scoreBoard.add(ScoreBoard.PREPARED_OFFICIAL, po);
            set(PREPARED_OFFICIAL, po);
        }
    }

    private int roleIndex() {
        switch (get(ROLE)) {
            case ROLE_PLT:
                return 0;
            case ROLE_PT:
                return 1;
            case ROLE_PW:
                return 2;
            case ROLE_WB:
                return 3;
            case ROLE_JT:
                return 4;
            case ROLE_SK:
                return 5;
            case ROLE_SBO:
                return 6;
            case ROLE_PBM:
                return 7;
            case ROLE_PBT:
                return 8;
            case ROLE_LT:
                return 9;
            case ROLE_ALTN:
                return 10;

            case ROLE_HR:
                return 1;
            case ROLE_IPR:
                return 2;
            case ROLE_JR:
                return 3;
            case ROLE_OPR:
                return 4;
            case ROLE_ALTR:
                return 5;

            default:
                return 15;
        }
    }

    @Override
    public Child<Official> getType() {
        return ownType;
    }

    private static String normalize(String input) {
        return input.replaceAll("\\W", "").toLowerCase(Locale.ROOT);
    }

    private final Game game;

    public static Map<String, String> roleMap =
            Stream
                    .of(new String[][]{{normalize(ROLE_HNSO), ROLE_HNSO},
                            {normalize(ROLE_PLT), ROLE_PLT},
                            {normalize(ROLE_PT), ROLE_PT},
                            {normalize(ROLE_PW), ROLE_PW},
                            {normalize(ROLE_WB), ROLE_WB},
                            {normalize(ROLE_JT), ROLE_JT},
                            {normalize(ROLE_SK), ROLE_SK},
                            {normalize(ROLE_SBO), ROLE_SBO},
                            {normalize(ROLE_PBM), ROLE_PBM},
                            {normalize(ROLE_PBT), ROLE_PBT},
                            {normalize(ROLE_LT), ROLE_LT},
                            {normalize(ROLE_ALTN), ROLE_ALTN},
                            {normalize(ROLE_HR), ROLE_HR},
                            {normalize(ROLE_IPR), ROLE_IPR},
                            {normalize(ROLE_JR), ROLE_JR},
                            {normalize(ROLE_OPR), ROLE_OPR},
                            {normalize(ROLE_ALTR), ROLE_ALTR},
                            {"hnso", ROLE_HNSO},
                            {"plt", ROLE_PLT},
                            {"pt", ROLE_PT},
                            {"pw", ROLE_PW},
                            {"iwb", ROLE_WB},
                            {"jt", ROLE_JT},
                            {"sk", ROLE_SK},
                            {"sbo", ROLE_SBO},
                            {"pbm", ROLE_PBM},
                            {"pbt", ROLE_PBT},
                            {"lt", ROLE_LT},
                            {"altn", ROLE_ALTN},
                            {"hr", ROLE_HR},
                            {"ipr", ROLE_IPR},
                            {"jr", ROLE_JR},
                            {"opr", ROLE_OPR},
                            {"altr", ROLE_ALTR}})
                    .collect(Collectors.collectingAndThen(Collectors.toMap(data -> data[0], data -> data[1]),
                            Collections::<String, String>unmodifiableMap));
}
