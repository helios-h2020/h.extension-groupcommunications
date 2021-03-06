package eu.h2020.helios_social.modules.groupcommunications.group;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.Identity;
import eu.h2020.helios_social.modules.groupcommunications_utils.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.GroupMessageFactory;

public class GroupMessageFactoryImpl implements
        GroupMessageFactory {

    private final IdentityManager identityManager;

    @Inject
    public GroupMessageFactoryImpl(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    @Override
    public GroupMessage createGroupMessage(String groupId,
                                           String text, long timestamp, String funnyName, String fakeId) {
        Identity identity = identityManager.getIdentity();
        PeerInfo peerInfo;
        if (funnyName == null)
            peerInfo = new PeerInfo.Builder()
                    .peerId(new PeerId(identity.getNetworkId(), fakeId))
                    .alias(identity.getAlias()).build();
        else
            peerInfo = new PeerInfo.Builder()
                    .peerId(new PeerId(null, fakeId))
                    .funny_name(funnyName).build();
        return new GroupMessage(UUID.randomUUID().toString(), groupId,
                                timestamp, text, Message.Type.TEXT, peerInfo);
    }

    @Override
    public GroupMessage createAttachmentMessage(String groupId, List<Attachment> attachments, Message.Type messageType,
                                                String text, long timestamp, String funnyName, String fakeId) {
        Identity identity = identityManager.getIdentity();
        PeerInfo peerInfo;
        if (funnyName == null)
            peerInfo = new PeerInfo.Builder()
                    .peerId(new PeerId(identity.getNetworkId(), fakeId))
                    .alias(identity.getAlias()).build();
        else
            peerInfo = new PeerInfo.Builder()
                    .peerId(new PeerId(null, fakeId))
                    .funny_name(funnyName).build();

        return new GroupMessage(UUID.randomUUID().toString(), groupId,
                                timestamp, text, messageType, attachments, peerInfo);
    }
}
