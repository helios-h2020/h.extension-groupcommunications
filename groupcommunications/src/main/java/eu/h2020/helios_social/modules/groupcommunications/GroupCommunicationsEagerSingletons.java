package eu.h2020.helios_social.modules.groupcommunications;

import eu.h2020.helios_social.modules.groupcommunications.context.ContextModule;
import eu.h2020.helios_social.modules.groupcommunications.group.GroupModule;
import eu.h2020.helios_social.modules.groupcommunications.messaging.MessagingModule;
import eu.h2020.helios_social.modules.groupcommunications.mining.MiningModule;
import eu.h2020.helios_social.modules.groupcommunications.profile.ProfileModule;
import eu.h2020.helios_social.modules.groupcommunications.resourcediscovery.ResourceDiscoveryModule;

public interface GroupCommunicationsEagerSingletons {

    void inject(GroupCommunicationsModule.EagerSingletons init);

    void inject(ContextModule.EagerSingletons init);

    void inject(ResourceDiscoveryModule.EagerSingletons init);

    void inject(MessagingModule.EagerSingletons init);

    void inject(MiningModule.EagerSingletons init);

    void inject(ProfileModule.EagerSingletons init);

    void inject(GroupModule.EagerSingletons init);

    class Helper {

        public static void injectEagerSingletons(
                GroupCommunicationsEagerSingletons c) {
            c.inject(new ContextModule.EagerSingletons());
            c.inject(new ResourceDiscoveryModule.EagerSingletons());
            c.inject(new GroupCommunicationsModule.EagerSingletons());
            c.inject(new MessagingModule.EagerSingletons());
            c.inject(new MiningModule.EagerSingletons());
            c.inject(new ProfileModule.EagerSingletons());
            c.inject(new GroupModule.EagerSingletons());
        }
    }
}
