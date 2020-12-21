package eu.h2020.helios_social.modules.groupcommunications.api.forum;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Datespan;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Daytime;
import eu.h2020.helios_social.modules.groupcommunications.api.context.utils.Season;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.FormatException;

/**
 * Interface for Forum Manager
 */
public interface ForumManager<T> {

	void addForum(Forum forum, ForumType type, boolean isAdministrator)
			throws FormatException, DbException;

	/**
	 * Allows User to add a new general Forum with a given name
	 */
	void addForum(T txn, Forum forum, ForumType type, boolean isAdministrator)
			throws FormatException, DbException;

	Forum getForum(String groupId) throws DbException, FormatException;

	Forum getForum(T txn, String groupId) throws DbException, FormatException;

	/**
	 * @return all forums user is subscribed to
	 */
	Collection<Forum> getForums() throws DbException, FormatException;

	Collection<Forum> getForums(T txn)
			throws DbException, FormatException;

	/**
	 * @param contextId
	 * @return all forums user is subscribed to in the defined context
	 */
	Collection<Forum> getForums(String contextId)
			throws DbException, FormatException;

	Collection<Forum> getForums(T txn, String contextId)
			throws DbException, FormatException;

	/**
	 * Returns all members of the given Forum
	 *
	 * @param groupId
	 * @return s collection of Forum Member
	 */
	Collection<ForumMember> getForumMembers(String groupId)
			throws DbException, FormatException;

	Collection<ForumMember> getForumMembers(T txn,
			String groupId)
			throws DbException, FormatException;

	/**
	 * Returns user role in the given forum
	 *
	 * @param groupId
	 * @return
	 */
	ForumMemberRole getRole(String groupId) throws DbException, FormatException;

	ForumMemberRole getRole(T txn, String groupId)
			throws DbException, FormatException;

	String getFakeId(T txn, String groupId)
			throws DbException, FormatException;

	void updateRole(String groupId,
			ForumMemberRole newRole)
			throws DbException, FormatException;

	void updateRole(T txn, String groupId,
			ForumMemberRole newRole)
			throws DbException, FormatException;

	void updateForumMemberRole(ForumMember forumMember)
			throws DbException;

	void updateForumMemberRole(T txn, ForumMember forumMember)
			throws DbException;

	void addForumMember(ForumMember forumMember)
			throws DbException;

	void addForumMember(T txn, ForumMember forumMember)
			throws DbException;

	void addForumMembers(Collection<ForumMember> forumMembers)
			throws DbException;

	void addForumMember(T txn,
			Collection<ForumMember> forumMembers)
			throws DbException;

	void addModerator(String groupId, String moderator)
			throws DbException, FormatException;

	void addModerator(T txn, String groupId,
			String moderator) throws DbException, FormatException;

	void removeForumMember(ForumMember forumMember)
			throws DbException;

	void removeForumMember(T txn, ForumMember forumMember)
			throws DbException;

	void removeModerator(String groupId, String moderator)
			throws DbException, FormatException;

	void removeModerator(T txn, String groupId,
			String moderator) throws DbException, FormatException;

	void removeForumMemberList(String groupId)
			throws DbException, FormatException;
}
