package eu.h2020.helios_social.modules.groupcommunications.api.profile;

import eu.h2020.helios_social.modules.groupcommunications.api.messaging.AbstractMessage;

public class Profile implements AbstractMessage {

	private String contextId;
	private String alias;
	private String fullname;
	private Integer gender;
	private Integer country;
	private String university;
	private String work;
	private String interests;
	private String quote;
	private byte[] profile_pic;

	public Profile(String contextId,
			String alias,
			String fullname,
			Integer gender,
			Integer country,
			String university,
			String work,
			String interests,
			String quote,
			byte[] profile_pic) {
		this.contextId = contextId;
		this.alias = alias;
		this.fullname = fullname;
		this.gender = gender;
		this.country = country;
		this.university = university;
		this.work = work;
		this.interests = interests;
		this.quote = quote;
		this.profile_pic = profile_pic;
	}

	public String getContextId() {
		return contextId;
	}

	public String getUniversity() {
		return university;
	}

	public String getWork() {
		return work;
	}

	public String getAlias() {
		return alias;
	}

	public String getFullname() {
		return fullname;
	}

	public Integer getGender() {
		return gender;
	}

	public Integer getCountry() {
		return country;
	}

	public String getInterests() {
		return interests;
	}

	public String getQuote() {
		return quote;
	}

	public byte[] getProfilePic() {
		return profile_pic;
	}
}
