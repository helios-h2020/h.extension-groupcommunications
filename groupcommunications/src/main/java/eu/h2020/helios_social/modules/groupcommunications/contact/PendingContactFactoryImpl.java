package eu.h2020.helios_social.modules.groupcommunications.contact;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications_utils.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionInfo;

public class PendingContactFactoryImpl implements PendingContactFactory {

    private final Clock clock;

    @Inject
    PendingContactFactoryImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PendingContact createOutgoingPendingContact(String peerId,
                                                       String alias, String message) {
        return new PendingContact(new ContactId(peerId), alias,
                PendingContactType.OUTGOING,
                message, clock.currentTimeMillis());
    }

    @Override
    public PendingContact createIncomingPendingContact(String peerId,
                                                       ConnectionInfo connectionInfo) {
        return new PendingContact(new ContactId(peerId),
                connectionInfo.getAlias(),
                connectionInfo.getProfilePicture(),
                PendingContactType.INCOMING,
                connectionInfo.getMessage(), connectionInfo.getTimestamp());
    }
}
