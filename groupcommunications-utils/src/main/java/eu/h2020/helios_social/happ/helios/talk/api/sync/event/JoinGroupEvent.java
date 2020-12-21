package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;

public class JoinGroupEvent extends Event {

	private String groupId;
	private String password;
	private GroupType groupType;

	public JoinGroupEvent(String groupId, String password,
			GroupType groupType) {
		this.groupId = groupId;
		this.password = password;
		this.groupType = groupType;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getPassword() {
		return password;
	}

	public GroupType getGroupType() {
		return groupType;
	}
}
