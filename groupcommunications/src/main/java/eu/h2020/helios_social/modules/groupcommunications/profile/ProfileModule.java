package eu.h2020.helios_social.modules.groupcommunications.profile;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.ProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.api.profile.sharing.SharingProfileManager;
import eu.h2020.helios_social.modules.groupcommunications.profile.sharing.SharingProfileManagerImpl;

@Module
public class ProfileModule {

    @Provides
    @Singleton
    ProfileManager providesProfileManager(
            ProfileManagerImpl profileManager) {
        return profileManager;
    }

    @Provides
    @Singleton
    SharingProfileManager providesSharingProfileManager(
            SharingProfileManagerImpl sharingProfileManager) {
        return sharingProfileManager;
    }
}
