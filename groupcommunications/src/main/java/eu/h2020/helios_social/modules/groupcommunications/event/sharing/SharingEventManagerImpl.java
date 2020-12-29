package eu.h2020.helios_social.modules.groupcommunications.event.sharing;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.event.sharing.SharingEventManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;

public class SharingEventManagerImpl implements SharingEventManager {

    private final CommunicationManager communicationManager;
    private final Clock clock;

    public SharingEventManagerImpl(CommunicationManager communicationManager, Clock clock) {
        this.communicationManager = communicationManager;
        this.clock = clock;
    }

    @Override
    public void shareEvent(ContactId contactId, String groupId, HeliosEvent heliosEvent) {
        Message eventMessage = new Message(UUID.randomUUID().toString(), groupId,
                clock.currentTimeMillis(),
                heliosEvent.toJson(), Message.Type.EVENT);
        try {
            communicationManager.sendDirectMessage(PRIVATE_MESSAGE_PROTOCOL, contactId,
                    eventMessage);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shareEvent(Group group, HeliosEvent heliosEvent) {
        Message eventMessage = new Message(UUID.randomUUID().toString(), group.getId(),
                clock.currentTimeMillis(),
                heliosEvent.toJson(), Message.Type.EVENT);
        if (group instanceof PrivateGroup) {
            PrivateGroup privateGroup = (PrivateGroup) group;
            communicationManager.sendGroupMessage(privateGroup.getId(),
                    privateGroup.getPassword(), eventMessage);
        } else {
            Forum forum = (Forum) group;
            communicationManager.sendGroupMessage(forum.getId(),
                    forum.getPassword(), eventMessage);
        }
    }
}
