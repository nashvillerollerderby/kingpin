package com.nashvillerollerderby.scoreboard.event;

public interface OrderedScoreBoardEventProvider<C extends OrderedScoreBoardEventProvider<C>>
        extends ScoreBoardEventProvider {
    int getNumber();

    C getPrevious();

    boolean hasPrevious();

    void setPrevious(C prev);

    C getNext();

    boolean hasNext();

    void setNext(C next);

    Value<Integer> NUMBER = new Value<>(Integer.class, "Number", 0, null);
}
