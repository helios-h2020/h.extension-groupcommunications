package eu.h2020.helios_social.modules.groupcommunications.messaging.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;

public class GroupMessageReceivedEvent extends Event {

	private GroupMessageHeader messageHeader;

	public GroupMessageReceivedEvent(GroupMessageHeader messageHeader) {
		this.messageHeader = messageHeader;
	}

	public GroupMessageHeader getMessageHeader() {
		return messageHeader;
	}

}
