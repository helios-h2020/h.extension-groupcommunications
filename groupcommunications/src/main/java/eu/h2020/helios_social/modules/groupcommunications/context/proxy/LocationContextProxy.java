package eu.h2020.helios_social.modules.groupcommunications.context.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ContextListener;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class LocationContextProxy extends LocationContext implements AbstractMessage, ContextListener {
    private Integer color;
    private String privateName;
    private long contextStateChangeTimestamp = 0;
    public LocationContextProxy(@NotNull String id, String name, @NotNull Integer color,
                                double lat, double lon, double radius, @NotNull String privateName) {
        super(id, name, lat, lon, radius);
        this.color = color;
        this.privateName = privateName;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public String getPrivateName() {
        return privateName;
    }

    public void setPrivateName(@NotNull String privateName) {
        this.privateName = privateName;
    }

    public void setPublicName (@NotNull String name){
        super.setName(name);
    }

    @Override
    public void contextChanged(boolean active) {
        contextStateChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void setActive(boolean active) {
        if (active != this.isActive()) {
            contextStateChangeTimestamp = System.currentTimeMillis();
            super.setActive(active);
        }

    }

    public long getContextStateChangeTimestamp() {
        return contextStateChangeTimestamp;
    }
}
