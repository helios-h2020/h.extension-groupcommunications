package eu.h2020.helios_social.modules.groupcommunications.context.proxy;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class GeneralContextProxy extends Context implements AbstractMessage {
    private Integer color;

    public GeneralContextProxy(@NotNull String id, @NotNull String name, Integer color,
                               boolean active) {
        super(id, name, active);
        this.color = color;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }
}
