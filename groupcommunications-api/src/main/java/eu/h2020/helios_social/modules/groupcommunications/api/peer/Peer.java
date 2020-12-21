package eu.h2020.helios_social.modules.groupcommunications.api.peer;

/**
 * This class implements a peer.
 */
public class Peer {

    private PeerId peerId;
    private PeerInfo peerInfo;

    public Peer(PeerId peerId, PeerInfo info) {
        this.peerId = peerId;
        this.peerInfo = info;
    }

    public PeerId getPeerId() {
        return peerId;
    }

    public void setPeerId(PeerId peerId) {
        this.peerId = peerId;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }

    public void setPeerInfo(PeerInfo peerInfo) {
        this.peerInfo = peerInfo;
    }
}
