package com.nashvillerollerderby.scoreboard.core.interfaces;

import com.nashvillerollerderby.scoreboard.event.Child;
import com.nashvillerollerderby.scoreboard.event.Property;
import com.nashvillerollerderby.scoreboard.event.ScoreBoardEventProvider;
import com.nashvillerollerderby.scoreboard.event.Value;

import java.util.ArrayList;
import java.util.Collection;

public interface Clients extends ScoreBoardEventProvider {

    Collection<Property<?>> props = new ArrayList<>();

    Value<Boolean> NEW_DEVICE_WRITE = new Value<>(Boolean.class, "NewDeviceWrite", true, props);
    Value<Boolean> ALL_LOCAL_DEVICES_WRITE =
            new Value<>(Boolean.class, "AllLocalDevicesWrite", true, props);

    Child<Device> DEVICE = new Child<>(Device.class, "Device", props);

    void postAutosaveUpdate();

    Device getDevice(String sessionId);

    Device getOrAddDevice(String sessionId);

    int gcOldDevices(long threshold);

    Client addClient(String deviceId, String remoteAddr, String source, String platform);

    void removeClient(Client c);

    // An active websocket client.
    interface Client extends ScoreBoardEventProvider {
        void write();

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "", props);
        Value<String> PLATFORM = new Value<>(String.class, "Platform", "", props);
        Value<String> SOURCE = new Value<>(String.class, "Source", "", props);
        Value<Long> CREATED = new Value<>(Long.class, "Created", 0L, props);
        Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L, props);
    }

    // A device is a HTTP cookie.
    interface Device extends ScoreBoardEventProvider {
        String getName();

        Boolean mayWrite();

        Boolean isLocal();

        void access();

        void write();

        @SuppressWarnings("hiding")
        Collection<Property<?>> props = new ArrayList<>();

        Value<String> SESSION_ID_SECRET =
                new Value<>(String.class, "SessionIdSecret", "", props);                           // The cookie.
        Value<String> NAME = new Value<>(String.class, "Name", "", props); // A human-readable name.
        Value<String> REMOTE_ADDR = new Value<>(String.class, "RemoteAddr", "", props);
        Value<String> PLATFORM = new Value<>(String.class, "Platform", "", props);
        Value<String> COMMENT = new Value<>(String.class, "Comment", "", props);
        Value<Long> CREATED = new Value<>(Long.class, "Created", 0L, props);
        Value<Long> WROTE = new Value<>(Long.class, "Wrote", 0L, props);
        Value<Long> ACCESSED = new Value<>(Long.class, "Accessed", 0L, props);
        Value<Boolean> MAY_WRITE = new Value<>(Boolean.class, "MayWrite", false, props);
        Value<Integer> NUM_CLIENTS = new Value<>(Integer.class, "NumClients", 0, props);

        Child<Client> CLIENT = new Child<>(Client.class, "Client", props);
    }
}
