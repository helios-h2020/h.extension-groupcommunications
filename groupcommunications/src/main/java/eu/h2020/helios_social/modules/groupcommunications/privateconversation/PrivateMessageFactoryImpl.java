package eu.h2020.helios_social.modules.groupcommunications.privateconversation;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.ContactInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privateconversation.PrivateMessageFactory;

public class PrivateMessageFactoryImpl
        implements PrivateMessageFactory {

    @Inject
    public PrivateMessageFactoryImpl() {

    }

    @Override
    public Message createTextMessage(String groupId, long timestamp,
                                     String text) {
        return new Message(UUID.randomUUID().toString(), groupId, timestamp,
                           text,
                           Message.Type.TEXT);
    }

    @Override
    public Message createAttachmentMessage(String groupId, long timestamp, String text,
                                           List<Attachment> attachments, Message.Type messageType) {
        return new Message(UUID.randomUUID().toString(), groupId, timestamp,
                           text, messageType, attachments);
    }

    @Override
    public Message createVideoCallMessage(String groupId, long timestamp,
                                          String room_id) {
        return new Message(UUID.randomUUID().toString(), groupId, timestamp,
                           room_id, Message.Type.VIDEOCALL);
    }

    @Override
    public Message createShareContactMessage(String groupId, long timestamp, PeerInfo peerInfo) {
        return new ContactInfo(UUID.randomUUID().toString(), peerInfo, groupId, timestamp);
    }
}
