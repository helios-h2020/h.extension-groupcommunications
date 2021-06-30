package eu.h2020.helios_social.modules.groupcommunications.messaging;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.AttachmentManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.lifecycle.IoExecutor;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.AckMessageEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessagingManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.MessageReceivedFromUnknownGroupEvent;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.ATTACHMENTS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;
import static java.util.logging.Logger.getLogger;

public class MessagingManagerImpl implements MessagingManager, EventListener {
    private static final Logger LOG =
            getLogger(MessagingManagerImpl.class.getName());

    private final DatabaseComponent db;
    private final Executor ioExecutor;
    private final GroupManager groupManager;
    private final MessageTracker messageTracker;
    private final ContextualEgoNetwork egoNetwork;
    private final SwitchableMiner switchableMiner;
    private final CommunicationManager communicationManager;
    private final Encoder encoder;
    private final AttachmentManager attachmentManager;

    @Inject
    public MessagingManagerImpl(DatabaseComponent db, @IoExecutor Executor ioExecutor,
                                GroupManager groupManager,
                                ContextualEgoNetwork egoNetwork, MessageTracker messageTracker,
                                CommunicationManager communicationManager, Encoder encoder,
                                SwitchableMiner switchableMiner,
                                AttachmentManager attachmentManager) {
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.groupManager = groupManager;
        this.messageTracker = messageTracker;
        this.egoNetwork = egoNetwork;
        this.switchableMiner = switchableMiner;
        this.communicationManager = communicationManager;
        this.encoder = encoder;
        this.attachmentManager = attachmentManager;
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
                privateMessage.getMessageType(),
                privateMessage.getMessageBody() != null);
        try {
            db.addMessage(txn, privateMessage, MessageState.PENDING, contextId,
                          false);
            if (privateMessage.getMessageType().equals(Message.Type.IMAGES) ||
                    privateMessage.getMessageType().equals(Message.Type.FILE_ATTACHMENT)) {
                messageHeader.setAttachments(privateMessage.getAttachments());
                try {
                    attachmentManager.storeOutgoingAttachmentsToExternalStorage(privateMessage.getAttachments());
                    addAttachmentMetadata(txn, privateMessage.getId(), privateMessage.getAttachments());
                } catch (FormatException e) {
                    e.printStackTrace();
                }
            }

            messageTracker.trackOutgoingMessage(txn, privateMessage);

            Interaction interaction = egoNetwork
                    .getCurrentContext()
                    .getOrAddEdge(
                            egoNetwork.getEgo(),
                            egoNetwork.getOrCreateNode(contactId.getId()))
                    .addDetectedInteraction(null);

            String preferences = switchableMiner.getModelParameters(interaction);
            privateMessage.setPreferences(preferences);

            sendMessage(PRIVATE_MESSAGE_PROTOCOL, contactId,
                        privateMessage);

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
                groupMessage.getMessageBody() != null,
                groupMessage.getPeerInfo()
        );
        try {
            if (group instanceof PrivateGroup) {
                PrivateGroup privateGroup = (PrivateGroup) group;
                db.addMessage(txn, groupMessage, MessageState.DELIVERED,
                              privateGroup.getContextId(),
                              false);
                addMessageMetadata(txn, groupMessage.getId(),
                                   groupMessage.getPeerInfo());
                if (groupMessage.getMessageType().equals(Message.Type.IMAGES) ||
                        groupMessage.getMessageType().equals(Message.Type.FILE_ATTACHMENT)) {
                    messageHeader.setAttachments(groupMessage.getAttachments());
                    try {
                        attachmentManager.storeOutgoingAttachmentsToExternalStorage(groupMessage.getAttachments());
                        addAttachmentMetadata(txn, groupMessage.getId(), groupMessage.getAttachments());
                    } catch (FormatException e) {
                        e.printStackTrace();
                    }
                }
                messageTracker.trackOutgoingMessage(txn, groupMessage);
            } else {
                Forum forum = (Forum) group;
                ForumMemberRole role = groupManager.getRole(txn, forum);

                if (role.getInt() <= 2) {
                    db.addMessage(txn, groupMessage, MessageState.DELIVERED,
                                  forum.getContextId(),
                                  false);
                    addMessageMetadata(txn, groupMessage.getId(),
                                       groupMessage.getPeerInfo());
                    if (groupMessage.getMessageType().equals(Message.Type.IMAGES) ||
                            groupMessage.getMessageType().equals(Message.Type.FILE_ATTACHMENT)) {
                        messageHeader.setAttachments(groupMessage.getAttachments());
                        try {
                            attachmentManager.storeOutgoingAttachmentsToExternalStorage(groupMessage.getAttachments());
                            addAttachmentMetadata(txn, groupMessage.getId(), groupMessage.getAttachments());
                        } catch (FormatException e) {
                            e.printStackTrace();
                        }
                    }
                    messageTracker.trackOutgoingMessage(txn, groupMessage);
                }
            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        sendMessage(group, groupMessage);
        return messageHeader;
    }

    private void addMessageMetadata(Transaction txn, String messageId,
                                    PeerInfo peerInfo) throws FormatException, DbException {
        LOG.info(peerInfo.toString());
        BdfDictionary meta = new BdfDictionary();
        if (peerInfo.getPeerId().getId() != null) {
            meta.put(PEER_ID, peerInfo.getPeerId().getId());
            meta.put(PEER_ALIAS, peerInfo.getAlias());
        } else {
            meta.put(PEER_FAKE_ID, peerInfo.getPeerId().getFakeId());
            meta.put(PEER_FUNNY_NAME, peerInfo.getFunnyName());
        }
        db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
    }

    private void addAttachmentMetadata(Transaction txn, String messageId,
                                       List<Attachment> attachments) throws FormatException, DbException {
        BdfDictionary meta = new BdfDictionary();
        BdfList attachmentList = new BdfList();
        for (Attachment a : attachments) {
            attachmentList.add(BdfList.of(a.getUri(), a.getUrl(), a.getContentType(), a.getAttachmentName()));
        }
        meta.put(ATTACHMENTS, attachmentList);
        db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
    }

    private void addAttachmentMetadata(String messageId,
                                       List<Attachment> attachments) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            addAttachmentMetadata(txn, messageId, attachments);
            db.commitTransaction(txn);
        } catch (FormatException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void eventOccurred(Event e) {
        if (e instanceof AckMessageEvent) {
            LOG.info("sending ack...");
            ioExecutor.execute(() -> {
                communicationManager.sendDirectMessage(
                        PRIVATE_MESSAGE_PROTOCOL,
                        ((AckMessageEvent) e).getContactId(),
                        ((AckMessageEvent) e).getAck()
                );
            });
        } else if (e instanceof MessageReceivedFromUnknownGroupEvent) {
            ioExecutor.execute(() -> {
                communicationManager.sendDirectMessage(
                        PRIVATE_MESSAGE_PROTOCOL,
                        new PeerId(((MessageReceivedFromUnknownGroupEvent) e).getPeerId()),
                        new Message(((MessageReceivedFromUnknownGroupEvent) e).getGroupId())
                );
            });
        }
    }

    private void sendMessage(Group group, Message groupMessage) {
        ioExecutor.execute(() -> {
            if (groupMessage.getMessageType().equals(Message.Type.IMAGES) || groupMessage.getMessageType().equals(Message.Type.FILE_ATTACHMENT)) {
                attachmentManager.uploadAttachments(groupMessage.getAttachments());
                try {
                    addAttachmentMetadata(groupMessage.getId(), groupMessage.getAttachments());
                } catch (DbException e) {
                    e.printStackTrace();
                }
                for (Attachment attachment : groupMessage.getAttachments()) {
                    attachment.setUri(null);
                }

                LOG.info("Attachments: " + groupMessage.getAttachments());
            }
            if (group instanceof PrivateGroup) {
                communicationManager.sendGroupMessage(
                        group.getId(),
                        ((PrivateGroup) group).getPassword(),
                        groupMessage
                );
            } else if (group instanceof Forum)
                communicationManager.sendGroupMessage(
                        group.getId(),
                        ((Forum) group).getPassword(),
                        groupMessage
                );
        });
    }

    private void sendMessage(String protocol, ContactId contactId, Message message) {
        ioExecutor.execute(() -> {
            if (message.getMessageType().equals(Message.Type.IMAGES) || message.getMessageType().equals(Message.Type.FILE_ATTACHMENT)) {
                attachmentManager.uploadAttachments(message.getAttachments());
                LOG.info("Attachments: " + message.getAttachments());
                try {
                    addAttachmentMetadata(message.getId(), message.getAttachments());
                } catch (DbException e) {
                    e.printStackTrace();
                }
                for (Attachment attachment : message.getAttachments()) {
                    attachment.setUri(null);
                }
                LOG.info("Attachments: " + message.getAttachments());

            }

            communicationManager.sendDirectMessage(
                    protocol,
                    contactId,
                    message
            );

        });
    }
}
