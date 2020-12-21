package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;

public class LeaveGroupEvent extends Event {

	private String groupId;
	private GroupType groupType;

	public LeaveGroupEvent(String groupId, GroupType groupType) {
		this.groupId = groupId;
		this.groupType = groupType;
	}

	public String getGroupId() {
		return groupId;
	}

	public GroupType getGroupType() {
		return groupType;
	}
}
