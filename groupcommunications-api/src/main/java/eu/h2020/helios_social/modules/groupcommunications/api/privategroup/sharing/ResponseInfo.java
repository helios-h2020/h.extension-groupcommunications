package eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class ResponseInfo implements AbstractMessage {

	public enum Response {
		ACCEPT, REJECT
	}

	private Response response;
	private String groupId;
	private GroupInvitationType groupInvitationType;

	public ResponseInfo(Response response, String groupId,
			GroupInvitationType groupInvitationType) {
		this.response = response;
		this.groupId = groupId;
		this.groupInvitationType = groupInvitationType;
	}

	public Response getResponse() {
		return response;
	}

	public String getGroupId() {
		return groupId;
	}

	public GroupInvitationType getGroupInvitationType() {
		return groupInvitationType;
	}
}
