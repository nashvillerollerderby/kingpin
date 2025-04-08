package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Media extends ScoreBoardEventProvider {
    MediaFormat getFormat(String format);

    // Deletes a file off disk. True if successful.
    boolean removeMediaFile(String format, String type, String id);

    boolean validFileName(String fn);

    Collection<Property<?>> props = new ArrayList<>();

    Child<MediaFormat> FORMAT = new Child<>(MediaFormat.class, "Format", props);

    interface MediaFormat extends ScoreBoardEventProvider {
        String getFormat();

        MediaType getType(String type);

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Child<MediaType> TYPE = new Child<>(MediaType.class, "Type", props);
    }

    interface MediaType extends ScoreBoardEventProvider {
        String getFormat();

        String getType();

        MediaFile getFile(String id);

        void addFile(MediaFile file);

        void removeFile(MediaFile file);

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Child<MediaFile> FILE = new Child<>(MediaFile.class, "File", props);
    }

    interface MediaFile extends ScoreBoardEventProvider {
        String getFormat();

        String getType();

        @Override
        String getId();

        String getName();

        void setName(String s);

        String getSrc();

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Value<String> SRC = new Value<>(String.class, "Src", "", props);
        Value<String> NAME = new Value<>(String.class, "Name", "", props);
    }
}
