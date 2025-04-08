package com.nashvillerollerderby.scoreboard.event;

import com.nashvillerollerderby.scoreboard.viewer.FormatSpecifierViewer;

public class FormatSpecifierScoreBoardListener<T>
        extends ConditionalScoreBoardListener<T> implements ScoreBoardListener {
    @SuppressWarnings("unchecked")
    public FormatSpecifierScoreBoardListener(FormatSpecifierViewer v, String f, ScoreBoardListener l)
            throws IllegalArgumentException {
        super((ScoreBoardCondition<T>) v.getScoreBoardCondition(f), l);
        formatSpecifierViewer = v;
        format = f;
    }

    public String getFormat() {
        return format;
    }

    @Override
    protected boolean checkScoreBoardEvent(ScoreBoardEvent<?> e) {
        return (super.checkScoreBoardEvent(e) && formatSpecifierViewer.checkCondition(getFormat(), e));
    }

    protected FormatSpecifierViewer formatSpecifierViewer;
    protected String format;
}
