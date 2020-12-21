package eu.h2020.helios_social.modules.groupcommunications.api.conversation;

import java.util.Collection;
import java.util.Set;

import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupMessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Favourite;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.GroupCount;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;

/**
 * Interface for Conversation Manager
 */
public interface ConversationManager {

	Collection<MessageHeader> getMessageHeaders(String groupId)
			throws DbException;

	Collection<GroupMessageHeader> getGroupMessageHeaders(String groupId)
			throws DbException, FormatException;

	String getMessageText(String messageId)
			throws DbException;

	/**
	 * Returns/creates a group for the private conversation for the given contact
	 * in the given context
	 *
	 * @param contactId
	 * @param contextId
	 * @return
	 */
	Group getContactGroup(ContactId contactId, String contextId)
			throws DbException;

	void addContactGroup(ContactId contactId, Group group)
			throws DbException;

	/**
	 * Returns the group count for the private messages exchanged in the given conversation
	 *
	 * @param groupId conversation Id
	 * @return
	 */
	GroupCount getGroupCount(String groupId) throws DbException;

	Collection<Favourite> getFavourites(String contextId)
			throws DbException, FormatException;

	/**
	 * Returns all the messages in the given private conversation based on groupId
	 *
	 * @param groupId
	 * @return
	 */
	Set<String> getMessageIds(String groupId);

	/**
	 * Returns all conversation {@Link PrivateMassage}s based in groupId
	 *
	 * @param groupId Conversation Identifier
	 * @return
	 */
	Collection<Message> getAllMessages(String groupId);

	void favourite(String messageId) throws DbException;

	void unfavourite(String messageId) throws DbException;

	/**
	 * Deletes all conversation messages
	 *
	 * @param groupId Conversation Identifier
	 */
	void deleteAllMessages(String groupId);

	/**
	 * Deletes the given set of message.
	 *
	 * @param messageIds set of message Identifiers to be deleted
	 */
	void deleteMessages(Collection<String> messageIds);

	void setReadFlag(String groupId, String messageId, boolean read)
			throws DbException;
}
