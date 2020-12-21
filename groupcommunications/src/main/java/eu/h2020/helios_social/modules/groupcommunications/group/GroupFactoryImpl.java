package eu.h2020.helios_social.modules.groupcommunications.group;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.identity.IdentityManager;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.utils.password.PasswordGenerator;

public class GroupFactoryImpl implements GroupFactory {

	private IdentityManager identityManager;
	private PasswordGenerator passwordGenerator;

	@Inject
	public GroupFactoryImpl(IdentityManager identityManager) {
		this.identityManager = identityManager;
		this.passwordGenerator = new PasswordGenerator();
	}

	@Override
	public PrivateGroup createPrivateGroup(@NotNull String name,
			String contextId) {
		return new PrivateGroup(UUID.randomUUID().toString(), contextId, name,
				passwordGenerator.generateRandomPassword(8),
				identityManager.getIdentity().getNetworkId());
	}
}
