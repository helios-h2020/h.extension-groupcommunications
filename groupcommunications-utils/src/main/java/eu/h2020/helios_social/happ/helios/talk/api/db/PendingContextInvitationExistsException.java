package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

/**
 * Thrown when a duplicate pending context invite is added to the database. This
 * exception may occur due to concurrent updates and does not indicate a
 * database error.
 */
public class PendingContextInvitationExistsException extends DbException {

	private final ContextInvitation contextInvite;

	public PendingContextInvitationExistsException(ContextInvitation contextInvite) {
		this.contextInvite = contextInvite;
	}

	public ContextInvitation getContextInvitation() {
		return contextInvite;
	}
}
