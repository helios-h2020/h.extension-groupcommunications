package eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;

public interface GroupInvitationFactory {

	GroupInvitation createIncomingGroupInvitation(ContactId contactId,
			GroupInfo groupInfo);

	GroupInvitation createOutgoingGroupInvitation(ContactId contactId,
			Group group);
}
