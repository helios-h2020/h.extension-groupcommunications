package eu.h2020.helios_social.modules.groupcommunications.privateconversation;

import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.privateconversation.PrivateMessageFactory;

public class PrivateMessageFactoryImpl
		implements PrivateMessageFactory {

	@Inject
	public PrivateMessageFactoryImpl() {

	}

	@Override
	public Message createTextMessage(String groupId, long timestamp,
			String text) {
		return new Message(UUID.randomUUID().toString(), groupId, timestamp,
				text,
				Message.Type.TEXT);
	}

	@Override
	public Message createVideoCallMessage(String groupId, long timestamp,
			String room_id) {
		return new Message(UUID.randomUUID().toString(), groupId, timestamp,
				room_id, Message.Type.VIDEOCALL);
	}
}
