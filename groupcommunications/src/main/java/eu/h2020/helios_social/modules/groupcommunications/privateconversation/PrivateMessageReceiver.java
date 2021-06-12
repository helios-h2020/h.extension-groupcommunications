package eu.h2020.helios_social.modules.groupcommunications.privateconversation;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosMessagingReceiver;
import eu.h2020.helios_social.core.messaging_nodejslibp2p.HeliosNetworkAddress;
import eu.h2020.helios_social.modules.groupcommunications.api.attachment.AttachmentManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.AckMessageEvent;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.mining.MiningManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.MessageSentEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.PrivateMessageReceivedEvent;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;
import eu.h2020.helios_social.modules.socialgraphmining.SwitchableMiner;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.ATTACHMENTS;
import static java.util.logging.Logger.getLogger;

public class PrivateMessageReceiver
        implements HeliosMessagingReceiver {
    private static final Logger LOG =
            getLogger(PrivateMessageReceiver.class.getName());

    private final DatabaseComponent db;
    private final ContextualEgoNetwork egoNetwork;
    private final SwitchableMiner switchableMiner;
    private final MessageTracker messageTracker;
    private final EventBus eventBus;
    private final Encoder encoder;
    private final AttachmentManager attachmentManager;

    @Inject
    public PrivateMessageReceiver(DatabaseComponent db,
                                  ContextualEgoNetwork egoNetwork, SwitchableMiner switchableMiner,
                                  MessageTracker messageTracker, Encoder encoder,
                                  AttachmentManager attachmentManager,
                                  EventBus eventBus) {

        this.db = db;
        this.egoNetwork = egoNetwork;
        this.switchableMiner = switchableMiner;
        this.messageTracker = messageTracker;
        this.encoder = encoder;
        this.attachmentManager = attachmentManager;
        this.eventBus = eventBus;
    }

    @Override
    public void receiveMessage(
            @NotNull HeliosNetworkAddress heliosNetworkAddress,
            @NotNull String protocolId,
            @NotNull FileDescriptor fileDescriptor) {
        if (!protocolId.equals(PRIVATE_MESSAGE_PROTOCOL)) return;
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        try (FileInputStream fileInputStream = new FileInputStream(
                fileDescriptor)) {
            int byteRead;
            while ((byteRead = fileInputStream.read()) != -1) {
                ba.write(byteRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        receiveMessage(heliosNetworkAddress, protocolId, ba.toByteArray());
    }

    @Override
    public void receiveMessage(
            @NotNull HeliosNetworkAddress heliosNetworkAddress,
            @NotNull String protocolId, @NotNull byte[] data) {
        if (!protocolId.equals(PRIVATE_MESSAGE_PROTOCOL)) return;
        String stringMessage = new String(data, StandardCharsets.UTF_8);
        Message privateMessage =
                new Gson().fromJson(stringMessage, Message.class);
        try {
            ContactId contactId =
                    new ContactId(heliosNetworkAddress.getNetworkId());
            if (privateMessage.getMessageType().equals(Message.Type.ACK)) {
                LOG.info("Ack Received...");
                onReceiveAckMessage(contactId, privateMessage);
            } else {
                onReceivePrivateMessage(contactId, privateMessage);
            }
        } catch (DbException e) {
            e.printStackTrace();
        }

    }

    private void onReceivePrivateMessage(ContactId contactId,
                                         Message privateMessage)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            if (!db.containsMessage(txn, privateMessage.getId())) {
                LOG.info("private message received...");
                String contextId =
                        db.getGroupContext(txn, privateMessage.getGroupId());
                db.addMessage(txn, privateMessage, MessageState.DELIVERED,
                              contextId, true);
                MessageHeader messageHeader = new MessageHeader(
                        privateMessage.getId(),
                        privateMessage.getGroupId(),
                        privateMessage.getTimestamp(),
                        MessageState.DELIVERED,
                        true,
                        false,
                        privateMessage.getMessageType(),
                        privateMessage.getMessageBody() != null);

                messageTracker.trackIncomingMessage(txn, privateMessage);

                LOG.info("received ack preferences: " + privateMessage.getPreferences());
                if (privateMessage.getPreferences() != null)
                    ackMessage(txn, contactId, privateMessage);
                db.commitTransaction(txn);

                if (messageHeader.getMessageType() == Message.Type.IMAGES || messageHeader.getMessageType() == Message.Type.FILE_ATTACHMENT) {
                    attachmentManager.downloadAttachments(privateMessage.getId(), privateMessage.getAttachments());
                    addAttachmentMetadata(privateMessage.getId(), privateMessage.getAttachments());
                } else {
                    eventBus.broadcast(
                            new PrivateMessageReceivedEvent(messageHeader, contactId));
                }
            }
        } finally {
            db.endTransaction(txn);
        }
    }

    private void onReceiveAckMessage(ContactId contactId, Message ack) throws DbException {
        String[] fields = ack.getMessageBody().split("%");
        ArrayList<Interaction> interactions = egoNetwork
                .getOrCreateContext(fields[0] + "%" + fields[1])
                .getOrAddEdge(
                        egoNetwork.getEgo(),
                        egoNetwork.getOrCreateNode(contactId.getId(), null))
                .getInteractions();
        Interaction interaction = interactions.get(interactions.size() - 1);
        LOG.info("ack preferences: " + ack.getPreferences());
        switchableMiner.newInteraction(interaction, ack.getPreferences(),
                                       SocialGraphMiner.InteractionType.RECEIVE_REPLY);
        messageTracker.setDeliveredFlag(fields[2]);
        eventBus.broadcast(new MessageSentEvent(fields[2]));

    }

    private void ackMessage(Transaction txn, ContactId contactId, Message privateMessage) throws DbException {
        Group group = db.getGroup(txn, privateMessage.getGroupId());
        DBContext context = db.getContext(txn, group.getContextId());

        Interaction interaction = egoNetwork
                .getOrCreateContext(context.getName() + "%" + context.getId())
                .getOrAddEdge(
                        egoNetwork.getEgo(),
                        egoNetwork.getOrCreateNode(contactId.getId(), null)
                ).addDetectedInteraction(null);

        switchableMiner.newInteraction(
                interaction,
                privateMessage.getPreferences(),
                SocialGraphMiner.InteractionType.RECEIVE
        );
        String ack_preferences = switchableMiner.getModelParameters(interaction);
        LOG.info("send ack_preferences: " + ack_preferences);
        Message ack = new Message(ack_preferences, context.getName() + "%" + context.getId() + "%" + privateMessage.getId());
        eventBus.broadcast(new AckMessageEvent(contactId, ack));
    }

    private void addAttachmentMetadata(String messageId,
                                       List<Attachment> attachments) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            BdfDictionary meta = new BdfDictionary();
            BdfList attachmentList = new BdfList();
            for (Attachment a : attachments) {
                attachmentList.add(BdfList.of(a.getUri(), a.getUrl(), a.getContentType(), a.getAttachmentName()));
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
}
