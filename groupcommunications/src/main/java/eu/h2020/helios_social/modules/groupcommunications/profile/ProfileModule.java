package eu.h2020.helios_social.modules.groupcommunications.profile;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.sharing.SharingProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.profile.sharing.SharingProfileManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;

@Module
public class ProfileModule {

    public static class EagerSingletons {
        @Inject
        SharingProfileManager sharingProfileManager;
    }

    @Provides
    @Singleton
    ProfileManager providesProfileManager(ProfileManagerImpl profileManager) {
        return profileManager;
    }

    @Provides
    @Singleton
    SharingProfileManager providesSharingProfileManager(EventBus eventBus,
                                                        SharingProfileManagerImpl sharingProfileManager) {
        eventBus.addListener(sharingProfileManager);
        return sharingProfileManager;
    }
}
