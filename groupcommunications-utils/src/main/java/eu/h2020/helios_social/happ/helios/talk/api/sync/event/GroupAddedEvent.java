package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import javax.annotation.concurrent.Immutable;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;

/**
 * An event that is broadcast when a group is added to the database.
 */
@Immutable
@NotNullByDefault
public class GroupAddedEvent extends Event {

	private final Group group;
	private final GroupType groupType;

	public GroupAddedEvent(Group group, GroupType groupType) {
		this.group = group;
		this.groupType = groupType;
	}

	public Group getGroup() {
		return group;
	}

	public GroupType getGroupType() {
		return groupType;
	}
}
