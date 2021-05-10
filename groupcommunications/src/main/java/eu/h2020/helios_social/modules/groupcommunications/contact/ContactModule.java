package eu.h2020.helios_social.modules.groupcommunications.contact;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.PendingContactFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.connection.ConnectionRegistry;
import eu.h2020.helios_social.modules.groupcommunications.contact.connection.ConnectionManagerImpl;
import eu.h2020.helios_social.modules.groupcommunications.contact.connection.ConnectionRegistryImpl;

@Module
public class ContactModule {

    @Provides
    @Singleton
    ContactManager providesContactManager(ContactManagerImpl contactManager) {
        return contactManager;
    }

    @Provides
    @Singleton
    ConnectionManager providesConnectionManager(
            ConnectionManagerImpl connectionManager) {
        return connectionManager;
    }

    @Provides
    @Singleton
    PendingContactFactory providesPendingContactFactory(
            PendingContactFactoryImpl pendingContactFactory) {
        return pendingContactFactory;
    }

    @Provides
    @Singleton
    ConnectionRegistry providesConnectionRegistry(
            ConnectionRegistryImpl connectionRegistry) {
        return connectionRegistry;
    }
}

