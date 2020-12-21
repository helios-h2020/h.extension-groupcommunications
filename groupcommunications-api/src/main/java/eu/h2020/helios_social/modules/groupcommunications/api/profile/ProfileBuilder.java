package eu.h2020.helios_social.modules.groupcommunications.api.profile;

public class ProfileBuilder {

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

	public ProfileBuilder(String contextId) {
		this.contextId = contextId;
	}

	public ProfileBuilder setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public ProfileBuilder setFullname(String fullname) {
		this.fullname = fullname;
		return this;
	}

	public ProfileBuilder setCountry(int countryId) {
		this.country = countryId;
		return this;
	}

	public ProfileBuilder setGender(int genderId) {
		this.gender = genderId;
		return this;
	}

	public ProfileBuilder setUniversity(String university) {
		this.university = university;
		return this;
	}

	public ProfileBuilder setWork(String work) {
		this.work = work;
		return this;
	}

	public ProfileBuilder setInterests(String interests) {
		this.interests = interests;
		return this;
	}

	public ProfileBuilder setQuote(String quote) {
		this.quote = quote;
		return this;
	}

	public ProfileBuilder setProfilePicture(byte[] profile_pic) {
		this.profile_pic = profile_pic;
		return this;
	}

	public Profile build() {
		return new Profile(contextId, alias, fullname, gender, country,
				university, work, interests, quote, profile_pic);
	}
}
