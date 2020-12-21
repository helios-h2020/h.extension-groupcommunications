package eu.h2020.helios_social.modules.groupcommunications.api.forum.sharing;

import eu.h2020.helios_social.modules.groupcommunications.api.contact.ContactId;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerId;

public interface ForumSharingManager {

    /**
     * Invite contact to join a forum
     *
     * @param contactId
     * @param groupId
     */
    public void sendForumInvitation(ContactId contactId, String groupId);

    /**
     * Send additional forum info to contact after she has accepted the forum invite
     *
     * @param contactId
     * @param groupId
     */
    public void sendForumInfo(ContactId contactId, String groupId);

    /**
     * After accepting invite to a forum, user request additional info about the forum (includes
     * list of moderators, and the list of 100 last messages)
     *
     * @param contactId
     * @param groupId
     */
    public void requestForumInfo(ContactId contactId, String groupId);

    /**
     * Respond to Forum Invite by sending conversationInfo to all your connections and
     * initialize conversations only with those already in the context
     */
    public void respondToForumInvitation(ContactId contactId, String groupId);

    /**
     * Request access to forum of PROTECTED type from peer (the peer should be admin/moderator of
     * the forum)
     *
     * @param peerId
     */
    public void requestAccessToForum(PeerId peerId, String forumId);
}
