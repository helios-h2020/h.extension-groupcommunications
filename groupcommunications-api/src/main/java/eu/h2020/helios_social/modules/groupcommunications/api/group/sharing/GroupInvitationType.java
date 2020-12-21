package eu.h2020.helios_social.modules.groupcommunications.api.group.sharing;

public enum GroupInvitationType {

	PrivateGroup(0), Forum(1), LocationForum(2), SeasonalForum(3);

	private int value;

	GroupInvitationType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static GroupInvitationType fromValue(int value) {
		for (GroupInvitationType g : values()) if (g.value == value) return g;
		throw new IllegalArgumentException();
	}
}
