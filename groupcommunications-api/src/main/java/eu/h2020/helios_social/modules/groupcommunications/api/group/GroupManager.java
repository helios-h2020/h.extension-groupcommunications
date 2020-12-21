package eu.h2020.helios_social.modules.groupcommunications.api.group;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.Pair;

public interface GroupManager<T> {

	void addGroup(Group group)
			throws FormatException, DbException;

	void addGroup(T txn, Group group)
			throws FormatException, DbException;

	Group getGroup(String groupId, GroupType groupType)
			throws FormatException, DbException;

	Collection<Group> getGroups(String contextId)
			throws DbException, FormatException;

	Collection<Group> getGroups(T txn)
			throws DbException, FormatException;

	Pair<String, String> getFakeIdentity(String groupId)
			throws DbException, FormatException;

	Pair<String, String> getFakeIdentity(T txn, String groupId)
			throws DbException, FormatException;

	void addGroupInvitation(T txn, GroupInvitation groupInvitation)
			throws DbException;

	void addGroupInvitation(GroupInvitation groupInvitation)
			throws DbException;

	void removeGroupInvitation(ContactId contactId, String groupId)
			throws DbException;

	void removeGroupInvitation(T txn,
			ContactId contactId, String groupId)
			throws DbException;

	Collection<GroupInvitation> getGroupInvitations()
			throws DbException;

	boolean isInvitationAllowed(String groupId, GroupType groupType)
			throws FormatException, DbException;
}
