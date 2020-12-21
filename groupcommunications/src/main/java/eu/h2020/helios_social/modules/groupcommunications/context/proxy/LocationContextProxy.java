package eu.h2020.helios_social.modules.groupcommunications.context.proxy;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class LocationContextProxy extends LocationContext  implements AbstractMessage {
    private Integer color;

    public LocationContextProxy(@NotNull String id, @NotNull String name, @NotNull Integer color,
                                double lat, double lon, double radius) {
        super(id, name, lat, lon, radius);
        this.color = color;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }
}
