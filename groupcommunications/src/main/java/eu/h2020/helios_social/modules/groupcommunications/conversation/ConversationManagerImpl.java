package eu.h2020.helios_social.modules.groupcommunications.conversation;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Attachment;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfDictionary;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.BdfList;
import eu.h2020.helios_social.modules.groupcommunications_utils.data.Parser;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Metadata;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.conversation.ConversationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Favourite;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupCount;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;

import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.ATTACHMENTS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_REAL_NAME;

public class ConversationManagerImpl implements
        ConversationManager<Transaction> {
    public static final String TAG = ConversationManager.class.getName();
    private static final Logger LOG = Logger.getLogger(TAG);

    private final DatabaseComponent db;
    private final MessageTracker messageTracker;
    private final ContactManager contactManager;
    private final Parser parser;

    @Inject
    public ConversationManagerImpl(DatabaseComponent db,
                                   ContactManager contactManager,
                                   MessageTracker messageTracker, Parser parser) {
        this.db = db;
        this.messageTracker = messageTracker;
        this.parser = parser;
        this.contactManager = contactManager;
    }

    @Override
    public Collection<MessageHeader> getMessageHeaders(String groupId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Collection<MessageHeader> messageHeaders;
        try {
            messageHeaders = db.getMessageHeaders(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageHeaders;
    }

    @Override
    public MessageHeader getMessageHeader(String messageId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        MessageHeader messageHeader;
        try {
            messageHeader = db.getMessageHeader(txn, messageId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageHeader;
    }

    @Override
    public GroupMessageHeader getGroupMessageHeader(String messageId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        GroupMessageHeader groupMessageHeader;
        try {
            groupMessageHeader = getGroupMessageHeader(txn, messageId);

            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return groupMessageHeader;
    }

    @Override
    public MessageHeader getMessageHeader(Transaction txn, String messageId) throws DbException {
        return db.getMessageHeader(txn, messageId);
    }

    @Override
    public GroupMessageHeader getGroupMessageHeader(Transaction txn, String messageId) throws DbException, FormatException {
        MessageHeader messageHeader = db.getMessageHeader(txn, messageId);
        BdfDictionary meta = parser.parseMetadata(db.getMessageMetadata(txn, messageId));
        return new GroupMessageHeader(messageHeader, getPeerInfo(meta));
    }

    @Override
    public Collection<GroupMessageHeader> getGroupMessageHeaders(String groupId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<GroupMessageHeader> messageHeaders;
        try {
            messageHeaders = getGroupMessageHeaders(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageHeaders;
    }

    @Override
    public String getMessageText(String messageId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        String messageText;
        try {
            messageText = db.getMessageText(txn, messageId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return messageText;
    }

    @Override
    public List<Attachment> getAttachments(String messageId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        List<Attachment> attachments = new ArrayList<>();
        try {
            Metadata metadata = db.getMessageMetadata(txn, messageId);
            BdfDictionary meta = parser.parseMetadata(metadata);
            BdfList attachmentList = meta.getList(ATTACHMENTS, new BdfList());
            for (int i = 0; i < attachmentList.size(); i++) {
                BdfList a = attachmentList.getList(i);
                attachments.add(new Attachment(a.getOptionalString(0), a.getOptionalString(1), a.getOptionalString(2), a.getString(3, null)));
            }
            db.commitTransaction(txn);
        } catch (FormatException e) {
            throw new DbException(e);
        } finally {
            db.endTransaction(txn);
        }
        return attachments;
    }

    @Override
    public Group getContactGroup(ContactId contactId, String contextId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Group group;
        try {
            group = db.getContactGroup(txn, contactId, contextId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return group;
    }

    @Override
    public Group getContactGroup(String groupId)
            throws DbException {
        Transaction txn = db.startTransaction(true);
        Group group;
        try {
            group = db.getGroup(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return group;
    }

    @Override
    public ContactId getContactIdByGroupId(String groupId) throws DbException {
        Transaction txn = db.startTransaction(true);
        ContactId contactId;
        try {
            contactId = db.getContactIdByGroupId(txn, groupId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return contactId;
    }

    @Override
    public ContactId getContactIdByGroupId(Transaction txn, String groupId) throws DbException {
        return db.getContactIdByGroupId(txn, groupId);
    }

    @Override
    public void addContactGroup(ContactId contactId, Group group)
            throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.addContactGroup(txn, group, contactId);
            messageTracker.initializeGroupCount(txn, group.getId());
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public GroupCount getGroupCount(String groupId) throws DbException {
        return messageTracker.getGroupCount(groupId);
    }

    @Override
    public Collection<Favourite> getFavourites(String contextId)
            throws DbException, FormatException {
        Transaction txn = db.startTransaction(true);
        Collection<Message> favouriteMsgs;
        Collection<Favourite> favourites = new ArrayList<>();
        try {
            favouriteMsgs = db.getFavourites(txn, contextId);
            for (Message message : favouriteMsgs) {
                Metadata metadata = db.getMessageMetadata(txn, message.getId());

                if (metadata != null && metadata.size() > 0) {
                    BdfDictionary meta = parser.parseMetadata(metadata);
                    PeerInfo peerInfo = getPeerInfo(meta);
                    if (peerInfo.getAlias() != null && !peerInfo.getAlias().equals(""))
                        favourites.add(new Favourite(message.getId(),
                                                     peerInfo.getAlias(), message.getMessageBody(), message.getMessageType(),
                                                     message.getTimestamp()));
                    else if (peerInfo.getFunnyName() != null && !peerInfo.getFunnyName().equals(""))
                        favourites.add(new Favourite(message.getId(),
                                                     peerInfo.getFunnyName(), message.getMessageBody(), message.getMessageType(),
                                                     message.getTimestamp()));
                    else {
                        Contact contact =
                                db.getContact(txn, db.getContactIdByGroupId(txn,
                                                                            message.getGroupId()));
                        favourites.add(new Favourite(message.getId(),
                                                     contact.getAlias(), message.getMessageBody(), message.getMessageType(),
                                                     message.getTimestamp()));
                    }
                } else {
                    Contact contact =
                            db.getContact(txn, db.getContactIdByGroupId(txn,
                                                                        message.getGroupId()));
                    favourites.add(new Favourite(message.getId(),
                                                 contact.getAlias(), message.getMessageBody(), message.getMessageType(),
                                                 message.getTimestamp()));
                }

            }
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
        return favourites;
    }

    @Override
    public Set<String> getMessageIds(String groupId) {
        return null;
    }

    @Override
    public Collection<Message> getAllMessages(String groupId) {
        return null;
    }

    @Override
    public void favourite(String messageId) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.addToFavourites(txn, messageId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void unfavourite(String messageId) throws DbException {
        Transaction txn = db.startTransaction(false);
        try {
            db.removeFromFavourites(txn, messageId);
            db.commitTransaction(txn);
        } finally {
            db.endTransaction(txn);
        }
    }

    @Override
    public void deleteAllMessages(String groupId) {

    }

    @Override
    public void deleteMessages(Collection<String> messageIds) {

    }

    @Override
    public void setReadFlag(String groupId, String messageId, boolean read)
            throws DbException {
        messageTracker.setReadFlag(groupId, messageId);
    }

    private Collection<GroupMessageHeader> getGroupMessageHeaders(
            Transaction txn,
            String groupId)
            throws DbException, FormatException {
        Collection<MessageHeader> messageHeaders =
                db.getMessageHeaders(txn, groupId);
        Map<String, Metadata> messageMetadata =
                db.getMessageMetadataByGroupId(txn, groupId);
        Collection<GroupMessageHeader> groupMessageHeaders = new ArrayList();

        for (MessageHeader messageHeader : messageHeaders) {
            BdfDictionary meta = parser.parseMetadata(
                    messageMetadata.get(messageHeader.getMessageId())
            );
            groupMessageHeaders.add(
                    new GroupMessageHeader(messageHeader, getPeerInfo(meta))
            );
        }
        return groupMessageHeaders;
    }

    private PeerInfo getPeerInfo(BdfDictionary meta) throws FormatException {
        return new PeerInfo.Builder()
                .peerId(new PeerId(meta.getOptionalString(PEER_ID),
                                   meta.getOptionalString(PEER_FAKE_ID)))
                .alias(meta.getOptionalString(PEER_ALIAS))
                .real_name(meta.getOptionalString(PEER_REAL_NAME))
                .funny_name(meta.getOptionalString(PEER_FUNNY_NAME))
                .build();

    }
}
