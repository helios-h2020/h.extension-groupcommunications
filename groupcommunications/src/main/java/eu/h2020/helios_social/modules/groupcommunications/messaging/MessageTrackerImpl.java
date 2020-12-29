package eu.h2020.helios_social.modules.groupcommunications.messaging;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.data.Parser;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupCount;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;

import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTrackerConstants.GROUP_KEY_LATEST_MSG;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTrackerConstants.GROUP_KEY_MSG_COUNT;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTrackerConstants.GROUP_KEY_UNREAD_COUNT;

public class MessageTrackerImpl implements MessageTracker<Transaction> {

	private final DatabaseComponent db;
	private final Clock clock;
	private final Encoder encoder;
	private final Parser parser;

	@Inject
	MessageTrackerImpl(DatabaseComponent db, Clock clock,
			Encoder encoder, Parser parser) {
		this.db = db;
		this.clock = clock;
		this.encoder = encoder;
		this.parser = parser;
	}

	@Override
	public void initializeGroupCount(Transaction txn, String groupId)
			throws DbException {
		long now = clock.currentTimeMillis();
		GroupCount groupCount = new GroupCount(0, 0, now);
		storeGroupCount(txn, groupId, groupCount);
	}

	@Override
	public GroupCount getGroupCount(String groupId) throws DbException {
		GroupCount count;
		Transaction txn = db.startTransaction(true);
		try {
			count = getGroupCount(txn, groupId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return count;
	}

	@Override
	public GroupCount getGroupCount(Transaction txn, String groupId)
			throws DbException {
		try {
			Metadata metadata = db.getGroupMetadata(txn, groupId);
			BdfDictionary d = parser.parseMetadata(metadata);
			return new GroupCount(
					d.getLong(GROUP_KEY_MSG_COUNT, 0L).intValue(),
					d.getLong(GROUP_KEY_UNREAD_COUNT, 0L).intValue(),
					d.getLong(GROUP_KEY_LATEST_MSG, 0L)
			);
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}

	@Override
	public void trackIncomingMessage(Transaction txn, Message message)
			throws DbException {
		trackMessage(txn, message.getGroupId(), message.getTimestamp(), false);
	}

	@Override
	public void trackOutgoingMessage(Transaction txn, Message message)
			throws DbException {
		trackMessage(txn, message.getGroupId(), message.getTimestamp(), true);
	}

	@Override
	public void trackMessage(Transaction txn, String groupId, long timestamp,
			boolean read) throws DbException {
		GroupCount c = getGroupCount(txn, groupId);
		int msgCount = c.getMsgCount() + 1;
		int unreadCount = c.getUnreadCount() + (read ? 0 : 1);
		long latestMsgTime = Math.max(c.getLatestMsgTime(), timestamp);
		storeGroupCount(txn, groupId, new GroupCount(msgCount, unreadCount,
				latestMsgTime));
	}

	@Override
	public void setReadFlag(String groupId, String messageId)
			throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			// if status changed
			if (!db.getMessageState(txn, messageId).equals(MessageState.SEEN)) {
				db.setMessageState(txn, messageId, MessageState.SEEN);
				// update unread counter in group metadata
				GroupCount c = getGroupCount(txn, groupId);
				int unreadCount = c.getUnreadCount() - 1;
				if (unreadCount < 0) throw new DbException();
				storeGroupCount(txn, groupId, new GroupCount(c.getMsgCount(),
						unreadCount, c.getLatestMsgTime()));
			}
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void resetGroupCount(Transaction txn, String groupId,
			int msgCount,
			int unreadCount) throws DbException {
		//TODO
	}

	private void storeGroupCount(Transaction txn, String
			groupId, GroupCount c)
			throws DbException {
		try {
			BdfDictionary d = BdfDictionary.of(
					new BdfEntry(GROUP_KEY_MSG_COUNT, c.getMsgCount()),
					new BdfEntry(GROUP_KEY_UNREAD_COUNT,
							c.getUnreadCount()),
					new BdfEntry(GROUP_KEY_LATEST_MSG, c.getLatestMsgTime())
			);
			db.mergeGroupMetadata(txn, groupId, encoder.encodeMetadata(d));
		} catch (FormatException e) {
			throw new DbException(e);
		}
	}
}
