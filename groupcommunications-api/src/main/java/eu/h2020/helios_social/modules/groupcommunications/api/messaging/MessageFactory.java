package eu.h2020.helios_social.modules.groupcommunications.api.messaging;

/**
 * A class for creating new Messages
 */
public interface MessageFactory {

	/**
	 * Create a new private message given the Group Identifier the message is shared to and the
	 * message body
	 *
	 * @param groupId
	 * @param messageBody
	 * @return
	 */
	Message createMessage(String groupId, String messageBody);

	/**
	 * Create a new private message given the Group Identifier the message is shared to, the
	 * message body and the media file name to be shared
	 *
	 * @param groupId
	 * @param messageBody
	 * @return
	 */
	Message createMessage(String groupId, String mediaFileName,
			String messageBody);

	/**
	 * Create a new private message to initiate a video call with contact assigned to group
	 *
	 * @param groupId
	 * @return
	 */
	Message createVideoCallMessage(String groupId);

	GroupMessage createGroupMessage(String groupId,
			String messageBody);
}
