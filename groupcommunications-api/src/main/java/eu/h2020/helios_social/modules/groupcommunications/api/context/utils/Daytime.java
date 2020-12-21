package eu.h2020.helios_social.modules.groupcommunications.api.context.utils;

import java.time.LocalTime;

public enum Daytime {

	MORNING(0), AFTERNOON(1), EVENING(2), NIGHT(3);
	private int value;

	Daytime(int value) {
		this.value = value;
	}

	private static final LocalTime morning = LocalTime.of(0, 0, 0);
	private static final LocalTime afternoon = LocalTime.of(12, 0, 0);
	private static final LocalTime evening = LocalTime.of(16, 0, 0);
	private static final LocalTime night = LocalTime.of(21, 0, 0);

	static public Daytime of(final LocalTime time) {
		if (!time.isBefore(morning) && time.isBefore(afternoon))
			return MORNING;
		else if (!time.isBefore(afternoon) && time.isBefore(evening))
			return AFTERNOON;
		else if (!time.isBefore(evening) && time.isBefore(night))
			return EVENING;
		else
			return NIGHT;
	}

	public int getValue() {
		return value;
	}

	public static Daytime fromValue(int value) {
		for (Daytime s : values()) if (s.value == value) return s;
		throw new IllegalArgumentException();
	}
}
