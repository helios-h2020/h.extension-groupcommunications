package eu.h2020.helios_social.modules.groupcommunications.api.contact;

import java.util.Objects;

public class ContactId {

	public String cid;

	public ContactId(String cid) {
		this.cid = cid;
	}

	public String getId() {
		return cid;
	}

	@Override
	public String toString() {
		return "ContactId{" +
				"cid='" + cid + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ContactId contactId = (ContactId) o;
		return Objects.equals(cid, contactId.cid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cid);
	}
}
