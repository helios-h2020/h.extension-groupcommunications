package eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

public class ForumInfo implements AbstractMessage {

	private String forumId;
	private Collection<PeerId> moderators;

	public ForumInfo(String forumId, Collection<PeerId> moderators) {
		this.forumId = forumId;
		this.moderators = moderators;
	}

	public String getForumId() {
		return forumId;
	}

	public Collection<PeerId> getModerators() {
		return moderators;
	}


}
