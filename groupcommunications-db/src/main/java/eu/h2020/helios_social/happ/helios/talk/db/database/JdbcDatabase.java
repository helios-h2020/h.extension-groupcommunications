package eu.h2020.helios_social.happ.helios.talk.db.database;

import eu.h2020.helios_social.happ.helios.talk.api.crypto.SecretKey;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooNewException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DataTooOldException;
import eu.h2020.helios_social.happ.helios.talk.api.db.DbClosedException;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMember;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumMemberRole;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageHeader;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.MessageState;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.happ.helios.talk.api.db.Metadata;
import eu.h2020.helios_social.happ.helios.talk.api.db.MigrationListener;
import eu.h2020.helios_social.happ.helios.talk.api.identity.Identity;
import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;
import eu.h2020.helios_social.happ.helios.talk.api.settings.Settings;
import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.Contact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContact;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.ContextType;
import eu.h2020.helios_social.modules.groupcommunications.api.context.DBContext;
import eu.h2020.helios_social.modules.groupcommunications.api.messaging.Message;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.Peer;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import static eu.h2020.helios_social.happ.helios.talk.api.db.Metadata.REMOVE;
import static java.sql.Types.BINARY;
import static java.sql.Types.VARCHAR;
import static java.util.Arrays.asList;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseConstants.DB_SETTINGS_NAMESPACE;
import static eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseConstants.LAST_COMPACTED_KEY;
import static eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseConstants.MAX_COMPACTION_INTERVAL_MS;
import static eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseConstants.SCHEMA_VERSION_KEY;
import static eu.h2020.helios_social.happ.helios.talk.db.database.JdbcUtils.tryToClose;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logDuration;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.logException;
import static eu.h2020.helios_social.happ.helios.talk.api.util.LogUtils.now;

/**
 * A generic database implementation that can be used with any JDBC-compatible
 * database library.
 */
@NotNullByDefault
abstract class JdbcDatabase implements Database<Connection> {

	// Package access for testing
	static final int CODE_SCHEMA_VERSION = 1;

	private static final String CREATE_SETTINGS =
			"CREATE TABLE settings"
					+ " (namespace _STRING NOT NULL,"
					+ " settingKey _STRING NOT NULL,"
					+ " value _STRING NOT NULL,"
					+ " PRIMARY KEY (namespace, settingKey))";

	private static final String CREATE_IDENTITIES =
			"CREATE TABLE identities"
					+ " (id _STRING NOT NULL,"
					+ " networkId _STRING,"
					+ " profilePicture BLOB,"
					+ " alias _STRING NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " PRIMARY KEY (id))";

	private static final String CREATE_PROFILES =
			"CREATE TABLE profiles"
					+ " (contextId _STRING NOT NULL,"
					+ " alias _STRING NOT NULL,"
					+ " fullname _STRING,"
					+ " gender INT,"
					+ " country INT,"
					+ " university _STRING,"
					+ " work _STRING,"
					+ " interests _STRING,"
					+ " quote _STRING,"
					+ " profilepic BLOB,"
					+ " PRIMARY KEY (contextId))";

	private static final String CREATE_CONTACTS =
			"CREATE TABLE contacts"
					+ " (contactId _STRING NOT NULL,"
					+ " profilePicture BLOB,"
					+ " alias _STRING NOT NULL,"
					+ " PRIMARY KEY (contactId))";

	private static final String CREATE_PENDING_CONTACTS =
			"CREATE TABLE pendingContacts"
					+ " (pendingContactId _STRING NOT NULL,"
					+ " alias _STRING NOT NULL,"
					+ " message _STRING,"
					+ " type INT NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " PRIMARY KEY (pendingContactId))";

	private static final String CREATE_CONTEXT_INVITES =
			"CREATE TABLE contextInvites"
					+ " (contactId _STRING NOT NULL,"
					+ " pendingContextId _STRING NOT NULL,"
					+ " name _STRING NOT NULL,"
					+ " jsonContext _STRING,"
					+ " type INT NOT NULL,"
					+ " incoming BOOLEAN NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " PRIMARY KEY (contactId, pendingContextId),"
					+ " FOREIGN KEY (contactId)"
					+ " REFERENCES contacts (contactId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_GROUP_INVITES =
			"CREATE TABLE groupInvites"
					+ " (contactId _STRING NOT NULL,"
					+ " contextId _STRING NOT NULL,"
					+ " pendingGroupId _STRING NOT NULL,"
					+ " name _STRING NOT NULL,"
					+ " json _STRING,"
					+ " type INT NOT NULL,"
					+ " incoming BOOLEAN NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " PRIMARY KEY (contactId, pendingGroupId),"
					+ " FOREIGN KEY (contactId)"
					+ " REFERENCES contacts (contactId)"
					+ " ON DELETE CASCADE,"
					+ " FOREIGN KEY (contextId)"
					+ " REFERENCES contexts (contextId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_CONTEXTS =
			"CREATE TABLE contexts"
					+ " (contextId _STRING NOT NULL,"
					+ " name _STRING NOT NULL,"
					+ " color INT NOT NULL,"
					+ " type INT NOT NULL,"
					+ " PRIMARY KEY (contextId))";

	private static final String CREATE_CONTEXTS_METADATA =
			"CREATE TABLE contextMetadata"
					+ " (contextId _STRING NOT NULL,"
					+ " metaKey _STRING NOT NULL,"
					+ " value _BINARY NOT NULL,"
					+ " PRIMARY KEY (contextId, metaKey),"
					+ " FOREIGN KEY (contextId)"
					+ " REFERENCES contexts (contextId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_GROUPS =
			"CREATE TABLE groups"
					+ " (groupId _STRING NOT NULL,"
					+ " contextId _STRING NOT NULL,"
					+ " contactId _STRING,"
					+ " descriptor _BINARY,"
					+ " type INT NOT NULL,"
					+ " PRIMARY KEY (groupId),"
					+ " FOREIGN KEY (contextId)"
					+ " REFERENCES contexts (contextId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_GROUP_METADATA =
			"CREATE TABLE groupMetadata"
					+ " (groupId _STRING NOT NULL,"
					+ " contextId _STRING NOT NULL,"
					+ " metaKey _STRING NOT NULL,"
					+ " value _BINARY NOT NULL,"
					+ " PRIMARY KEY (groupId, metaKey),"
					+ " FOREIGN KEY (groupId)"
					+ " REFERENCES groups (groupId)"
					+ " ON DELETE CASCADE,"
					+ " FOREIGN KEY (contextId)"
					+ " REFERENCES contexts (contextId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_FORUM_MEMBERLIST =
			"CREATE TABLE forumMemberList"
					+ " (groupId _STRING NOT NULL,"
					+ " peerId _STRING NOT NULL,"
					+ " fakeId _STRING,"
					+ " alias _STRING,"
					+ " role INT NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " PRIMARY KEY (groupId, fakeId),"
					+ " FOREIGN KEY (groupId)"
					+ " REFERENCES groups (groupId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_MESSAGES =
			"CREATE TABLE messages"
					+ " (messageId _STRING NOT NULL,"
					+ " groupId _STRING NOT NULL,"
					+ " contextId _STRING NOT NULL,"
					+ " timestamp BIGINT NOT NULL,"
					+ " text _STRING,"
					+ " mediaFileName _STRING,"
					+ " type INT NOT NULL,"
					+ " state INT NOT NULL,"
					+ " incoming BOOLEAN NOT NULL,"
					+ " favourite BOOLEAN NOT NULL,"
					+ " temporary BOOLEAN NOT NULL,"
					+ " PRIMARY KEY (messageId),"
					+ " FOREIGN KEY (groupId)"
					+ " REFERENCES groups (groupId)"
					+ " ON DELETE CASCADE)";

	private static final String CREATE_MESSAGE_METADATA =
			"CREATE TABLE messageMetadata"
					+ " (messageId _STRING NOT NULL,"
					+ " groupId _STRING NOT NULL,"
					+ " metaKey _STRING NOT NULL,"
					+ " value _BINARY NOT NULL,"
					+ " PRIMARY KEY (messageId, metaKey),"
					+ " FOREIGN KEY (messageId)"
					+ " REFERENCES messages (messageId)"
					+ " ON DELETE CASCADE,"
					+ " FOREIGN KEY (groupId)"
					+ " REFERENCES groups (groupId)"
					+ " ON DELETE CASCADE)";

	private static final Logger LOG =
			getLogger(JdbcDatabase.class.getName());

	private final Clock clock;
	private final DatabaseTypes dbTypes;

	private final Lock connectionsLock = new ReentrantLock();
	private final Condition connectionsChanged = connectionsLock.newCondition();

	@GuardedBy("connectionsLock")
	private final LinkedList<Connection> connections = new LinkedList<>();

	@GuardedBy("connectionsLock")
	private int openConnections = 0;
	@GuardedBy("connectionsLock")
	private boolean closed = false;

	protected abstract Connection createConnection()
			throws DbException, SQLException;

	protected abstract void compactAndClose() throws DbException;

	JdbcDatabase(DatabaseTypes databaseTypes,
			Clock clock) {
		this.dbTypes = databaseTypes;
		this.clock = clock;
	}

	protected void open(String driverClass, boolean reopen,
			@SuppressWarnings("unused") SecretKey key,
			@Nullable MigrationListener listener) throws DbException {
		// Load the JDBC driver
		try {
			Class.forName(driverClass);
		} catch (ClassNotFoundException e) {
			throw new DbException(e);
		}
		// Open the database and create the tables and indexes if necessary
		boolean compact;
		Connection txn = startTransaction();
		try {
			if (reopen) {
				Settings s = getSettings(txn, DB_SETTINGS_NAMESPACE);
				compact = migrateSchema(txn, s, listener) || isCompactionDue(s);
			} else {
				createTables(txn);
				initialiseSettings(txn);
				compact = false;
			}
			createIndexes(txn);
			commitTransaction(txn);
		} catch (DbException e) {
			abortTransaction(txn);
			throw e;
		}
		// Compact the database if necessary
		if (compact) {
			if (listener != null) listener.onDatabaseCompaction();
			long start = now();
			compactAndClose();
			logDuration(LOG, "Compacting database", start);
			// Allow the next transaction to reopen the DB
			synchronized (connectionsLock) {
				closed = false;
			}
			txn = startTransaction();
			try {
				storeLastCompacted(txn);
				commitTransaction(txn);
			} catch (DbException e) {
				abortTransaction(txn);
				throw e;
			}
		}
	}

	/**
	 * Compares the schema version stored in the database with the schema
	 * version used by the current code and applies any suitable migrations to
	 * the data if necessary.
	 *
	 * @return true if any migrations were applied, false if the schema was
	 * already current
	 * @throws DataTooNewException if the data uses a newer schema than the
	 *                             current code
	 * @throws DataTooOldException if the data uses an older schema than the
	 *                             current code and cannot be migrated
	 */
	private boolean migrateSchema(Connection txn, Settings s,
			@Nullable MigrationListener listener) throws DbException {
		int dataSchemaVersion = s.getInt(SCHEMA_VERSION_KEY, -1);
		if (dataSchemaVersion == -1) throw new DbException();
		if (dataSchemaVersion == CODE_SCHEMA_VERSION) return false;
		if (CODE_SCHEMA_VERSION < dataSchemaVersion)
			throw new DataTooNewException();
		// Apply any suitable migrations in order
		for (Migration<Connection> m : getMigrations()) {
			int start = m.getStartVersion(), end = m.getEndVersion();
			if (start == dataSchemaVersion) {
				if (LOG.isLoggable(INFO))
					LOG.info("Migrating from schema " + start + " to " + end);
				if (listener != null) listener.onDatabaseMigration();
				// Apply the migration
				m.migrate(txn);
				// Store the new schema version
				storeSchemaVersion(txn, end);
				dataSchemaVersion = end;
			}
		}
		if (dataSchemaVersion != CODE_SCHEMA_VERSION)
			throw new DataTooOldException();
		return true;
	}

	// Package access for testing
	List<Migration<Connection>> getMigrations() {
		return asList(
		);
	}

	private boolean isCompactionDue(Settings s) {
		long lastCompacted = s.getLong(LAST_COMPACTED_KEY, 0);
		long elapsed = clock.currentTimeMillis() - lastCompacted;
		if (LOG.isLoggable(INFO))
			LOG.info(elapsed + " ms since last compaction");
		return elapsed > MAX_COMPACTION_INTERVAL_MS;
	}

	private void storeSchemaVersion(Connection txn, int version)
			throws DbException {
		Settings s = new Settings();
		s.putInt(SCHEMA_VERSION_KEY, version);
		mergeSettings(txn, s, DB_SETTINGS_NAMESPACE);
	}

	private void storeLastCompacted(Connection txn) throws DbException {
		Settings s = new Settings();
		s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis());
		mergeSettings(txn, s, DB_SETTINGS_NAMESPACE);
	}

	private void initialiseSettings(Connection txn) throws DbException {
		Settings s = new Settings();
		s.putInt(SCHEMA_VERSION_KEY, CODE_SCHEMA_VERSION);
		s.putLong(LAST_COMPACTED_KEY, clock.currentTimeMillis());
		mergeSettings(txn, s, DB_SETTINGS_NAMESPACE);
	}

	private void createTables(Connection txn) throws DbException {
		Statement s = null;
		try {
			s = txn.createStatement();
			s.executeUpdate(dbTypes.replaceTypes(CREATE_SETTINGS));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_IDENTITIES));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_PROFILES));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTACTS));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_PENDING_CONTACTS));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTEXTS));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTEXTS_METADATA));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_CONTEXT_INVITES));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_GROUPS));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_GROUP_METADATA));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_GROUP_INVITES));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_FORUM_MEMBERLIST));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_MESSAGES));
			s.executeUpdate(dbTypes.replaceTypes(CREATE_MESSAGE_METADATA));
			s.close();
		} catch (SQLException e) {
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	private void createIndexes(Connection txn) throws DbException {
		Statement s = null;
		try {
			s = txn.createStatement();
			s.close();
		} catch (SQLException e) {
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Connection startTransaction() throws DbException {
		Connection txn;
		connectionsLock.lock();
		try {
			if (closed) throw new DbClosedException();
			txn = connections.poll();
		} finally {
			connectionsLock.unlock();
		}
		try {
			if (txn == null) {
				// Open a new connection
				txn = createConnection();
				txn.setAutoCommit(false);
				connectionsLock.lock();
				try {
					openConnections++;
				} finally {
					connectionsLock.unlock();
				}
			}
		} catch (SQLException e) {
			throw new DbException(e);
		}
		return txn;
	}

	@Override
	public void abortTransaction(Connection txn) {
		try {
			txn.rollback();
			connectionsLock.lock();
			try {
				connections.add(txn);
				connectionsChanged.signalAll();
			} finally {
				connectionsLock.unlock();
			}
		} catch (SQLException e) {
			// Try to close the connection
			logException(LOG, WARNING, e);
			tryToClose(txn, LOG, WARNING);
			// Whatever happens, allow the database to close
			connectionsLock.lock();
			try {
				openConnections--;
				connectionsChanged.signalAll();
			} finally {
				connectionsLock.unlock();
			}
		}
	}

	@Override
	public void commitTransaction(Connection txn) throws DbException {
		try {
			txn.commit();
		} catch (SQLException e) {
			throw new DbException(e);
		}
		connectionsLock.lock();
		try {
			connections.add(txn);
			connectionsChanged.signalAll();
		} finally {
			connectionsLock.unlock();
		}
	}

	void closeAllConnections() throws SQLException {
		boolean interrupted = false;
		connectionsLock.lock();
		try {
			closed = true;
			for (Connection c : connections) c.close();
			openConnections -= connections.size();
			connections.clear();
			while (openConnections > 0) {
				try {
					connectionsChanged.await();
				} catch (InterruptedException e) {
					LOG.warning("Interrupted while closing connections");
					interrupted = true;
				}
				for (Connection c : connections) c.close();
				openConnections -= connections.size();
				connections.clear();
			}
		} finally {
			connectionsLock.unlock();
		}

		if (interrupted) Thread.currentThread().interrupt();
	}

	@Override
	public boolean containsIdentity(Connection txn)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM identities";
			ps = txn.prepareStatement(sql);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Identity getIdentity(Connection txn)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT id, networkId, profilePicture, alias, timestamp"
							+ " FROM identities";
			ps = txn.prepareStatement(sql);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			String id = rs.getString(1);
			String networkId = rs.getString(2);
			byte[] profilePic = rs.getBytes(3);
			String alias = rs.getString(4);
			long timestamp = rs.getLong(5);
			rs.close();
			ps.close();
			Identity i = new Identity(id, alias, timestamp);
			i.setNetworkId(networkId);
			i.setProfilePicture(profilePic);
			return i;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Profile getProfile(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT alias, fullname, gender, " +
					"country, university, work, interests, quote, " +
					"profilepic FROM profiles WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			Profile profile = new ProfileBuilder(contextId)
					.setAlias(rs.getString(1))
					.setFullname(rs.getString(2))
					.setGender(rs.getInt(3))
					.setCountry(rs.getInt(4))
					.setUniversity(rs.getString(5))
					.setWork(rs.getString(6))
					.setInterests(rs.getString(7))
					.setQuote(rs.getString(8))
					.setProfilePicture(rs.getBytes(9))
					.build();
			rs.close();
			ps.close();
			return profile;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void setIdentityNetworkId(Connection txn, String networkId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int affected = 0;
		try {
			String sql = "UPDATE identities SET networkId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, networkId);
			affected = ps.executeUpdate();
			if (affected < 0) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void setIdentityProfilePicture(Connection txn, byte[] profilePic)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		int affected = 0;
		try {
			String sql = "UPDATE identities SET profilePicture = ?";
			ps = txn.prepareStatement(sql);
			ps.setBytes(1, profilePic);
			affected = ps.executeUpdate();
			if (affected < 0) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeProfile(Connection txn, String contextId) throws
			DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM localAuthors WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addIdentity(Connection txn, Identity identity)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO identities"
					+ " (id, alias, timestamp)"
					+ " VALUES (?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, identity.getId());
			ps.setString(2, identity.getAlias());
			ps.setLong(3, identity.getTimeCreated());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addContact(Connection txn, Contact contact)
			throws DbException {
		PreparedStatement ps = null;
		try {
			// Create a contact row
			String sql = "INSERT INTO contacts"
					+ " (contactId, alias, profilePicture)"
					+ " VALUES (?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contact.getId().getId());
			ps.setString(2, contact.getAlias());
			if (contact.getProfilePicture() != null)
				ps.setBytes(3, contact.getProfilePicture());
			else
				ps.setNull(3, BINARY);
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addGroup(Connection txn, Group group, byte[] descriptor,
			GroupType groupType)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO groups"
					+ " (groupId, contextId, descriptor, type)"
					+ " VALUES (?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, group.getId());
			ps.setString(2, group.getContextId());
			ps.setBytes(3, descriptor);
			ps.setInt(4, groupType.getValue());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addForumMember(Connection txn, ForumMember forumMember)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO forumMemberList"
					+ " (groupId, peerId, fakeId, alias, role, timestamp)"
					+ " VALUES (?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, forumMember.getGroupId());
			ps.setString(2, forumMember.getPeerId().getId());
			ps.setString(3, forumMember.getPeerId().getFakeId());
			ps.setString(4, forumMember.getAlias());
			ps.setInt(5, forumMember.getRole().getInt());
			ps.setLong(6, forumMember.getLstTimestamp());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addForumMembers(Connection txn,
			Collection<ForumMember> forumMembers)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO forumMemberList"
					+ " (groupId, peerId, fakeId, alias, role, timestamp)"
					+ " VALUES (?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);

			for (ForumMember forumMember : forumMembers) {
				ps.setString(1, forumMember.getGroupId());
				ps.setString(2, forumMember.getPeerId().getId());
				ps.setString(3, forumMember.getPeerId().getFakeId());
				ps.setString(4, forumMember.getAlias());
				ps.setInt(5, forumMember.getRole().getInt());
				ps.setLong(6, forumMember.getLstTimestamp());
				ps.addBatch();
			}
			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != forumMembers.size())
				throw new DbStateException();
			for (int rows : batchAffected)
				if (rows != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void updateForumMemberRole(Connection txn, ForumMember
			forumMember)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE forumMemberList SET role = ? " +
					"WHERE groupId = ? AND fakeId = ? AND timestamp < ?";
			ps = txn.prepareStatement(sql);
			ps.setInt(1, forumMember.getRole().getInt());
			ps.setString(2, forumMember.getGroupId());
			ps.setString(3, forumMember.getPeerId().getFakeId());
			ps.setLong(4, forumMember.getLstTimestamp());
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void updateForumMemberRole(Connection txn, String groupId,
			String fakeId, ForumMemberRole forumMemberRole, long timestamp)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE forumMemberList SET role = ? " +
					"WHERE groupId = ? AND fakeId = ? AND timestamp < ?";
			ps = txn.prepareStatement(sql);
			ps.setInt(1, forumMemberRole.getInt());
			ps.setString(2, groupId);
			ps.setString(3, fakeId);
			ps.setLong(4, timestamp);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void updateProfile(Connection txn, Profile p) throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE profiles"
					+ " SET alias = ?, fullname = ?, gender = ?, country = ?,"
					+ " interests = ?, quote = ?, profilepic = ?,"
					+ " university = ?, work = ?"
					+ " WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(10, p.getContextId());
			ps.setString(1, p.getAlias());
			ps.setString(2, p.getFullname());
			ps.setInt(3, p.getGender());
			ps.setInt(4, p.getCountry());
			ps.setString(5, p.getInterests());
			ps.setString(6, p.getQuote());
			if (p.getProfilePic() == null) {
				ps.setNull(7, Types.BLOB);
			} else {
				ps.setBytes(7, p.getProfilePic());
			}
			ps.setString(8, p.getUniversity());
			ps.setString(9, p.getWork());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addContactGroup(Connection txn, Group group,
			ContactId contactId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO groups"
					+ " (groupId, contextId, contactId, type)"
					+ " VALUES (?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, group.getId());
			ps.setString(2, group.getContextId());
			ps.setString(3, contactId.getId());
			ps.setInt(4, GroupType.PrivateConversation.getValue());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void raiseFavouriteFlag(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE messages SET favourite = TRUE"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			int affected = ps.executeUpdate();
			if (affected < 0 || affected > 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeFavouriteFlag(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE messages SET favourite = FALSE"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			int affected = ps.executeUpdate();
			if (affected < 0 || affected > 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addMessage(Connection txn, Message message, MessageState
			state,
			String contextId, boolean incoming) throws DbException {
		PreparedStatement ps = null;
		try {
			String sql =
					"INSERT INTO messages (messageId, contextId, groupId, timestamp,"
							+
							" text, mediaFileName, type, state, incoming, favourite, temporary)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, message.getId());
			ps.setString(2, contextId);
			ps.setString(3, message.getGroupId());
			ps.setLong(4, message.getTimestamp());
			ps.setString(5, message.getMessageBody());
			ps.setString(6, message.getMediaFileName());
			ps.setInt(7, message.getMessageType().getValue());
			ps.setInt(8, state.getValue());
			ps.setBoolean(9, incoming);
			ps.setBoolean(10, false);
			ps.setBoolean(11, false);
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addContext(Connection txn, DBContext c)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO contexts"
					+ " (contextId, name, color, type)"
					+ " VALUES (?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, c.getId());
			ps.setString(2, c.getName());
			ps.setInt(3, c.getColor());
			ps.setInt(4, c.getContextType().getValue());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addProfile(Connection txn, Profile p) throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO profiles"
					+ " (contextId, alias, fullname, gender, country,"
					+ " university, work, interests, quote, profilepic)"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, p.getContextId());
			ps.setString(2, p.getAlias());
			ps.setString(3, p.getFullname());
			ps.setInt(4, p.getGender());
			ps.setInt(5, p.getCountry());
			ps.setString(6, p.getUniversity());
			ps.setString(7, p.getWork());
			ps.setString(8, p.getInterests());
			ps.setString(9, p.getQuote());
			if (p.getProfilePic() == null) {
				ps.setNull(10, Types.BLOB);
			} else {
				ps.setBytes(10, p.getProfilePic());
			}
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean setMessageState(Connection txn, String messageId,
			MessageState state)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "UPDATE messages SET state = ?"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setInt(1, state.getValue());
			ps.setString(2, messageId);
			int affected = ps.executeUpdate();
			if (affected != 1) return false;
			ps.close();
			return true;
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsContact(Connection txn, ContactId contactId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM contacts"
					+ " WHERE contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contactId.getId());
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsContext(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM contexts WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsGroup(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM groups WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Contact getContact(Connection txn, ContactId cid)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT alias, profilePicture"
					+ " FROM contacts"
					+ " WHERE contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, cid.getId());
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			String alias = rs.getString(1);
			byte[] profilePic = rs.getBytes(2);
			rs.close();
			ps.close();
			return new Contact(cid, alias, profilePic);
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsContactGroup(Connection txn, ContactId
			contactId,
			String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT NULL FROM groups WHERE contextId = ? AND contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			ps.setString(2, contactId.getId());
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Contact> getContacts(Connection txn) throws
			DbException {
		Statement s = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT contactId, profilePicture, alias"
					+ " FROM contacts";
			s = txn.createStatement();
			rs = s.executeQuery(sql);
			List<Contact> contacts = new ArrayList<>();
			while (rs.next()) {
				ContactId contactId = new ContactId(rs.getString(1));
				byte[] profilePicture = rs.getBytes(2);
				String alias = rs.getString(3);
				contacts.add(new Contact(contactId, alias, profilePicture));
			}
			rs.close();
			s.close();
			return contacts;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<String> getContactIds(Connection txn, String
			contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT contactId"
					+
					" FROM groups WHERE contextId = ? AND contactId IS NOT NULL";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			List<String> contacts = new ArrayList<>();
			while (rs.next()) {
				contacts.add(rs.getString(1));
			}
			rs.close();
			ps.close();
			return contacts;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Integer getContextColor(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT color"
					+ " FROM contexts"
					+ " WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			Integer color = rs.getInt(1);
			rs.close();
			ps.close();
			return color;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<DBContext> getContexts(Connection txn)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT contextId, name, color, type FROM contexts";
			ps = txn.prepareStatement(sql);
			rs = ps.executeQuery();
			List<DBContext> contexts = new ArrayList<>();
			while (rs.next()) {
				DBContext context =
						new DBContext(rs.getString(1),
								rs.getString(2),
								rs.getInt(3),
								ContextType.fromValue(rs.getInt(4)));
				contexts.add(context);
			}
			rs.close();
			ps.close();
			return contexts;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public DBContext getContext(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT contextId, name, color, type FROM contexts " +
							"WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			DBContext context = null;
			while (rs.next()) context =
					new DBContext(rs.getString(1), rs.getString(2),
							rs.getInt(3),
							ContextType.fromValue(rs.getInt(4)));
			rs.close();
			ps.close();
			return context;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Group getContactGroup(Connection txn, ContactId contactId,
			String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId FROM groups " +
					"WHERE contactId = ? AND contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contactId.getId());
			ps.setString(2, contextId);
			rs = ps.executeQuery();
			Group group = null;
			while (rs.next()) group =
					new Group(rs.getString(1), contextId,
							GroupType.PrivateConversation);
			rs.close();
			ps.close();
			return group;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Group getGroup(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT contextId, descriptor, type FROM groups " +
					"WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			Group group = null;
			while (rs.next())
				group = new Group(groupId, rs.getString(1), rs.getBytes(2),
						GroupType.fromValue(rs.getInt(3)));
			rs.close();
			ps.close();
			return group;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Group> getGroups(Connection txn, String contextId,
			GroupType groupType)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId, descriptor, type" +
					" FROM groups WHERE contextId = ? AND type = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			ps.setInt(2, groupType.getValue());
			rs = ps.executeQuery();
			List<Group> groups = new ArrayList<>();
			while (rs.next()) {
				groups.add(
						new Group(rs.getString(1), contextId,
								rs.getBytes(2),
								GroupType.fromValue(rs.getInt(3))));
			}
			rs.close();
			ps.close();
			return groups;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Group> getForums(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId, descriptor, type" +
					" FROM groups WHERE contextId = ? AND type >= 2";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			List<Group> groups = new ArrayList<>();
			while (rs.next()) {
				groups.add(
						new Group(rs.getString(1), contextId,
								rs.getBytes(2),
								GroupType.fromValue(rs.getInt(3))));
			}
			rs.close();
			ps.close();
			return groups;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}


	@Override
	public Collection<ForumMember> getForumMembers(Connection txn,
			String groupId) throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT peerId, fakeId, alias, role, timestamp" +
					" FROM forumMemberList WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			List<ForumMember> forumMembers = new ArrayList<>();
			while (rs.next()) {
				forumMembers.add(
						new ForumMember(
								new PeerId(rs.getString(1),
										rs.getString(2)),
								groupId,
								rs.getString(3),
								ForumMemberRole.valueOf(rs.getInt(4)),
								rs.getLong(5)));
			}
			rs.close();
			ps.close();
			return forumMembers;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Group> getGroups(Connection txn, GroupType groupType)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId, contextId, descriptor" +
					" FROM groups WHERE type = ?";
			ps = txn.prepareStatement(sql);
			ps.setInt(1, groupType.getValue());
			rs = ps.executeQuery();
			List<Group> groups = new ArrayList<>();
			while (rs.next()) {
				groups.add(
						new Group(rs.getString(1), rs.getString(2),
								rs.getBytes(3), groupType));
			}
			rs.close();
			ps.close();
			return groups;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Group> getForums(Connection txn)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId, contextId, descriptor, type" +
					" FROM groups WHERE type >= 2";
			ps = txn.prepareStatement(sql);
			rs = ps.executeQuery();
			List<Group> groups = new ArrayList<>();
			while (rs.next()) {
				groups.add(
						new Group(rs.getString(1), rs.getString(2),
								rs.getBytes(3),
								GroupType.fromValue(rs.getInt(4))));
			}
			rs.close();
			ps.close();
			return groups;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public String getGroupContext(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT contextId FROM groups " +
					"WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			String contextId = null;
			while (rs.next()) contextId =
					rs.getString(1);
			rs.close();
			ps.close();
			return contextId;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public String getContextId(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT contextId FROM groups " +
					"WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			String contextId = rs.getString(1);
			rs.close();
			ps.close();
			return contextId;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Metadata getContextMetadata(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT metaKey, value FROM contextMetadata"
					+ " WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			Metadata metadata = new Metadata();
			while (rs.next()) metadata.put(rs.getString(1), rs.getBytes(2));
			rs.close();
			ps.close();
			return metadata;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Metadata getMessageMetadata(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT metaKey, value FROM messageMetadata"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			Metadata metadata = new Metadata();
			while (rs.next()) metadata.put(rs.getString(1), rs.getBytes(2));
			rs.close();
			ps.close();
			return metadata;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}


	@Override
	public Map<String, Metadata> getMessageMetadataByGroupId(Connection
			txn,
			String groupId) throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT messageId, metaKey, value"
					+ " FROM messageMetadata"
					+ " WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			Map<String, Metadata> all = new HashMap<>();
			while (rs.next()) {
				String messageId = rs.getString(1);
				Metadata metadata = all.get(messageId);
				if (metadata == null) {
					metadata = new Metadata();
					all.put(messageId, metadata);
				}
				metadata.put(rs.getString(2), rs.getBytes(3));
			}
			rs.close();
			ps.close();
			return all;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Metadata getGroupMetadata(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT metaKey, value FROM groupMetadata"
					+ " WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			Metadata metadata = new Metadata();
			while (rs.next()) metadata.put(rs.getString(1), rs.getBytes(2));
			rs.close();
			ps.close();
			return metadata;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Map<String, Metadata> getGroupMetadata(Connection txn,
			String[] groupIds) throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT groupId, metaKey, value"
					+ " FROM groupMetadata"
					+ " WHERE groupId IN (?)";
			ps = txn.prepareStatement(sql);
			ps.setArray(1, txn.createArrayOf("VARCHAR", groupIds));
			rs = ps.executeQuery();
			Map<String, Metadata> all = new HashMap<>();
			while (rs.next()) {
				String messageId = rs.getString(1);
				Metadata metadata = all.get(messageId);
				if (metadata == null) {
					metadata = new Metadata();
					all.put(messageId, metadata);
				}
				metadata.put(rs.getString(2), rs.getBytes(3));
			}
			rs.close();
			ps.close();
			return all;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeContact(Connection txn, ContactId c)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM contacts WHERE contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, c.getId());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeContext(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM contexts WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeForumMember(Connection txn, String groupId, String
			fakeId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql =
					"DELETE FROM forumMemberList WHERE groupId = ? AND fakeId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			ps.setString(2, fakeId);
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeForumMemberList(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM forumMemberList WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			int affected = ps.executeUpdate();
			if (affected < 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeContextMetadata(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM contextMetadata WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			int affected = ps.executeUpdate();
			System.out.println("deleted affected: " + affected);
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void setContactAlias(Connection txn, ContactId c,
			@Nullable String alias) throws DbException {
		PreparedStatement ps = null;
		try {
			String sql =
					"UPDATE contacts SET alias = ? WHERE contactId = ?";
			ps = txn.prepareStatement(sql);
			if (alias == null) ps.setNull(1, VARCHAR);
			else ps.setString(1, alias);
			ps.setString(2, c.getId());
			int affected = ps.executeUpdate();
			if (affected < 0 || affected > 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addPendingContact(Connection txn, PendingContact p)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "INSERT INTO pendingContacts (pendingContactId,"
					+ " alias, message, type, timestamp)"
					+ " VALUES (?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, p.getId().getId());
			ps.setString(2, p.getAlias());
			ps.setString(3, p.getMessage());
			ps.setInt(4, p.getPendingContactType().getValue());
			ps.setLong(5, p.getTimestamp());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addContextInvitation(Connection txn,
			ContextInvitation contextInvite)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql =
					"INSERT INTO contextInvites (contactId, pendingContextId, name,"
							+ " jsonContext, type, incoming, timestamp)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextInvite.getContactId().getId());
			ps.setString(2, contextInvite.getContextId());
			ps.setString(3, contextInvite.getName());
			if (contextInvite.isIncoming())
				ps.setString(4, contextInvite.getJson());
			else
				ps.setString(4, null);
			ps.setInt(5, contextInvite.getContextType().getValue());
			ps.setBoolean(6, contextInvite.isIncoming());
			ps.setLong(7, contextInvite.getTimestamp());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void addGroupInvitation(Connection txn, GroupInvitation
			groupInvite)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql =
					"INSERT INTO groupInvites (contactId, contextId, pendingGroupId, name,"
							+ " json, type, incoming, timestamp)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupInvite.getContactId().getId());
			ps.setString(2, groupInvite.getContextId());
			ps.setString(3, groupInvite.getGroupId());
			ps.setString(4, groupInvite.getName());
			if (groupInvite.isIncoming())
				ps.setString(5, groupInvite.getJson());
			else
				ps.setString(5, null);
			ps.setInt(6, groupInvite.getGroupInvitationType().getValue());
			ps.setBoolean(7, groupInvite.isIncoming());
			ps.setLong(8, groupInvite.getTimestamp());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsPendingContact(Connection txn,
			ContactId pendingContactId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM pendingContacts"
					+ " WHERE pendingContactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingContactId.getId());
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsPendingContext(Connection txn,
			String pendingContextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM contextInvites"
					+ " WHERE pendingContextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingContextId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsPendingGroup(Connection txn,
			String pendingGroupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM groupInvites"
					+ " WHERE pendingGroupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingGroupId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsPendingContextInvitation(Connection txn,
			ContactId contactId,
			String pendingContextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM contextInvites"
					+ " WHERE pendingContextId = ? AND contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingContextId);
			ps.setString(2, contactId.getId());
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsPendingGroupInvitation(Connection txn,
			ContactId contactId,
			String pendingGroupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM groupInvites"
					+ " WHERE pendingGroupId = ? AND contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingGroupId);
			ps.setString(2, contactId.getId());
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsProfile(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM profiles"
					+ " WHERE contextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public boolean containsMessage(Connection txn,
			String messageId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT NULL FROM messages"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			boolean found = rs.next();
			if (rs.next()) throw new DbStateException();
			rs.close();
			ps.close();
			return found;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public PendingContact getPendingContact(Connection txn,
			ContactId pendingContactId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT alias, type, message, timestamp"
					+ " FROM pendingContacts"
					+ " WHERE pendingContactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingContactId.getId());
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			String alias = rs.getString(1);
			PendingContactType type =
					PendingContactType.fromValue(rs.getInt(2));
			String message = rs.getString(3);
			long timestamp = rs.getLong(4);
			return new PendingContact(pendingContactId, alias, type,
					message,
					timestamp);
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<PendingContact> getPendingContacts(Connection txn)
			throws DbException {
		Statement s = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT pendingContactId, alias, type, timestamp, message"
							+ " FROM pendingContacts";
			s = txn.createStatement();
			rs = s.executeQuery(sql);
			List<PendingContact> pendingContacts = new ArrayList<>();
			while (rs.next()) {
				ContactId id = new ContactId(rs.getString(1));
				String alias = rs.getString(2);
				PendingContactType type =
						PendingContactType.fromValue(rs.getInt(3));
				long timestamp = rs.getLong(4);
				String message = rs.getString(5);
				pendingContacts
						.add(new PendingContact(id, alias, type, message,
								timestamp));
			}
			rs.close();
			s.close();
			return pendingContacts;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations(
			Connection txn)
			throws DbException {
		Statement s = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT pendingContextId, contactId, name, type, jsonContext, timestamp, incoming"
							+ " FROM contextInvites";
			s = txn.createStatement();
			rs = s.executeQuery(sql);
			List<ContextInvitation> contextInvites = new ArrayList<>();
			while (rs.next()) {
				String pendingContextId = rs.getString(1);
				ContactId contactId = new ContactId(rs.getString(2));
				String name = rs.getString(3);
				ContextType contextType =
						ContextType.fromValue(rs.getInt(4));
				String jsonContext = rs.getString(5);
				long timestamp = rs.getLong(6);
				boolean incoming = rs.getBoolean(7);
				contextInvites
						.add(new ContextInvitation(contactId,
								pendingContextId,
								name,
								contextType, jsonContext, timestamp,
								incoming));
			}
			rs.close();
			s.close();
			return contextInvites;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<GroupInvitation> getGroupInvitations(Connection txn)
			throws DbException {
		Statement s = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT pendingGroupId, contextId, contactId, name, type, json, timestamp, incoming"
							+ " FROM groupInvites";
			s = txn.createStatement();
			rs = s.executeQuery(sql);
			List<GroupInvitation> groupInvites = new ArrayList<>();
			while (rs.next()) {
				String pendingGroupId = rs.getString(1);
				String contextId = rs.getString(2);
				ContactId contactId = new ContactId(rs.getString(3));
				String name = rs.getString(4);
				GroupInvitationType groupInvitationType =
						GroupInvitationType.fromValue(rs.getInt(5));
				String json = rs.getString(6);
				long timestamp = rs.getLong(7);
				boolean incoming = rs.getBoolean(8);
				groupInvites.add(new GroupInvitation(contactId, contextId,
						pendingGroupId, name, groupInvitationType, json,
						timestamp, incoming));
			}
			rs.close();
			s.close();
			return groupInvites;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(s, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<ContextInvitation> getPendingContextInvitations(
			Connection txn,
			String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT pendingContextId, contactId, name, type, jsonContext, timestamp, incoming" +
							" FROM contextInvites WHERE pendingContextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			List<ContextInvitation> contextInvites = new ArrayList<>();
			while (rs.next()) {
				String pendingContextId = rs.getString(1);
				ContactId contactId = new ContactId(rs.getString(2));
				String name = rs.getString(3);
				ContextType contextType =
						ContextType.fromValue(rs.getInt(4));
				String jsonContext = rs.getString(5);
				long timestamp = rs.getLong(6);
				boolean incoming = rs.getBoolean(7);
				contextInvites
						.add(new ContextInvitation(contactId,
								pendingContextId,
								name,
								contextType, jsonContext, timestamp,
								incoming));
			}
			rs.close();
			ps.close();
			return contextInvites;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Message getMessage(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT groupId, timestamp, text, type, mediaFileName, incoming"
							+ " FROM messages"
							+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			String groupId = rs.getString(1);
			long timestamp = rs.getLong(2);
			String body = rs.getString(3);
			Message.Type type = Message.Type.fromValue(rs.getInt(4));
			String mediaFileName = rs.getString(5);
			Message message =
					new Message(messageId, groupId, timestamp, body,
							mediaFileName, type);
			return message;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public MessageState getMessageState(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT state"
					+ " FROM messages"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			MessageState state = MessageState.fromValue(rs.getInt(1));
			rs.close();
			ps.close();
			return state;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<String> getMessageIds(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT messageId FROM messages WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			List<String> messages = new ArrayList<>();
			while (rs.next()) {
				messages.add(rs.getString(1));
			}
			rs.close();
			ps.close();
			return messages;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<MessageHeader> getMessageHeaders(Connection txn,
			String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT messageId, timestamp, state, incoming, favourite, type FROM messages WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			List<MessageHeader> messageHeaders = new ArrayList<>();
			while (rs.next()) {
				String messageId = rs.getString(1);
				long timestamp = rs.getLong(2);
				MessageState messageState =
						MessageState.fromValue(rs.getInt(3));
				boolean incoming = rs.getBoolean(4);
				boolean favourite = rs.getBoolean(5);
				Message.Type msgType = Message.Type.fromValue(rs.getInt(6));
				messageHeaders.add(
						new MessageHeader(messageId, groupId, timestamp,
								messageState, incoming, favourite, msgType)
				);
			}
			rs.close();
			ps.close();
			return messageHeaders;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Collection<Message> getFavourites(Connection txn,
			String contextId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT messageId, groupId, text, timestamp FROM messages WHERE contextId = ? AND favourite = TRUE";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			rs = ps.executeQuery();
			List<Message> favourites = new ArrayList<>();
			while (rs.next()) {
				String messageId = rs.getString(1);
				String groupId = rs.getString(2);
				String message_text = rs.getString(3);
				long timestamp = rs.getLong(4);
				favourites.add(new Message(messageId, groupId, timestamp,
						message_text, Message.Type.TEXT)
				);
			}
			rs.close();
			ps.close();
			return favourites;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public String getMessageText(Connection txn, String messageId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT text FROM messages"
							+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			return rs.getString(1);
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removePendingContact(Connection txn, ContactId
			pendingContactId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM pendingContacts"
					+ " WHERE pendingContactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingContactId.getId());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removePendingContext(Connection txn, String contextId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM contextInvites"
					+ " WHERE pendingContextId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removePendingGroup(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM groupInvites"
					+ " WHERE pendingGroupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeContextInvitation(Connection txn, ContactId
			contactId,
			String contextId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM contextInvites"
					+ " WHERE pendingContextId = ? AND contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			ps.setString(2, contactId.getId());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void removeGroupInvitation(Connection txn, ContactId contactId,
			String pendingGroupId)
			throws DbException {
		PreparedStatement ps = null;
		try {
			String sql = "DELETE FROM groupInvites"
					+ " WHERE pendingGroupId = ? AND contactId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, pendingGroupId);
			ps.setString(2, contactId.getId());
			int affected = ps.executeUpdate();
			if (affected != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public Settings getSettings(Connection txn, String namespace)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT settingKey, value FROM settings"
					+ " WHERE namespace = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, namespace);
			rs = ps.executeQuery();
			Settings s = new Settings();
			while (rs.next()) s.put(rs.getString(1), rs.getString(2));
			rs.close();
			ps.close();
			return s;
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void mergeContextMetadata(Connection txn, String contextId,
			Metadata meta)
			throws DbException {
		PreparedStatement ps = null;
		try {
			Map<String, byte[]> added = removeOrUpdateMetadata(txn,
					contextId, meta,
					"contextMetadata", "contextId");
			if (added.isEmpty()) return;
			// Insert any keys that don't already exist
			String sql =
					"INSERT INTO contextMetadata (contextId, metaKey, value)"
							+ " VALUES (?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, contextId);
			for (Map.Entry<String, byte[]> e : added.entrySet()) {
				ps.setString(2, e.getKey());
				ps.setBytes(3, e.getValue());
				ps.addBatch();
			}
			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != added.size())
				throw new DbStateException();
			for (int rows : batchAffected)
				if (rows != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public ContactId getContactIdByGroupId(Connection txn, String groupId)
			throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql =
					"SELECT contactId FROM groups"
							+ " WHERE groupId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			return new ContactId(rs.getString(1));
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void mergeMessageMetadata(Connection txn, String messageId,
			Metadata meta) throws DbException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Map<String, byte[]> added = removeOrUpdateMetadata(txn,
					messageId, meta, "messageMetadata", "messageId");
			if (added.isEmpty()) return;
			// Get the group ID and message state for the denormalised columns
			String sql = "SELECT groupId, state FROM messages"
					+ " WHERE messageId = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new DbStateException();
			String groupId = rs.getString(1);
			rs.close();
			ps.close();
			// Insert any keys that don't already exist
			sql = "INSERT INTO messageMetadata"
					+ " (messageId, groupId, metaKey, value)"
					+ " VALUES (?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, messageId);
			ps.setString(2, groupId);
			for (Map.Entry<String, byte[]> e : added.entrySet()) {
				ps.setString(3, e.getKey());
				ps.setBytes(4, e.getValue());
				ps.addBatch();
			}
			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != added.size())
				throw new DbStateException();
			for (int rows : batchAffected)
				if (rows != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(rs, LOG, WARNING);
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void mergeGroupMetadata(Connection txn, String groupId,
			Metadata meta)
			throws DbException {
		PreparedStatement ps = null;
		try {
			Map<String, byte[]> added = removeOrUpdateMetadata(txn,
					groupId, meta, "groupMetadata", "groupId");
			if (added.isEmpty()) return;
			// Insert any keys that don't already exist
			String contextId = getGroupContext(txn, groupId);
			String sql =
					"INSERT INTO groupMetadata (groupId, contextId, metaKey, value)"
							+ " VALUES (?, ?, ?, ?)";
			ps = txn.prepareStatement(sql);
			ps.setString(1, groupId);
			ps.setString(2, contextId);
			for (Map.Entry<String, byte[]> e : added.entrySet()) {
				ps.setString(3, e.getKey());
				ps.setBytes(4, e.getValue());
				ps.addBatch();
			}
			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != added.size())
				throw new DbStateException();
			for (int rows : batchAffected)
				if (rows != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	private Map<String, byte[]> removeOrUpdateMetadata(Connection txn,
			String id, Metadata meta, String tableName, String columnName)
			throws DbException {
		PreparedStatement ps = null;
		try {
			// Determine which keys are being removed
			List<String> removed = new ArrayList<>();
			Map<String, byte[]> notRemoved = new HashMap<>();
			for (Map.Entry<String, byte[]> e : meta.entrySet()) {
				if (e.getValue() == REMOVE) removed.add(e.getKey());
				else notRemoved.put(e.getKey(), e.getValue());
			}
			// Delete any keys that are being removed
			if (!removed.isEmpty()) {
				String sql = "DELETE FROM " + tableName
						+ " WHERE " + columnName + " = ? AND metaKey = ?";
				ps = txn.prepareStatement(sql);
				ps.setString(1, id);
				for (String key : removed) {
					ps.setString(2, key);
					ps.addBatch();
				}
				int[] batchAffected = ps.executeBatch();
				if (batchAffected.length != removed.size())
					throw new DbStateException();
				for (int rows : batchAffected) {
					if (rows < 0) throw new DbStateException();
					if (rows > 1) throw new DbStateException();
				}
				ps.close();
			}
			if (notRemoved.isEmpty()) return Collections.emptyMap();
			// Update any keys that already exist
			String sql = "UPDATE " + tableName + " SET value = ?"
					+ " WHERE " + columnName + " = ? AND metaKey = ?";
			ps = txn.prepareStatement(sql);
			ps.setString(2, id);
			for (Map.Entry<String, byte[]> e : notRemoved.entrySet()) {
				ps.setBytes(1, e.getValue());
				ps.setString(3, e.getKey());
				ps.addBatch();
			}

			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != notRemoved.size())
				throw new DbStateException();
			for (int rows : batchAffected) {
				if (rows < 0) throw new DbStateException();
				if (rows > 1) throw new DbStateException();
			}
			ps.close();
			// Are there any keys that don't already exist?
			Map<String, byte[]> added = new HashMap<>();
			int updateIndex = 0;
			for (Map.Entry<String, byte[]> e : notRemoved.entrySet()) {
				if (batchAffected[updateIndex++] == 0)
					added.put(e.getKey(), e.getValue());
			}
			return added;
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}

	@Override
	public void mergeSettings(Connection txn, Settings s, String namespace)
			throws DbException {
		PreparedStatement ps = null;
		try {
			// Update any settings that already exist
			String sql = "UPDATE settings SET value = ?"
					+ " WHERE namespace = ? AND settingKey = ?";
			ps = txn.prepareStatement(sql);
			for (Map.Entry<String, String> e : s.entrySet()) {
				ps.setString(1, e.getValue());
				ps.setString(2, namespace);
				ps.setString(3, e.getKey());
				ps.addBatch();
			}
			int[] batchAffected = ps.executeBatch();
			if (batchAffected.length != s.size())
				throw new DbStateException();
			for (int rows : batchAffected) {
				if (rows < 0) throw new DbStateException();
				if (rows > 1) throw new DbStateException();
			}
			// Insert any settings that don't already exist
			sql = "INSERT INTO settings (namespace, settingKey, value)"
					+ " VALUES (?, ?, ?)";
			ps = txn.prepareStatement(sql);
			int updateIndex = 0, inserted = 0;
			for (Map.Entry<String, String> e : s.entrySet()) {
				if (batchAffected[updateIndex] == 0) {
					ps.setString(1, namespace);
					ps.setString(2, e.getKey());
					ps.setString(3, e.getValue());
					ps.addBatch();
					inserted++;
				}
				updateIndex++;
			}
			batchAffected = ps.executeBatch();
			if (batchAffected.length != inserted)
				throw new DbStateException();
			for (int rows : batchAffected)
				if (rows != 1) throw new DbStateException();
			ps.close();
		} catch (SQLException e) {
			tryToClose(ps, LOG, WARNING);
			throw new DbException(e);
		}
	}
}
