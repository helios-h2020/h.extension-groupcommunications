package eu.h2020.helios_social.modules.groupcommunications.api.event.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;

public interface SharingEventManager {

    public void sendEventInvitation(ContactId contactId,
		    HeliosEvent heliosEvent);

}
