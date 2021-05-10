package eu.h2020.helios_social.modules.groupcommunications.context.proxy;


import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.core.context.ext.TimeContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class TimeContextProxy extends TimeContext implements AbstractMessage {

	private Integer color;

	public TimeContextProxy(@NotNull String id, String name,
			@NotNull Integer color,
			@NotNull long startTime, @NotNull long endTime) {
		super(id, name, startTime, endTime);
		this.color = color;
	}

	public Integer getColor() {
		return color;
	}

	public void setColor(Integer color) {
		this.color = color;
	}


}
