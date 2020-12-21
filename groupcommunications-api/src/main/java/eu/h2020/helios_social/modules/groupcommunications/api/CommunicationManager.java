package eu.h2020.helios_social.modules.groupcommunications.api;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.GroupId;

public interface CommunicationManager<T> {

	void sendDirectMessage(String protocolId, ContactId contactId,
			AbstractMessage message)
			throws InterruptedException, ExecutionException, TimeoutException;

	void sendDirectMessage(String protocolId, PeerId peerId,
			AbstractMessage message)
			throws InterruptedException, ExecutionException, TimeoutException;

	void sendGroupMessage(String groupId, String password,
			AbstractMessage message);

	void registerReceiver(String protocolId, T receiver);

	void announceTag(String tag);

	void observeTag(String tag);

	void unannounceTag(String tag);

	void unobserveTag(String tag);

	void subscribe(String groupId, String password);

	void unsubscribe(String groupId, String password);
}
