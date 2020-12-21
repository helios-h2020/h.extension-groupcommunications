package eu.h2020.helios_social.modules.groupcommunications.profile;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;

@Module
public class ProfileModule {

	@Provides
	@Singleton
	ProfileManager providesProfileManager(
			ProfileManagerImpl profileManager) {
		return profileManager;
	}
}
