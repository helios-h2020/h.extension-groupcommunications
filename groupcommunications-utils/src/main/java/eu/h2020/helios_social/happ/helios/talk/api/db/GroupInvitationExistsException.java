package eu.h2020.helios_social.happ.helios.talk.api.db;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;

/**
 * Thrown when a duplicate pending context invite is added to the database. This
 * exception may occur due to concurrent updates and does not indicate a
 * database error.
 */
public class GroupInvitationExistsException extends DbException {

	private final GroupInvitation groupInvite;

	public GroupInvitationExistsException(GroupInvitation groupInvite) {
		this.groupInvite = groupInvite;
	}

	public GroupInvitation getContextInvite() {
		return groupInvite;
	}
}
