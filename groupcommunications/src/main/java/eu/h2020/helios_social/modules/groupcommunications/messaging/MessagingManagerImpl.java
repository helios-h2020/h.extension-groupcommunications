package eu.h2020.helios_social.modules.groupcommunications.messaging;

import android.app.Application;
import android.net.Uri;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
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
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import io.tus.android.client.TusAndroidUpload;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

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

    private final Application app;
    private final DatabaseComponent db;
    private final Executor ioExecutor;
    private final GroupManager groupManager;
    private final MessageTracker messageTracker;
    private final ContextualEgoNetwork egoNetwork;
    private final MiningManager miningManager;
    private final TusClient tusClient;
    private final CommunicationManager communicationManager;
    private final EventBus eventBus;
    private final Encoder encoder;

    @Inject
    public MessagingManagerImpl(Application app, DatabaseComponent db, @IoExecutor Executor ioExecutor,
                                GroupManager groupManager,
                                ContextualEgoNetwork egoNetwork, MessageTracker messageTracker,
                                CommunicationManager communicationManager, Encoder encoder,
                                TusClient tusClient,
                                MiningManager miningManager,
                                EventBus eventBus) {
        this.app = app;
        this.db = db;
        this.ioExecutor = ioExecutor;
        this.groupManager = groupManager;
        this.messageTracker = messageTracker;
        this.egoNetwork = egoNetwork;
        this.miningManager = miningManager;
        this.tusClient = tusClient;
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
                privateMessage.getMessageType(),
                privateMessage.getMessageBody() != null);
        try {
            db.addMessage(txn, privateMessage, MessageState.PENDING, contextId,
                    false);
            if (privateMessage.getMessageType().equals(Message.Type.IMAGES) ||
                    privateMessage.getMessageType().equals(Message.Type.ATTACHMENT)) {
                messageHeader.setAttachments(privateMessage.getAttachments());
                try {
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

            String preferences =
                    miningManager.getSocialGraphMiner().getModelParameters(interaction);
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
                        groupMessage.getMessageType().equals(Message.Type.ATTACHMENT)) {
                    messageHeader.setAttachments(groupMessage.getAttachments());
                    try {
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
                            groupMessage.getMessageType().equals(Message.Type.ATTACHMENT)) {
                        messageHeader.setAttachments(groupMessage.getAttachments());
                        try {
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
            attachmentList.add(BdfList.of(a.getUri(), a.getUrl(), a.getContentType()));
        }
        meta.put(ATTACHMENTS, attachmentList);
        db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
    }

    private void addAttachmentMetadata(String messageId,
                                       List<Attachment> attachments) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            BdfDictionary meta = new BdfDictionary();
            BdfList attachmentList = new BdfList();
            for (Attachment a : attachments) {
                attachmentList.add(BdfList.of(a.getUri(), a.getUrl(), a.getContentType()));
            }
            meta.put(ATTACHMENTS, attachmentList);
            db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
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

    private void sendMessage(Group group, Message groupMessage) {
        ioExecutor.execute(() -> {
            if (groupMessage.getMessageType().equals(Message.Type.IMAGES) || groupMessage.getMessageType().equals(Message.Type.ATTACHMENT)) {
                for (Attachment attachment : groupMessage.getAttachments()) {
                    try {
                        TusUpload upload = new TusAndroidUpload(Uri.parse(attachment.getUri()), app);

                        TusUploader uploader = tusClient.resumeOrCreateUpload(upload);
                        long totalBytes = upload.getSize();
                        long uploadedBytes = uploader.getOffset();

                        // Upload file in 1MiB chunks
                        uploader.setChunkSize(1024 * 1024);

                        while (uploader.uploadChunk() > 0) {
                            uploadedBytes = uploader.getOffset();
                        }

                        uploader.finish();
                        attachment.setUrl(uploader.getUploadURL().toString());
                        LOG.info("file: " + attachment.getUri() + " uploaded! Available in URL: " + uploader.getUploadURL().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
                        ((PrivateGroup) group).getId(),
                        ((PrivateGroup) group).getPassword(),
                        groupMessage
                );
            } else if (group instanceof Forum)
                communicationManager.sendGroupMessage(
                        ((Forum) group).getId(),
                        ((Forum) group).getPassword(),
                        groupMessage
                );
        });
    }

    private void sendMessage(String protocol, ContactId contactId, Message message) {
        ioExecutor.execute(() -> {
            if (message.getMessageType().equals(Message.Type.IMAGES) || message.getMessageType().equals(Message.Type.ATTACHMENT)) {
                for (Attachment attachment : message.getAttachments()) {
                    try {
                        TusUpload upload = new TusAndroidUpload(Uri.parse(attachment.getUri()), app);

                        TusUploader uploader = tusClient.resumeOrCreateUpload(upload);
                        long totalBytes = upload.getSize();
                        long uploadedBytes = uploader.getOffset();

                        // Upload file in 1MiB chunks
                        uploader.setChunkSize(1024 * 1024);

                        while (uploader.uploadChunk() > 0) {
                            uploadedBytes = uploader.getOffset();
                        }

                        uploader.finish();
                        attachment.setUrl(uploader.getUploadURL().toString());
                        LOG.info("file: " + attachment.getUri() + " uploaded! Available in URL: " + uploader.getUploadURL().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
            try {
                communicationManager.sendDirectMessage(
                        protocol,
                        contactId,
                        message
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
