package eu.h2020.helios_social.modules.groupcommunications.api.messaging;


import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

public interface MessageTracker<T> {

	/**
	 * Initializes the group count with zero messages,
	 * but uses the current time as latest message time for sorting.
	 */
	void initializeGroupCount(T txn, String groupId)
			throws DbException;

	/**
	 * Gets the number of visible and unread messages in the group
	 * as well as the timestamp of the latest message
	 **/
	GroupCount getGroupCount(String groupId) throws DbException;

	/**
	 * Gets the number of visible and unread messages in the group
	 * as well as the timestamp of the latest message
	 **/
	GroupCount getGroupCount(T txn, String groupId) throws
			DbException;

	/**
	 * Updates the group count for the given incoming message.
	 */
	void trackIncomingMessage(T txn, Message message)
			throws DbException;

	/**
	 * Updates the group count for the given outgoing message.
	 */
	void trackOutgoingMessage(T txn, Message message)
			throws DbException;

	/**
	 * Updates the group count for the given message.
	 */
	void trackMessage(T txn, String groupId, long timestamp,
			boolean read)
			throws DbException;

	/**
	 * Marks a message as read or unread and updates the group count.
	 */
	void setReadFlag(String groupId, String messageId)
			throws DbException;

	/**
	 * Resets the {@link GroupCount} to the given msgCount and unreadCount.
	 * The latestMsgTime will be set to the current time.
	 * <p>
	 * Such reset is needed when recalculating the counts
	 * after deleting messages from a group.
	 */
	void resetGroupCount(T txn, String groupId, int msgCount,
			int unreadCount) throws DbException;

}
