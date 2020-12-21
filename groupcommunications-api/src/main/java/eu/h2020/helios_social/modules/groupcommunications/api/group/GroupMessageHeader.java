package eu.h2020.helios_social.modules.groupcommunications.api.group;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;

public class GroupMessageHeader extends MessageHeader {

	private PeerInfo peerInfo;

	public GroupMessageHeader(String messageId, String groupId, long timestamp,
			MessageState state, boolean incoming, boolean favourite,
			Message.Type messageType, PeerInfo peerInfo) {
		super(messageId, groupId, timestamp, state, incoming, favourite,
				messageType);
		this.peerInfo = peerInfo;
	}

	public GroupMessageHeader(MessageHeader messageHeader, PeerInfo peerInfo) {
		super(messageHeader.getMessageId(), messageHeader.getGroupId(),
				messageHeader.getTimestamp(), messageHeader.getMessageState(),
				messageHeader.isIncoming(), messageHeader.isFavourite(),
				messageHeader.getMessageType());
		this.peerInfo = peerInfo;
	}

	public PeerInfo getPeerInfo() {
		return peerInfo;
	}
}
