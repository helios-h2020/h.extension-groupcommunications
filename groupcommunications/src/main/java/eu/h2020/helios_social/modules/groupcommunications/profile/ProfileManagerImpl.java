package eu.h2020.helios_social.modules.groupcommunications.profile;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications_utils.db.DatabaseComponent;
import eu.h2020.helios_social.modules.groupcommunications_utils.db.Transaction;
import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.Profile;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.Event;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventListener;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.ProfilingStateEvent;

public class ProfileManagerImpl implements ProfileManager<Transaction>/*, EventListener*/ {

	private final DatabaseComponent db;
	//private String state;
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
		Transaction txn = db.startTransaction(true);
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

/*	@Override
	public void eventOccurred(Event e) {
		if (e instanceof ProfilingStateEvent) {
			state = ((ProfilingStateEvent) e).getState();

		}
	}*/

/*	@Override
	public String getProfilingState() {
		return state;
	}*/
}
