package eu.h2020.helios_social.modules.groupcommunications.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.Event;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventListener;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.IoExecutor;
import eu.h2020.helios_social.happ.helios.talk.api.sync.event.AckMessageEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessagingManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.messaging.event.PrivateMessageReceivedEvent;
import eu.h2020.helios_social.modules.socialgraphmining.GNN.GNNMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;
import static java.util.logging.Logger.getLogger;

public class MessagingManagerImpl implements MessagingManager, EventListener {
    private static final Logger LOG =
            getLogger(MessagingManagerImpl.class.getName());

    private final DatabaseComponent db;
    private final Executor ioExecutor;
    private final MessageTracker messageTracker;
    private final ContextualEgoNetwork egoNetwork;
    private final MiningManager miningManager;
    private final CommunicationManager communicationManager;
    private final EventBus eventBus;
    private final Encoder encoder;

    @Inject
    public MessagingManagerImpl(DatabaseComponent db, @IoExecutor Executor ioExecutor,
                                ContextualEgoNetwork egoNetwork, MessageTracker messageTracker,
                                CommunicationManager communicationManager, Encoder encoder,
                                MiningManager miningManager,
                                EventBus eventBus) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.messageTracker = messageTracker;
        this.egoNetwork = egoNetwork;
        this.miningManager = miningManager;
        this.communicationManager = communicationManager;
        this.encoder = encoder;
        this.eventBus = eventBus;
        this.eventBus.addListener(this);
    }

    @Override
    public MessageHeader sendPrivateMessage(ContactId contactId,
                                            String contextId,
                                            Message privateMessage)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        MessageHeader messageHeader = new MessageHeader(
                privateMessage.getId(),
                privateMessage.getGroupId(),
                privateMessage.getTimestamp(),
                MessageState.PENDING,
                false,
                false,
                privateMessage.getMessageType());
        try {
            db.addMessage(txn, privateMessage, MessageState.PENDING, contextId,
                    false);
            messageTracker.trackOutgoingMessage(txn, privateMessage);
            try {
                Interaction interaction = egoNetwork
                        .getCurrentContext()
                        .getOrAddEdge(
                                egoNetwork.getEgo(),
                                egoNetwork.getOrCreateNode(contactId.getId()))
                        .addDetectedInteraction(null);

                String preferences =
                        miningManager.getSocialGraphMiner().getModelParameters(interaction);
                privateMessage.setPreferences(preferences);

                communicationManager
                        .sendDirectMessage(PRIVATE_MESSAGE_PROTOCOL, contactId,
                                privateMessage);
                db.setMessageState(txn, privateMessage.getId(),
                        MessageState.DELIVERED);
                messageHeader.setMessageState(MessageState.DELIVERED);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageHeader;
    }

    @Override
    public GroupMessageHeader sendGroupMessage(Group group,
                                               GroupMessage groupMessage)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(false);
        GroupMessageHeader messageHeader = new GroupMessageHeader(
                groupMessage.getId(),
                groupMessage.getGroupId(),
                groupMessage.getTimestamp(),
                MessageState.DELIVERED,
                false,
                false,
                groupMessage.getMessageType(),
                groupMessage.getPeerInfo());
        try {
            if (group instanceof PrivateGroup) {
                PrivateGroup privateGroup = (PrivateGroup) group;
                db.addMessage(txn, groupMessage, MessageState.DELIVERED,
                        privateGroup.getContextId(),
                        false);
                addMessageMetadata(txn, groupMessage.getId(),
                        groupMessage.getPeerInfo());
                messageTracker.trackOutgoingMessage(txn, groupMessage);
                communicationManager.sendGroupMessage(
                        privateGroup.getId(),
                        privateGroup.getPassword(),
                        groupMessage
                );
            } else {
                Forum forum = (Forum) group;
                db.addMessage(txn, groupMessage, MessageState.DELIVERED,
                        forum.getContextId(),
                        false);
                addMessageMetadata(txn, groupMessage.getId(),
                        groupMessage.getPeerInfo());
                messageTracker.trackOutgoingMessage(txn, groupMessage);
                communicationManager.sendGroupMessage(
                        forum.getId(),
                        forum.getPassword(),
                        groupMessage
                );
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageHeader;
    }

    private void addMessageMetadata(Transaction txn, String messageId,
                                    PeerInfo peerInfo) throws FormatException, DbException {
        BdfDictionary meta = BdfDictionary.of(
                new BdfEntry(PEER_ID, peerInfo.getPeerId().getId()),
                new BdfEntry(PEER_ALIAS, peerInfo.getAlias())
                //new BdfEntry(PEER_REAL_NAME, peerInfo.getRealName()),
        );
        if (peerInfo.getFunnyName() != null)
            meta.put(PEER_FUNNY_NAME, peerInfo.getFunnyName());
        db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
    }

    @Override
    public void eventOccurred(Event e) {
        if (e instanceof AckMessageEvent) {
            LOG.info("sending ack...");
            ioExecutor.execute(() -> {
                try {
                    communicationManager.sendDirectMessage(
                            PRIVATE_MESSAGE_PROTOCOL,
                            ((AckMessageEvent) e).getContactId(),
                            ((AckMessageEvent) e).getAck()
                    );
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (ExecutionException ex) {
                    ex.printStackTrace();
                } catch (TimeoutException ex) {
                    ex.printStackTrace();
                }
            });
        }
    }
}
