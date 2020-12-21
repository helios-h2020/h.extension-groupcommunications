package eu.h2020.helios_social.modules.groupcommunications.privategroup;

import com.google.gson.Gson;

import javax.inject.Inject;

import eu.h2020.helios_social.core.messaging.HeliosMessage;
import eu.h2020.helios_social.core.messaging.HeliosMessageListener;
import eu.h2020.helios_social.core.messaging.HeliosTopic;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.messaging.event.GroupMessageReceivedEvent;

import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FAKE_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;

public class GroupMessageListener implements HeliosMessageListener {

	private final DatabaseComponent db;
	private final MessageTracker messageTracker;
	private final EventBus eventBus;
	private final Encoder encoder;

	@Inject
	public GroupMessageListener(DatabaseComponent db,
			MessageTracker messageTracker, Encoder encoder,
			EventBus eventBus) {
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
					groupMessage.getPeerInfo());
			messageTracker.trackIncomingMessage(txn, groupMessage);
			addMessageMetadata(txn, groupMessage.getId(),
					groupMessage.getPeerInfo());
			eventBus.broadcast(
					new GroupMessageReceivedEvent(messageHeader));
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
