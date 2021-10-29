package eu.h2020.helios_social.modules.groupcommunications.forum;

import javax.inject.Inject;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.Forum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.LocationForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.SeasonalForum;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing.ForumAccessRequest;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing.ForumAccessRequestFactory;
import eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing.ForumInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.group.Group;
import eu.h2020.helios_social.modules.groupcommunications.api.group.sharing.GroupInvitationType;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.PrivateGroup;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInfo;
import eu.h2020.helios_social.modules.groupcommunications.api.privategroup.sharing.GroupInvitation;
import eu.h2020.helios_social.modules.groupcommunications.api.sharing.Request;
import eu.h2020.helios_social.modules.groupcommunications_utils.system.Clock;

public class ForumAccessRequestFactoryImpl implements ForumAccessRequestFactory
{
    private final Clock clock;

    @Inject
    public ForumAccessRequestFactoryImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ForumAccessRequest createIncomingForumAccessRequest(ContactId contactId,
                                                               ForumInfo forumInfo) {
        return new ForumAccessRequest(contactId, forumInfo.getContextId(),
                forumInfo.getGroupId(), forumInfo.getName(),
                forumInfo.getType(),
                forumInfo.getTimestamp(), true, forumInfo.getPeerName());
    }

    @Override
    public ForumAccessRequest createOutgoingForumAccessRequest(ContactId contactId,
                                                         Forum forum, String peerName) {
            return new ForumAccessRequest(contactId, forum.getContextId(),
                    forum.getId(), forum.getName(),
                    Request.Type.FORUM_ACCESS, clock.currentTimeMillis(), false, peerName);
    }
}
