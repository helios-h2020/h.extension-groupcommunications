package eu.h2020.helios_social.modules.groupcommunications.messaging;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import eu.h2020.helios_social.core.contextualegonetwork.ContextualEgoNetwork;
import eu.h2020.helios_social.core.contextualegonetwork.Interaction;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfDictionary;
import eu.h2020.helios_social.happ.helios.talk.api.data.BdfEntry;
import eu.h2020.helios_social.happ.helios.talk.api.data.Encoder;
import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.happ.helios.talk.api.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications.api.CommunicationManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageTracker;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessagingManager;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;

import static eu.h2020.helios_social.modules.groupcommunications.api.CommunicationConstants.PRIVATE_MESSAGE_PROTOCOL;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ALIAS;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_FUNNY_NAME;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_ID;
import static eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageConstants.PEER_REAL_NAME;

public class MessagingManagerImpl implements MessagingManager {

	private final DatabaseComponent db;
	private final MessageTracker messageTracker;
	private final ContextualEgoNetwork egoNetwork;
	private final CommunicationManager communicationManager;
	private final Encoder encoder;

	@Inject
	public MessagingManagerImpl(DatabaseComponent db,
			ContextualEgoNetwork egoNetwork, MessageTracker messageTracker,
			CommunicationManager communicationManager,
			Encoder encoder,
			EventBus eventBus) {
		this.db = db;
		this.messageTracker = messageTracker;
		this.egoNetwork = egoNetwork;
		this.communicationManager = communicationManager;
		this.encoder = encoder;
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
	public GroupMessageHeader sendPrivateGroupMessage(PrivateGroup privateGroup,
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
}
