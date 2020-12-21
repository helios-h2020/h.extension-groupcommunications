package eu.h2020.helios_social.modules.groupcommunications.api.event;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class HeliosEvent implements AbstractMessage {

    public enum Type {
        PRIVATE, PUBLIC, SHARABLE
    }

    private String eventId;
    private String title;
    private String description;
    private String contextId;
    private double lat, lng;
    private String url;
    private Type eventType;

    public HeliosEvent(@NotNull String eventId, @NotNull String title) {
        this.eventId = eventId;
        this.title = title;
        this.eventType = Type.PUBLIC;
    }

    public HeliosEvent setDescription(String description) {
        this.description = description;
        return this;
    }

    public HeliosEvent setContextId(String contextId) {
        this.contextId = contextId;
        return this;
    }

    public HeliosEvent setEventType(Type eventType) {
        this.eventType = eventType;
        return this;
    }

    public HeliosEvent setLocation(double latitude, double longitude) {
        this.lat = latitude;
        this.lng = longitude;
        return this;
    }

    public HeliosEvent setURL(String url) {
        this.url = url;
        return this;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String ContextId() {
        return contextId;
    }

    public String getDescription() {
        return description;
    }

    public Type getEventType() {
        return eventType;
    }

    public double getLatitude() {
        return lat;
    }

    public double getLongitude() {
        return lng;
    }

    public String getUrl() {
        return url;
    }
}
