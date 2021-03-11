package eu.h2020.helios_social.modules.groupcommunications.privateconversation;

import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.webkit.MimeTypeMap;

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
import eu.h2020.helios_social.modules.groupcommunications.messaging.event.PrivateMessageReceivedEvent;
import eu.h2020.helios_social.modules.socialgraphmining.SocialGraphMiner;

import static android.content.Context.DOWNLOAD_SERVICE;
import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.ATTACHMENTS;
import static java.util.logging.Logger.getLogger;

public class PrivateMessageReceiver
        implements HeliosMessagingReceiver {
    private static final Logger LOG =
            getLogger(PrivateMessageReceiver.class.getName());

    private final DatabaseComponent db;
    private final ContextualEgoNetwork egoNetwork;
    private final MiningManager miningManager;
    private final MessageTracker messageTracker;
    private final EventBus eventBus;
    private final Application app;
    private final Encoder encoder;

    @Inject
    public PrivateMessageReceiver(Application app, DatabaseComponent db,
                                  ContextualEgoNetwork egoNetwork, MiningManager miningManager,
                                  MessageTracker messageTracker, Encoder encoder, EventBus eventBus) {
        this.app = app;
        this.db = db;
        this.egoNetwork = egoNetwork;
        this.miningManager = miningManager;
        this.messageTracker = messageTracker;
        this.encoder = encoder;
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


            MimeTypeMap mime = MimeTypeMap.getSingleton();
            DownloadManager downloadmanager = (DownloadManager) app.getSystemService(DOWNLOAD_SERVICE);
            List<Long> ids = new ArrayList<>();
            if (messageHeader.getMessageType() == Message.Type.IMAGES) {
                for (Attachment a : privateMessage.getAttachments()) {
                    String path = "/" + a.getUrl().replaceAll(".*/", "") + "." + mime.getExtensionFromMimeType(a.getContentType());
                    Uri uri = Uri.parse(a.getUrl());
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setMimeType(a.getContentType());
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

                    LOG.info("Downloading attachment started! " + path);

                    request.setDestinationInExternalFilesDir(app.getApplicationContext(), "/data", path);

                    ids.add(downloadmanager.enqueue(request));
                }


                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context arg0, Intent intent) {

                        Long downloadId = intent.getLongExtra(
                                DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                        LOG.info("Download completed to id " + downloadId);
                        privateMessage.getAttachments().get(ids.indexOf(downloadId)).setUri(downloadmanager.getUriForDownloadedFile(downloadId).toString());
                        DownloadManager.Query query = new DownloadManager.Query();
                        query.setFilterByStatus(DownloadManager.STATUS_PAUSED |
                                DownloadManager.STATUS_PENDING |
                                DownloadManager.STATUS_RUNNING);
                        Cursor cursor = downloadmanager.query(query);
                        if (cursor != null && cursor.getCount() > 0) {
                            return;
                        } else {
                            try {
                                addAttachmentMetadata(privateMessage.getId(), privateMessage.getAttachments());
                            } catch (DbException e) {
                                e.printStackTrace();
                            }
                            eventBus.broadcast(
                                    new PrivateMessageReceivedEvent(messageHeader, contactId));
                        }
                    }
                };
                app.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            } else {
                eventBus.broadcast(
                        new PrivateMessageReceivedEvent(messageHeader, contactId));
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
        miningManager.getSocialGraphMiner().newInteraction(interaction, ack.getPreferences(),
                SocialGraphMiner.InteractionType.RECEIVE_REPLY);
        messageTracker.setDeliveredFlag(fields[2]);

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

        miningManager.getSocialGraphMiner().newInteraction(
                interaction,
                privateMessage.getPreferences(),
                SocialGraphMiner.InteractionType.RECEIVE
        );
        String ack_preferences = miningManager
                .getSocialGraphMiner()
                .getModelParameters(interaction);
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
}
