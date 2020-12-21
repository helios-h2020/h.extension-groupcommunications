package eu.h2020.helios_social.happ.helios.talk.api.context;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;

public class ContextInvitationAutoResponseEvent extends Event {

	private ContextInvitation contextInvite;

	public ContextInvitationAutoResponseEvent(ContextInvitation contextInvite) {
		this.contextInvite = contextInvite;
	}

	public ContextInvitation getContextInvitation() {
		return contextInvite;
	}
}
