package eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class GroupInfo implements AbstractMessage {

	private String contextId;
	private String groupId;
	private String name;
	private GroupInvitationType groupInvitationType;
	private String json;
	private long timestamp;

	public GroupInfo(String contextId, String groupId, String name,
			GroupInvitationType groupInvitationType, String json,
			long timestamp) {
		this.contextId = contextId;
		this.groupId = groupId;
		this.name = name;
		this.groupInvitationType = groupInvitationType;
		this.json = json;
		this.timestamp = timestamp;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getContextId() {
		return contextId;
	}

	public String getName() {
		return name;
	}

	public GroupInvitationType getGroupInvitationType() {
		return groupInvitationType;
	}

	public String getJson() {
		return json;
	}

	public long getTimestamp() {
		return timestamp;
	}
}
