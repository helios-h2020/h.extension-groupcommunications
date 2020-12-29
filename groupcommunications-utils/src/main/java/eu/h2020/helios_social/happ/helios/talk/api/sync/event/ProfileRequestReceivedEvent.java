package eu.h2020.helios_social.happ.helios.talk.api.sync.event;

import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public class ProfileRequestReceivedEvent extends Event {

    private ContactId contactId;
    private String contextId;

    public ProfileRequestReceivedEvent(ContactId contactId, String contextId) {
        this.contactId = contactId;
        this.contextId = contextId;
    }

    public ContactId getContactId() {
        return contactId;
    }

    public String getContextId() {
        return contextId;
    }
}
