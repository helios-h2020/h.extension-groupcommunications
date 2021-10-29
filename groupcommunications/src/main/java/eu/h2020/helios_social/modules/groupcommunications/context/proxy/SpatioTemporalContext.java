package eu.h2020.helios_social.modules.groupcommunications.context.proxy;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.core.context.ContextAnd;
import eu.h2020.helios_social.core.context.ContextListener;
import eu.h2020.helios_social.core.context.ext.LocationContext;
import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.core.sensor.SensorValueListener;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;


//TODO: Implement SpatioTemporalContext
public class SpatioTemporalContext extends Context implements AbstractMessage, ContextListener {
    private Integer color;
    private String privateName;
    private long startTime;
    private long endTime;
    private int repeat;
    private double lat;
    private double lon;
    private double radius;
    private LocationContext contextA;
    private TimeContext contextB;
    private long contextStateChangeTimestamp = 0;
    public SpatioTemporalContext(@NotNull String id, String name, @NotNull Integer color,
                                 @NotNull String privateName, @NotNull LocationContext contextA,
                                 @NotNull TimeContext contextB) {
        super(id, name, false);
        this.color = color;
        this.privateName = privateName;
        this.startTime = contextB.getStartTime();
        this.endTime = contextB.getEndTime();
        this.repeat = contextB.getRepeat();
        this.lat = contextA.getLat();
        this.lon = contextA.getLon();
        this.radius = contextA.getRadius();
        this.contextA = contextA;
        this.contextB = contextB;
        contextA.registerContextListener(this);
        contextB.registerContextListener(this);
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

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public int getRepeat() {
        return repeat;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getRadius() {
        return radius;
    }

    public LocationContext getContextA() {
        return contextA;
    }

    public TimeContext getContextB() {
        return contextB;
    }

    @Override
    public void contextChanged(boolean active) {
        if (this.isActive()!=(this.contextA.isActive() && this.contextB.isActive())){
            contextStateChangeTimestamp = System.currentTimeMillis();
            this.setActive(this.contextA.isActive() && this.contextB.isActive());
        }
    }

    public long getContextStateChangeTimestamp() {
        return contextStateChangeTimestamp;
    }
}
