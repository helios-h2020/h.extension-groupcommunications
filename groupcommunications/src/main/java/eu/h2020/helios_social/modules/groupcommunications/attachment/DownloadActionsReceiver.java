package eu.h2020.helios_social.modules.groupcommunications.attachment;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.attachment.PendingAttachmentsMemory;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.GroupMessageReceivedEvent;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.PrivateMessageReceivedEvent;


public class DownloadActionsReceiver extends BroadcastReceiver {
    private static Logger LOG = Logger.getLogger(DownloadActionsReceiver.class.getName());

    private final ConcurrentHashMap<Long, String> pendingAttachmentsMemory;
    private final DownloadManager downloadManager;
    private final ConversationManager conversationManager;
    private final EventBus eventBus;
    private final DatabaseComponent db;

    @Inject
    public DownloadActionsReceiver(DatabaseComponent db, DownloadManager downloadManager,
                                   @PendingAttachmentsMemory ConcurrentHashMap<Long, String> pendingAttachmentsMemory,
                                   ConversationManager conversationManager,
                                   EventBus eventBus) {
        this.db = db;
        this.downloadManager = downloadManager;
        this.pendingAttachmentsMemory = pendingAttachmentsMemory;
        this.conversationManager = conversationManager;
        this.eventBus = eventBus;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Long downloadId = intent.getLongExtra(
                DownloadManager.EXTRA_DOWNLOAD_ID, 0);
        LOG.info("Download completed to id " + downloadId);
        String messageId = pendingAttachmentsMemory.get(downloadId);
        pendingAttachmentsMemory.remove(downloadId);
        LOG.info("ATTACHMENT MANAGER : pendingAttachmentsMemory=" + pendingAttachmentsMemory);
        if (!pendingAttachmentsMemory.containsValue(messageId)) {
            try {
                Transaction txn = db.startTransaction(true);
                try {
                    MessageHeader messageHeader = conversationManager.getMessageHeader(txn, messageId);
                    ContactId contactId = conversationManager.getContactIdByGroupId(txn, messageHeader.getGroupId());
                    if (contactId != null)
                        eventBus.broadcast(new PrivateMessageReceivedEvent(
                                messageHeader,
                                contactId
                        ));
                    else {
                        GroupMessageHeader groupMessageHeader = conversationManager.getGroupMessageHeader(txn, messageId);
                        eventBus.broadcast(new GroupMessageReceivedEvent(
                                groupMessageHeader
                        ));
                    }

                } finally {
                    db.endTransaction(txn);
                }
            } catch (DbException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }
    }
}
