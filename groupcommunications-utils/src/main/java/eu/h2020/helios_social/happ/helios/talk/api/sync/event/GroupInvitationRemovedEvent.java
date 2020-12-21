package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public class GroupInvitationRemovedEvent extends Event {

	private ContactId contactId;
	private String pendingGroupId;

	public GroupInvitationRemovedEvent(ContactId contactId,
			String pendingGroupId) {
		this.contactId = contactId;
		this.pendingGroupId = pendingGroupId;
	}

	public ContactId getContactId() {
		return contactId;
	}

	public String getPendingGroupId() {
		return pendingGroupId;
	}
}
