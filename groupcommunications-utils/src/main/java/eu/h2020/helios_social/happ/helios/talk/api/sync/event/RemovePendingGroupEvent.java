package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;

public class RemovePendingGroupEvent extends Event {

	private String pendingGroupId;

	public RemovePendingGroupEvent(String pendingGroupId) {
		this.pendingGroupId = pendingGroupId;
	}

	public String getPendingGroupId() {
		return pendingGroupId;
	}
}
