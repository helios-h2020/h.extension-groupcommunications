package eu.h2020.helios_social.happ.helios.talk.db;

import dagger.Module;
import eu.h2020.helios_social.happ.helios.talk.db.crypto.CryptoExecutorModule;
import eu.h2020.helios_social.happ.helios.talk.db.crypto.CryptoModule;
import eu.h2020.helios_social.happ.helios.talk.db.data.DataModule;
import eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseExecutorModule;
import eu.h2020.helios_social.happ.helios.talk.db.database.DatabaseModule;
import eu.h2020.helios_social.happ.helios.talk.db.event.EventModule;
import eu.h2020.helios_social.happ.helios.talk.db.identity.IdentityModule;
import eu.h2020.helios_social.happ.helios.talk.db.lifecycle.LifecycleModule;
import eu.h2020.helios_social.happ.helios.talk.db.settings.SettingsModule;
import eu.h2020.helios_social.happ.helios.talk.db.system.SystemModule;

@Module(includes = {
		CryptoModule.class,
		CryptoExecutorModule.class,
		DataModule.class,
		DatabaseModule.class,
		DatabaseExecutorModule.class,
		EventModule.class,
		IdentityModule.class,
		LifecycleModule.class,
		SettingsModule.class,
		SystemModule.class,
})
public class HeliosTalkDbModule {
}
