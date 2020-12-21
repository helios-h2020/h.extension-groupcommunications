package eu.h2020.helios_social.modules.groupcommunications.api.peer;

/**
 * PeerInfo provides information about a peer that she is not included in the user's connections
 * but she can be communicate with her though public/secret forums
 */
public class PeerInfo {

	private PeerId peerId;
	private byte[] profilePicture;
	private String alias;
	private String realName;
	private String funnyName;

	private PeerInfo(Builder builder) {
		this.alias = builder.alias;
		this.realName = builder.realName;
		this.funnyName = builder.funnyName;
		this.profilePicture = builder.profilePicture;
		this.peerId = builder.peerId;
	}

	public static class Builder {

		private String alias, realName, funnyName;
		private byte[] profilePicture;
		private PeerId peerId;

		public Builder() {
		}

		public Builder alias(String alias) {
			this.alias = alias;
			return this;
		}

		public Builder real_name(String realName) {
			this.realName = realName;
			return this;
		}

		public Builder funny_name(String funnyName) {
			this.funnyName = funnyName;
			return this;
		}

		public Builder profile_picture(byte[] profilePicture) {
			this.profilePicture = profilePicture;
			return this;
		}

		public Builder peerId(PeerId peer_id) {
			this.peerId = peer_id;
			return this;
		}

		public PeerInfo build() {
			return new PeerInfo(this);
		}
	}

	/**
	 * Returns the profile picture of the peer
	 *
	 * @return
	 */
	public byte[] getProfilePicture() {
		return profilePicture;
	}

	/**
	 * Updates the Users's profile picture
	 *
	 * @param selectedPicture
	 */
	public void updateProfilePicture(byte[] selectedPicture) {
		this.profilePicture = selectedPicture;
	}

	public void setProfilePicture(byte[] profilePicture) {
		this.profilePicture = profilePicture;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getFunnyName() {
		return funnyName;
	}

	public void setFunnyName(String funnyName) {
		this.funnyName = funnyName;
	}

	public PeerId getPeerId() {
		return peerId;
	}

	public void setPeerId(PeerId peerId) {
		this.peerId = peerId;
	}
}
