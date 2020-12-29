package eu.h2020.helios_social.modules.groupcommunications.event;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.event.HeliosEventManager;
import eu.h2020.helios_social.modules.groupcommunications.api.event.sharing.SharingEventManager;
import eu.h2020.helios_social.modules.groupcommunications.event.sharing.SharingEventManagerImpl;

@Module
public class HeliosEventModule {

    @Provides
    @Singleton
    SharingEventManager providesSharingEventManager(
            SharingEventManagerImpl sharingEventManager) {
        return sharingEventManager;
    }

    @Provides
    @Singleton
    HeliosEventManager providesHeliosEventManager(
            HeliosEventManagerImpl eventManager) {
        return eventManager;
    }
}
