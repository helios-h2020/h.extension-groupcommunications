package eu.h2020.helios_social.modules.groupcommunications.api.context.utils;

import java.time.Month;

import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumType;

public enum Season {
	SPRING(0), SUMMER(1), FALL(2), WINTER(3);

	private int value;

	Season(int value) {
		this.value = value;
	}

	static public Season of(final Month month) {
		switch (month) {
			case MARCH:
				return Season.SPRING;
			case APRIL:
				return Season.SPRING;
			case MAY:
				return Season.SUMMER;
			case JUNE:
				return Season.SUMMER;
			case JULY:
				return Season.SUMMER;
			case AUGUST:
				return Season.SUMMER;
			case SEPTEMBER:
				return Season.FALL;
			case OCTOBER:
				return Season.FALL;
			case NOVEMBER:
				return Season.WINTER;
			case DECEMBER:
				return Season.WINTER;
			case JANUARY:
				return Season.WINTER;
			case FEBRUARY:
				return Season.WINTER;
			default:
				return null;
		}
	}

    public int getValue() {
        return value;
    }

    public static Season fromValue(int value) {
        for (Season s : values()) if (s.value == value) return s;
        throw new IllegalArgumentException();
    }

}
