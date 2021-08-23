package eu.h2020.helios_social.modules.groupcommunications.context.proxy;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class LocationContextProxy extends LocationContext implements AbstractMessage {
    private Integer color;
    private String privateName;
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
}
