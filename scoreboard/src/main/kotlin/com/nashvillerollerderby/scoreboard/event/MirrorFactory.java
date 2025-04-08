package com.nashvillerollerderby.scoreboard.event;

public interface MirrorFactory {
    <T extends ScoreBoardEventProvider> MirrorScoreBoardEventProvider<T>
    createMirror(ScoreBoardEventProvider parent, T mirrored);
}
