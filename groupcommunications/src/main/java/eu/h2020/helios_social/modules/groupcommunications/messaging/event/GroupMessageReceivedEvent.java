package eu.h2020.helios_social.modules.groupcommunications.messaging.event;

import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;

public class GroupMessageReceivedEvent extends Event {

	private GroupMessageHeader messageHeader;

	public GroupMessageReceivedEvent(GroupMessageHeader messageHeader) {
		this.messageHeader = messageHeader;
	}

	public GroupMessageHeader getMessageHeader() {
		return messageHeader;
	}

}
