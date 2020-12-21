package eu.h2020.helios_social.modules.groupcommunications.group.sharing;

import javax.inject.Inject;

import eu.h2020.helios_social.happ.helios.talk.api.system.Clock;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.GroupType;
import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitationFactory;

public class GroupInvitationFactoryImpl implements GroupInvitationFactory {

	private final Clock clock;

	@Inject
	public GroupInvitationFactoryImpl(Clock clock) {
		this.clock = clock;
	}

	@Override
	public GroupInvitation createIncomingGroupInvitation(ContactId contactId,
			GroupInfo groupInfo) {
		return new GroupInvitation(contactId, groupInfo.getContextId(),
				groupInfo.getGroupId(), groupInfo.getName(),
				groupInfo.getGroupInvitationType(), groupInfo.getJson(),
				groupInfo.getTimestamp(), true);
	}

	@Override
	public GroupInvitation createOutgoingGroupInvitation(ContactId contactId,
			Group group) {
		if (group instanceof PrivateGroup) {
			PrivateGroup privateGroup = (PrivateGroup) group;
			return new GroupInvitation(contactId, group.getContextId(),
					group.getId(), privateGroup.getName(),
					GroupInvitationType.PrivateGroup,
					privateGroup.toJson(), clock.currentTimeMillis(), false);
		} else if (group instanceof LocationForum) {
			LocationForum locationForum = (LocationForum) group;
			return new GroupInvitation(contactId, group.getContextId(),
					group.getId(), locationForum.getName(),
					GroupInvitationType.LocationForum,
					locationForum.toJson(), clock.currentTimeMillis(), false);
		} else if (group instanceof SeasonalForum) {
			SeasonalForum seasonalForum = (SeasonalForum) group;
			return new GroupInvitation(contactId, group.getContextId(),
					group.getId(), seasonalForum.getName(),
					GroupInvitationType.SeasonalForum,
					seasonalForum.toJson(), clock.currentTimeMillis(), false);
		} else if (group instanceof Forum) {
			Forum forum = (Forum) group;
			return new GroupInvitation(contactId, group.getContextId(),
					group.getId(), forum.getName(),
					GroupInvitationType.SeasonalForum,
					forum.toJson(), clock.currentTimeMillis(), false);
		} else {
			throw new IllegalArgumentException(
					"Group must be instance of PrivateGroup or Forum!");
		}
	}
}
