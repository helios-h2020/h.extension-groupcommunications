package eu.h2020.helios_social.happ.helios.talk.api.contact;

import java.util.regex.Pattern;

public interface HeliosLinkConstants {

	String LINK_PREFIX = "helios://";

	/**
	 * The length of a base32-encoded helios link in bytes, excluding the
	 * 'helios://' prefix.
	 */
	int BASE32_LINK_BYTES = 46;

	/**
	 * Regular expression for matching helios links, including or excluding
	 * the 'helios://' prefix.
	 */
	Pattern LINK_REGEX =
			Pattern.compile(
					"(helios://)?([a-zA-Z0-9]{" + BASE32_LINK_BYTES + "})");
}
