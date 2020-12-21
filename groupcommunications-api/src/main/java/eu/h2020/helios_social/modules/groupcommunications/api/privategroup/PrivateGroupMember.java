package eu.h2020.helios_social.modules.groupcommunications.api.privategroup;

import eu.h2020.helios_social.modules.groupcommunications.api.peer.Peer;
import eu.h2020.helios_social.modules.groupcommunications.api.peer.PeerInfo;

/**
 * A class of Secret Forum members
 */
public class PrivateGroupMember {

    private Peer member;
    private final boolean isOwner;

    public PrivateGroupMember(Peer peer, boolean isOwner) {
        this.member = peer;
        this.isOwner = isOwner;
    }

    /**
     * Returns true if the Member is also the owner of the Secret Forum
     *
     * @return
     */
    public boolean isOwner() {
        return this.isOwner;
    }

    /**
     * Returns Member's Profile
     *
     * @return
     */
    public PeerInfo getMemberInfo() {
        return member.getPeerInfo();
    }
}
