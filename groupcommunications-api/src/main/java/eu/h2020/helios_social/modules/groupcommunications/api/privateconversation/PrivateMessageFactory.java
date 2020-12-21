package eu.h2020.helios_social.modules.groupcommunications.api.privateconversation;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;

public interface PrivateMessageFactory {

	Message createTextMessage(String groupId, long timestamp,
			String text);

	Message createVideoCallMessage(String groupId, long timestamp,
			String room_id);
}
