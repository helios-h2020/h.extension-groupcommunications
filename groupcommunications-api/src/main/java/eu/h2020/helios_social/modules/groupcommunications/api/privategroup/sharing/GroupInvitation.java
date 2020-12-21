package eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.sharing.Invitation;

public class GroupInvitation extends Invitation implements AbstractMessage {

	private String groupId;
	private GroupInvitationType groupInvitationType;

	public GroupInvitation(ContactId contactId, String contextId,
			String groupId,
			String name, GroupInvitationType groupInvitationType, String json,
			long timestamp,
			boolean incoming) {
		super(contactId, contextId, name, json, timestamp, incoming);
		this.groupId = groupId;
		this.groupInvitationType = groupInvitationType;
	}

	public String getGroupId() {
		return groupId;
	}

	public GroupInvitationType getGroupInvitationType() {
		return groupInvitationType;
	}
}
