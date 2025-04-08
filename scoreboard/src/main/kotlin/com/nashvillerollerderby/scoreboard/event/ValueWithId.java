package com.nashvillerollerderby.scoreboard.event;

public interface ValueWithId {
    /**
     * Id to be used in order to identify this element amongst all elements of its
     * type. Used when the element is referenced by elements other than its parent.
     * (Typically a UUID.)
     */
    String getId();

    /**
     * Value of the element. For implementations of ScoreBoardEventProvider this
     * should usually be the same as getId().
     */
    String getValue();
}
