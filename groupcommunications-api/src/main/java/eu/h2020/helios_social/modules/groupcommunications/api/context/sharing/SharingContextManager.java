package eu.h2020.helios_social.modules.groupcommunications.api.context.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

public interface SharingContextManager {

	/**
	 * Invite contact to Context
	 *
	 * @param contextInvitation
	 */
	public void sendContextInvitation(ContextInvitation contextInvitation)
			throws DbException;

	/**
	 * Accept Context Invite
	 *
	 * @param pendingContextId
	 */
	public void acceptContextInvitation(String pendingContextId)
			throws DbException;

	void acceptContextInvitation(ContextInvitation contextInvitation)
			throws DbException;

	/**
	 * Reject Context Invite
	 *
	 * @param contextInvitation
	 */
	public void rejectContextInvitation(ContextInvitation contextInvitation)
			throws DbException;
}
