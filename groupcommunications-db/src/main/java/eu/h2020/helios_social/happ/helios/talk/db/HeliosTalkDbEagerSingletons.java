package eu.h2020.helios_social.happ.helios.talk.db;

import eu.h2020.helios_social.happ.helios.talk.db.crypto.CryptoExecutorModule;
import eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseExecutorModule;
import eu.h2020.helios_social.happ.helios.talk.db.identity.IdentityModule;
import eu.h2020.helios_social.happ.helios.talk.db.lifecycle.LifecycleModule;
import eu.h2020.helios_social.happ.helios.talk.db.system.SystemModule;

public interface HeliosTalkDbEagerSingletons {


	void inject(CryptoExecutorModule.EagerSingletons init);

	void inject(DatabaseExecutorModule.EagerSingletons init);

	void inject(IdentityModule.EagerSingletons init);

	void inject(LifecycleModule.EagerSingletons init);

	void inject(SystemModule.EagerSingletons init);


	class Helper {

		public static void injectEagerSingletons(HeliosTalkDbEagerSingletons c) {
			c.inject(new CryptoExecutorModule.EagerSingletons());
			c.inject(new DatabaseExecutorModule.EagerSingletons());
			c.inject(new IdentityModule.EagerSingletons());
			c.inject(new LifecycleModule.EagerSingletons());
			c.inject(new SystemModule.EagerSingletons());
		}
	}
}
