package eu.h2020.helios_social.modules.groupcommunications.group;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupManager;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.GroupMessageFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitationFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.SharingGroupManager;
import eu.h2020.helios_social.modules.groupcommunications.group.sharing.GroupInvitationFactoryImpl;
import eu.h2020.helios_social.modules.groupcommunications.group.sharing.SharingGroupManagerImpl;


@Module
public class GroupModule {

	@Provides
	@Singleton
	GroupManager providesGroupManager(GroupManagerImpl groupManager) {
		return groupManager;
	}

	@Provides
	@Singleton
	SharingGroupManager providesSharingGroupManager(
			SharingGroupManagerImpl sharingGroupManager) {
		return sharingGroupManager;
	}

	@Provides
	GroupInvitationFactory providesGroupInvitationFactoryImpl(
			GroupInvitationFactoryImpl groupInvitationFactory) {
		return groupInvitationFactory;
	}

	@Provides
	@Singleton
	GroupMessageFactory providesGroupMessageFactory(
			GroupMessageFactoryImpl groupMessageFactory) {
		return groupMessageFactory;
	}

	@Provides
	GroupFactory providesGroupFactory(
			GroupFactoryImpl groupFactory) {
		return groupFactory;
	}
}
