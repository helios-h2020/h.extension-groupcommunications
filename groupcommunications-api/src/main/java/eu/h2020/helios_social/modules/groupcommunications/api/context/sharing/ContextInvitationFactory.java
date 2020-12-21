package eu.h2020.helios_social.modules.groupcommunications.api.context.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public interface ContextInvitationFactory<T> {

	ContextInvitation createOutgoingContextInvitation(ContactId contactId,
			T context);

	ContextInvitation createIncomingContextInvitation(ContactId contactId,
			ContextInfo contextInfo);
}
