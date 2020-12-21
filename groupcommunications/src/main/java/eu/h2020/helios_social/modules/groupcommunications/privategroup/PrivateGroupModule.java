package eu.h2020.helios_social.modules.groupcommunications.privategroup;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroupManager;

@Module
public class PrivateGroupModule {

	@Provides
	@Singleton
	PrivateGroupManager providesPrivateGroupManager(
			PrivateGroupManagerImpl privateGroupManager) {
		return privateGroupManager;
	}

}
