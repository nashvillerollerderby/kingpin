package com.nashvillerollerderby.scoreboard.event;

public interface NumberedScoreBoardEventProvider<C extends NumberedScoreBoardEventProvider<C>>
        extends OrderedScoreBoardEventProvider<C> {
    int compareTo(NumberedScoreBoardEventProvider<?> other);

    void moveToNumber(int num);
}
