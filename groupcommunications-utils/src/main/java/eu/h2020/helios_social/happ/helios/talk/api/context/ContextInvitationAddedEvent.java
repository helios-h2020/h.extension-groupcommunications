package eu.h2020.helios_social.happ.helios.talk.api.context;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;

public class ContextInvitationAddedEvent extends Event {

	private ContextInvitation invite;

	public ContextInvitationAddedEvent(ContextInvitation invite) {
		this.invite = invite;
	}

	public ContextInvitation getInvite() {
		return invite;
	}
}
