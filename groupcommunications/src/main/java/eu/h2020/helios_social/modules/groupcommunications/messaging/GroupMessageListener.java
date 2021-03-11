package eu.h2020.helios_social.modules.groupcommunications.messaging;

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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosMessageListener;
import eu.h2020.helios_social.core.messaging.HeliosTopic;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.messaging.event.PrivateMessageReceivedEvent;
import eu.h2020.helios_social.modules.groupcommunications.privateconversation.PrivateMessageReceiver;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfEntry;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Encoder;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.messaging.event.GroupMessageReceivedEvent;

import static android.content.Context.DOWNLOAD_SERVICE;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.ATTACHMENTS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;
import static java.util.logging.Logger.getLogger;

public class GroupMessageListener implements HeliosMessageListener {
    private static final Logger LOG =
            getLogger(PrivateMessageReceiver.class.getName());

    private final DatabaseComponent db;
    private final MessageTracker messageTracker;
    private final EventBus eventBus;
    private final Encoder encoder;
    private final Application app;

    @Inject
    public GroupMessageListener(Application app, DatabaseComponent db,
                                MessageTracker messageTracker, Encoder encoder,
                                EventBus eventBus) {
        this.app = app;
        this.db = db;
        this.messageTracker = messageTracker;
        this.encoder = encoder;
        this.eventBus = eventBus;
    }

    @Override
    public void showMessage(HeliosTopic heliosTopic,
                            HeliosMessage heliosMessage) {
        GroupMessage groupMessage =
                new Gson().fromJson(heliosMessage.getMessage(),
                        GroupMessage.class);
        try {
            onReceiveGroupMessage(groupMessage);
        } catch (DbException e) {
            e.printStackTrace();
        }
    }

    private void onReceiveGroupMessage(GroupMessage groupMessage)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            if (!db.containsMessage(txn, groupMessage.getId())) {
                String contextId =
                        db.getGroupContext(txn, groupMessage.getGroupId());
                db.addMessage(txn, groupMessage, MessageState.DELIVERED,
                        contextId, true);
                GroupMessageHeader messageHeader = new GroupMessageHeader(
                        groupMessage.getId(),
                        groupMessage.getGroupId(),
                        groupMessage.getTimestamp(),
                        MessageState.DELIVERED,
                        true,
                        false,
                        groupMessage.getMessageType(),
                        groupMessage.getMessageBody() != null,
                        groupMessage.getPeerInfo());
                messageTracker.trackIncomingMessage(txn, groupMessage);
                addMessageMetadata(txn, groupMessage.getId(),
                        groupMessage.getPeerInfo());

                db.commitTransaction(txn);

                MimeTypeMap mime = MimeTypeMap.getSingleton();
                DownloadManager downloadmanager = (DownloadManager) app.getSystemService(DOWNLOAD_SERVICE);
                List<Long> ids = new ArrayList<>();
                if (messageHeader.getMessageType() == Message.Type.IMAGES) {
                    for (Attachment a : groupMessage.getAttachments()) {
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
                            groupMessage.getAttachments().get(ids.indexOf(downloadId)).setUri(downloadmanager.getUriForDownloadedFile(downloadId).toString());
                            DownloadManager.Query query = new DownloadManager.Query();
                            query.setFilterByStatus(DownloadManager.STATUS_PAUSED |
                                    DownloadManager.STATUS_PENDING |
                                    DownloadManager.STATUS_RUNNING);
                            Cursor cursor = downloadmanager.query(query);
                            if (cursor != null && cursor.getCount() > 0) {
                                return;
                            } else {
                                try {
                                    addAttachmentMetadata(groupMessage.getId(), groupMessage.getAttachments());
                                } catch (DbException e) {
                                    e.printStackTrace();
                                }
                                eventBus.broadcast(
                                        new GroupMessageReceivedEvent(messageHeader));
                            }
                        }
                    };
                    app.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                } else {
                    eventBus.broadcast(
                            new GroupMessageReceivedEvent(messageHeader));
                }
            }
        } catch (FormatException e) {
            e.printStackTrace();
        } finally {
            db.endTransaction(txn);
        }

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

    private void addMessageMetadata(Transaction txn, String messageId,
                                    PeerInfo peerInfo) throws FormatException, DbException {
        BdfDictionary meta = BdfDictionary.of(
                new BdfEntry(PEER_FAKE_ID, peerInfo.getPeerId().getFakeId())
        );
        if (peerInfo.getFunnyName() != null) {
            meta.put(PEER_FUNNY_NAME, peerInfo.getFunnyName());
        } else {
            meta.put(PEER_ID, peerInfo.getPeerId().getId());
            meta.put(PEER_ALIAS, peerInfo.getAlias());
        }
        db.mergeMessageMetadata(txn, messageId, encoder.encodeMetadata(meta));
    }
}
