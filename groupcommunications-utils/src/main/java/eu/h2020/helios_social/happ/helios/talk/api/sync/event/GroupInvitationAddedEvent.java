package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;

public class GroupInvitationAddedEvent extends Event {

	private GroupInvitation invite;

	public GroupInvitationAddedEvent(GroupInvitation invite) {
		this.invite = invite;
	}

	public GroupInvitation getInvite() {
		return invite;
	}

}
