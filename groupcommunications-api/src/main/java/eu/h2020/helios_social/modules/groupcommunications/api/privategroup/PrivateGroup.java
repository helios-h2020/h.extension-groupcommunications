package eu.h2020.helios_social.modules.groupcommunications.api.privategroup;


import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

/**
 * A Non-sharable Private Group Conversation
 */
public class PrivateGroup extends Group implements AbstractMessage {

	private String name;
	private final String owner;
	private String password;

	/**
	 * Contracts a new instance of a Private Group
	 *
	 * @param groupId   Private Group Identifier
	 * @param contextId Context Identifier
	 * @param name      Name of Private Group
	 * @param password
	 * @param owner
	 */
	public PrivateGroup(@NotNull String groupId, String contextId,
			@NotNull String name, @NotNull String password, String owner) {
		super(groupId, contextId, GroupType.PrivateGroup);
		this.name = name;
		this.owner = owner;
		this.password = password;
	}

	/**
	 * Get tbe name of the Group
	 *
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get Peer Identifier of Group's Owner
	 *
	 * @return Peer Identifier
	 */
	public String getOwner() {
		return owner;
	}

	public String getPassword() {
		return password;
	}
}
