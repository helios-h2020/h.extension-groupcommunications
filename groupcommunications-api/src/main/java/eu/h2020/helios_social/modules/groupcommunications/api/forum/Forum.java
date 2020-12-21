package eu.h2020.helios_social.modules.groupcommunications.api.forum;


import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

/**
 * Forum is a Sharable conversation
 */
public class Forum extends Group implements AbstractMessage {

	private String name;
	private Collection<String> moderators;
	private String password;
	private Collection<String> tags;
	private ForumMemberRole defaultMemberRole;

	/**
	 * Creates a sharable Forum with a given name
	 *
	 * @param groupId    GroupId of the Forum
	 * @param name       name of the forum
	 * @param moderators Peer Identifiers of forum's moderators
	 */
	public Forum(String groupId, String contextId, String name,
			String password, @NotNull Collection<String> moderators,
			GroupType groupType, Collection<String> tags,
			ForumMemberRole defaultRole) {
		super(groupId, contextId, groupType);
		this.name = name;
		this.password = password;
		this.moderators = moderators;
		this.groupType = groupType;
		if (groupType.getValue() < 1) {
			throw new IllegalArgumentException(groupType + " is not a Forum.");
		}
		this.tags = tags;
		this.defaultMemberRole = defaultRole;
	}

	/**
	 * @return name of the Forum
	 */
	public String getName() {
		return name;
	}

	public Collection<String> getModerators() {
		return moderators;
	}

	public String getPassword() {
		return password;
	}

	public GroupType getGroupType() {
		return groupType;
	}

	public Collection<String> getTags() {
		return tags;
	}

	public ForumMemberRole getDefaultMemberRole() {
		return defaultMemberRole;
	}
}
