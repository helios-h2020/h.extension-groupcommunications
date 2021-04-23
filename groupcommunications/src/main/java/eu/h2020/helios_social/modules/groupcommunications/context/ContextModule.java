package eu.h2020.helios_social.modules.groupcommunications.context;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.ContextInvitationFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.context.sharing.SharingContextManager;
import eu.h2020.helios_social.modules.groupcommunications.context.sharing.ContextInvitationFactoryImpl;
import eu.h2020.helios_social.modules.groupcommunications.context.sharing.SharingContextManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications_utils.sync.event.EventBus;

@Module
public class ContextModule {
    public static class EagerSingletons {
        @Inject
        ContextManager contextManager;
    }

    @Provides
    @Singleton
    ContextManager provideContextManager(ContextManagerImpl contextManager) {
        return contextManager;
    }

    @Provides
    ContextFactory providesContextFactory(ContextFactoryImpl contextFactory) {
        return contextFactory;
    }

    @Provides
    ContextInvitationFactory providesContextInvitationFactory(
            ContextInvitationFactoryImpl contextInvitationFactory) {
        return contextInvitationFactory;
    }

    @Provides
    @Singleton
    SharingContextManager providesSharingContextManager(EventBus eventBus,
                                                        SharingContextManagerImpl sharingContextManager) {
        eventBus.addListener(sharingContextManager);
        return sharingContextManager;
    }

}
