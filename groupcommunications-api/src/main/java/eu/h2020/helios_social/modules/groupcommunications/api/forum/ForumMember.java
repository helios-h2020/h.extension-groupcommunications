package eu.h2020.helios_social.modules.groupcommunications.api.forum;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.Peer;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

/**
 * A class for Forum members
 */
public class ForumMember implements AbstractMessage {

	private PeerId peerId;
	private ForumMemberRole role;
	private String groupId;
	private String alias;
	private long lstUpdateTimestamp;

	public ForumMember(PeerId peerId, String groupId, String alias,
			ForumMemberRole role,
			long lstUpdateTimestamp) {
		this.peerId = peerId;
		this.role = role;
		this.groupId = groupId;
		this.alias = alias;
		this.lstUpdateTimestamp = lstUpdateTimestamp;
	}

	public String getGroupId() {
		return groupId;
	}

	public PeerId getPeerId() {
		return peerId;
	}

	public String getAlias() {
		return alias;
	}

	public boolean isModerator() {
		return (role == ForumMemberRole.MODERATOR) ||
				(role == ForumMemberRole.ADMINISTRATOR);
	}

	public long getLstTimestamp() {
		return lstUpdateTimestamp;
	}

	/**
	 * Returns Member's Role
	 *
	 * @return
	 */
	public ForumMemberRole getRole() {
		return role;
	}

}
