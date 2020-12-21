package eu.h2020.helios_social.modules.groupcommunications.context;

import java.util.Collection;

import eu.h2020.helios_social.core.context.Context;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.GeneralContextProxy;
import eu.h2020.helios_social.modules.groupcommunications.context.proxy.LocationContextProxy;

public interface ContextManager<T> {


	void addContext(Context context) throws DbException;

	void addPendingContext(ContextInvitation contextInvitation)
			throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations()
			throws DbException;

	Collection<ContextInvitation> getPendingContextInvitations(T txn)
			throws DbException;

	void removePendingContext(String pendingContextId)
			throws DbException;

	void removeContextInvitation(ContactId contactId,
			String pendingContextId)
			throws DbException;

	boolean contextExists(String contextId) throws DbException;

	void addContext(T txn, GeneralContextProxy context)
			throws DbException;

	void addContext(T txn, LocationContextProxy context)
			throws DbException;

	void removeContext(String contextId) throws DbException;

	Collection<DBContext> getContexts() throws DbException;

	Context getContext(String contextId) throws DbException, FormatException;

	Integer getContextColor(String contextId) throws DbException;

	boolean isMember(T txn, String contextId, ContactId cid)
			throws DbException, FormatException;

	boolean isMember(String contextId, ContactId cid)
			throws DbException, FormatException;

	Collection<ContextInvitation> getOutgoingContextInvitations(String contextId)
			throws DbException;

	boolean belongsToContext(ContactId contactId, String contextId)
			throws DbException;

	/**
	 * Returns some stats (number of members, groups & forums) for the given context
	 */
	ContextCount getStats(String contextId) throws DbException,
			FormatException;

	void addMember(String contextId, ContactId cid)
			throws DbException, FormatException;

	void addMember(T txn, String contextId, ContactId a)
			throws DbException, FormatException;

	Collection<ContactId> getMembers(String contextId)
			throws DbException, FormatException;

	Collection<String> getPrivateGroups(String contextId)
			throws DbException, FormatException;

	Collection<String> getForums(String contextId)
			throws DbException, FormatException;

	void addForum(T txn, String contextId, String forumId)
			throws DbException, FormatException;

	void addForum(String contextId, String forumId)
			throws DbException, FormatException;

	void addPrivateGroup(String contextId, String groupId)
			throws DbException, FormatException;

	void addPrivateGroup(T txn, String contextId, String groupId)
			throws DbException, FormatException;

	Collection<ContactId> getMembers(T txn, String contextId)
			throws DbException, FormatException;

	class ContextCount {

		private final int membersCount, groupsCount, forumsCount;

		public ContextCount(int membersCount, int groupsCount,
				int forumsCount) {
			this.membersCount = membersCount;
			this.groupsCount = groupsCount;
			this.forumsCount = forumsCount;
		}

		public int getMembersCount() {
			return membersCount;
		}

		public int getPrivateGroupsCount() {
			return groupsCount;
		}

		public long getForumsCount() {
			return forumsCount;
		}
	}

}
