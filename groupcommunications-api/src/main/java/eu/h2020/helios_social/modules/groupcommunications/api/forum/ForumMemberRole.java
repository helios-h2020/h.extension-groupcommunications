package eu.h2020.helios_social.modules.groupcommunications.api.forum;

/**
 * Available Forum Member Roles
 */
public enum ForumMemberRole {

	ADMINISTRATOR(0),
	MODERATOR(1),
	EDITOR(2),
	READER(3),
	OUTSIDER(4);

	private final int value;

	ForumMemberRole(int value) {
		this.value = value;
	}

	public static ForumMemberRole valueOf(int value) {
		for (ForumMemberRole role : values())
			if (role.value == value) return role;
		return null;
	}

	public int getInt() {
		return value;
	}
}
