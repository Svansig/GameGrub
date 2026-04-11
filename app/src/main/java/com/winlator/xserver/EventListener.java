package com.winlator.xserver;

import com.winlator.xserver.events.Event;

import java.io.IOException;

/**
 * EventListener - X11 event listener for a client-window pair.
 * 
 * Record holding:
 * - client: The interested XClient
 * - eventMask: Events the client wants for this window
 * 
 * Filters and delivers events to interested clients.
 */
public record EventListener(XClient client, Bitmask eventMask) {

    public boolean isInterestedIn(int eventId) {
        return eventMask.isSet(eventId);
    }

    public boolean isInterestedIn(Bitmask mask) {
        return this.eventMask.intersects(mask);
    }

    public void sendEvent(Event event) {
        try {
            event.send(client.getSequenceNumber(), client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        client.sendEvent(event);
    }
}
