package eu.h2020.helios_social.modules.groupcommunications.forum;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.ForumManager;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.membership.ForumMembershipManager;
import eu.h2020.helios_social.modules.groupcommunications.forum.mebership.ForumMembershipManagerImpl;

@Module
public class ForumModule {

	@Provides
	@Singleton
	ForumManager providesForumManager(ForumManagerImpl forumManager) {
		return forumManager;
	}

	@Provides
	@Singleton
	ForumMembershipManager providesForumMembershipManager(
			ForumMembershipManagerImpl forumMembershipManager) {
		return forumMembershipManager;
	}
}
