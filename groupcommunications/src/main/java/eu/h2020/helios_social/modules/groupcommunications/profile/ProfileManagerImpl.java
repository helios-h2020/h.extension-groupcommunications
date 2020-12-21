package eu.h2020.helios_social.modules.groupcommunications.profile;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.db.DatabaseComponent;
import eu.h2020.helios_social.happ.helios.talk.api.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;

public class ProfileManagerImpl implements ProfileManager<Transaction> {

	private final DatabaseComponent db;

	@Inject
	public ProfileManagerImpl(DatabaseComponent db) {
		this.db = db;
	}

	@Override
	public void addProfile(Profile profile) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.addProfile(txn, profile);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public void updateProfile(Profile profile) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.updateProfile(txn, profile);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public boolean containsProfile(String contextId) throws DbException {
		Transaction txn = db.startTransaction(false);
		boolean isProfileExists;
		try {
			isProfileExists = db.containsProfile(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return isProfileExists;
	}

	@Override
	public void removeProfile(String contextId) throws DbException {
		Transaction txn = db.startTransaction(false);
		try {
			db.removeProfile(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
	}

	@Override
	public Profile getProfile(String contextId) throws DbException {
		Transaction txn = db.startTransaction(true);
		Profile profile;
		try {
			profile = db.getProfile(txn, contextId);
			db.commitTransaction(txn);
		} finally {
			db.endTransaction(txn);
		}
		return profile;
	}
}
