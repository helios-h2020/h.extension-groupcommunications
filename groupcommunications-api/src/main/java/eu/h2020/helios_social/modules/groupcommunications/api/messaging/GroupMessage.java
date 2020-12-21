package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

import org.jetbrains.annotations.NotNull;

import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;

public class GroupMessage extends Message {

	private PeerInfo peerInfo;

	public GroupMessage(@NotNull String id, @NotNull String groupId,
			long timestamp, String body, String mediaFileName, Type type,
			PeerInfo peerInfo) {
		super(id, groupId, timestamp, body, mediaFileName, type);
		this.peerInfo = peerInfo;
	}

	public GroupMessage(@NotNull String id, @NotNull String groupId,
			long timestamp, String body, Message.Type type, PeerInfo peerInfo) {
		super(id, groupId, timestamp, body, type);
		this.peerInfo = peerInfo;
	}


	public PeerInfo getPeerInfo() {
		return peerInfo;
	}
}
