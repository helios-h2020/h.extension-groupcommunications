package eu.h2020.helios_social.modules.groupcommunications.api.profile;

import eu.h2020.helios_social.modules.groupcommunications.api.exception.DbException;

public interface ProfileManager<T> {

	void addProfile(Profile profile) throws DbException;

	void updateProfile(Profile profile) throws DbException;

	boolean containsProfile(String contextId) throws DbException;

	void removeProfile(String contextId) throws DbException;

	Profile getProfile(String contextId) throws DbException;
}
