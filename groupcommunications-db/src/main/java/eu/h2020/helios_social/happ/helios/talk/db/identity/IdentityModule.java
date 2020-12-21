package eu.h2020.helios_social.happ.helios.talk.db.identity;

import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.happ.helios.talk.api.lifecycle.LifecycleManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class IdentityModule {

	public static class EagerSingletons {
		@Inject
		IdentityManager identityManager;
	}

	@Provides
	@Singleton
	IdentityManager provideIdentityManager(LifecycleManager lifecycleManager,
			IdentityManagerImpl identityManager) {
		lifecycleManager.registerOpenDatabaseHook(identityManager);
		return identityManager;
	}
}
