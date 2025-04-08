package com.nashvillerollerderby.scoreboard.event;

import java.util.Collection;

public interface MirrorScoreBoardEventProvider<C extends ScoreBoardEventProvider> extends ScoreBoardEventProvider {
    C getSourceElement();

    <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T> getMirror(Child<T> prop, String id);

    <T extends ScoreBoardEventProvider> Collection<MirrorScoreBoardEventProvider<T>>
    getAllMirrors(Child<T> prop);
}