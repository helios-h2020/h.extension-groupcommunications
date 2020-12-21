package eu.h2020.helios_social.happ.helios.talk.api.context;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;

public class RemovePendingContextEvent extends Event {

	private String pendingContextId;
	public RemovePendingContextEvent(String pendingContextId) {
		this.pendingContextId = pendingContextId;
	}

	public String getPendingContextId() {
		return pendingContextId;
	}
}
