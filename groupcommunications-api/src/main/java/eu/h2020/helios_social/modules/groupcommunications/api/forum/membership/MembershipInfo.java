package eu.h2020.helios_social.modules.groupcommunications.api.forum.membership;

import java.util.Collection;
import java.util.List;

import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

public class MembershipInfo implements AbstractMessage {

	public enum Action {
		ADD_MODERATOR, REMOVE_MODERATOR, UPDATE_ROLE, JOIN_FORUM, LEAVE_FORUM
	}

	private String groupId;
	private PeerId peerId;
	private ForumMemberRole role;
	private String alias;
	private long timestamp;
	private Action action;
	private Collection<ForumMember> forumMemberList;

	public MembershipInfo(String groupId,
			PeerId peerId,
			ForumMemberRole role,
			String alias,
			long timestamp) {
		this.groupId = groupId;
		this.peerId = peerId;
		this.role = role;
		this.timestamp = timestamp;
	}


	public String getGroupId() {
		return groupId;
	}

	public PeerId getPeerId() {
		return peerId;
	}

	public ForumMemberRole getRole() {
		return role;
	}

	public String getAlias() {
		return alias;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Action getAction() {
		return action;
	}

	public Collection<ForumMember> getForumMemberList() {
		return forumMemberList;
	}

	public void setForumMemberList(Collection<ForumMember> forumMemberList) {
		this.forumMemberList = forumMemberList;
	}

	public void setAction(Action action) {
		this.action = action;
	}
}
