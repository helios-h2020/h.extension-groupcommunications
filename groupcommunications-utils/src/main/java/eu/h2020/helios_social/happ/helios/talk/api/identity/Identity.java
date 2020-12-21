package eu.h2020.helios_social.happ.helios.talk.api.identity;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

import eu.h2020.helios_social.happ.helios.talk.api.nullsafety.NotNullByDefault;

@Immutable
@NotNullByDefault
public class Identity {

	private final String id;
	private String networkId;
	private byte[] profilePic;
	private final String alias;
	private final long created;

	public Identity(String id,
			String alias, long created) {
		this.id = id;
		this.alias = alias;
		this.created = created;
	}

	/**
	 * Returns the helios-id of the user.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the network-id of the user.
	 */
	public String getNetworkId() {
		return networkId;
	}

	public byte[] getProfilePicture() {
		return profilePic;
	}

	public void setProfilePicture(byte[] profilePic) {
		this.profilePic = profilePic;
	}

	/**
	 * Returns the network-id of the user.
	 */
	public void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

	/**
	 * Returns the user's pseudonym.
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns the time the identity was created, in milliseconds since the
	 * Unix epoch.
	 */
	public long getTimeCreated() {
		return created;
	}

	@Override
	public String toString() {
		return "Identity{" +
				"id='" + id + '\'' +
				", networkId='" + networkId + '\'' +
				", alias='" + alias + '\'' +
				", created=" + created +
				'}';
	}
}
